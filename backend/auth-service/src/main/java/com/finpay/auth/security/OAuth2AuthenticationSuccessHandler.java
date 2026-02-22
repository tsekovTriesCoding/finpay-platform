package com.finpay.auth.security;

import com.finpay.auth.dto.UserDto;
import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.entity.UserCredential;
import com.finpay.auth.event.UserRegisteredEvent;
import com.finpay.auth.kafka.AuthEventProducer;
import com.finpay.auth.repository.RefreshTokenRepository;
import com.finpay.auth.repository.UserCredentialRepository;
import com.finpay.auth.service.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;
    private final AuthEventProducer authEventProducer;

    @Value("${oauth2.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        
        if (authentication instanceof OAuth2AuthenticationToken oAuth2Token) {
            OAuth2User oAuth2User = oAuth2Token.getPrincipal();
            String provider = oAuth2Token.getAuthorizedClientRegistrationId();
            
            UserCredential credential = processOAuth2User(oAuth2User, provider);
            UserDto user = toUserDto(credential);
            
            String accessToken = jwtService.generateAccessToken(user);
            String refreshTokenValue = jwtService.generateRefreshToken(user);
            
            // Save refresh token
            RefreshToken refreshToken = RefreshToken.builder()
                    .token(refreshTokenValue)
                    .userId(user.id())
                    .userEmail(user.email())
                    .expiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                    .revoked(false)
                    .build();
            refreshTokenRepository.save(refreshToken);
            
            // Set HTTP-only cookies instead of passing tokens in URL
            cookieService.setAuthCookies(response, accessToken, refreshTokenValue);
            
            log.info("OAuth2 login successful for user: {}", user.email());
            
            // Redirect without tokens in URL (cookies are set)
            getRedirectStrategy().sendRedirect(request, response, frontendRedirectUri);
        }
    }

    private UserCredential processOAuth2User(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String email = extractEmail(attributes, provider);
        String firstName = extractFirstName(attributes, provider);
        String lastName = extractLastName(attributes, provider);
        String providerId = extractProviderId(attributes, provider);
        String profileImageUrl = extractProfileImageUrl(attributes, provider);
        
        String authProvider = provider.toUpperCase();
        
        Optional<UserCredential> existingCredential = credentialRepository.findByEmail(email);
        
        if (existingCredential.isPresent()) {
            UserCredential credential = existingCredential.get();
            
            // Link OAuth provider if user was registered locally
            if (credential.getOauthProvider() == null) {
                credential.setOauthProvider(authProvider);
                credential.setOauthProviderId(providerId);
                credential.setProfileImageUrl(profileImageUrl);
                credential.setLastLoginAt(LocalDateTime.now());
                return credentialRepository.save(credential);
            }
            
            // Update last login
            credential.setLastLoginAt(LocalDateTime.now());
            return credentialRepository.save(credential);
        }
        
        // Create new credential for OAuth user
        UserCredential newCredential = UserCredential.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .oauthProvider(authProvider)
                .oauthProviderId(providerId)
                .profileImageUrl(profileImageUrl)
                .enabled(true)
                .emailVerified(true)  // OAuth emails are verified
                .accountLocked(false)
                .lastLoginAt(LocalDateTime.now())
                .build();
        
        UserCredential savedCredential = credentialRepository.save(newCredential);
        log.info("New OAuth user registered with ID: {}", savedCredential.getId());
        
        // Publish event for user-service
        UserRegisteredEvent event = UserRegisteredEvent.createOAuth(
                savedCredential.getId(),
                savedCredential.getEmail(),
                savedCredential.getFirstName(),
                savedCredential.getLastName(),
                authProvider,
                providerId,
                profileImageUrl
        );
        authEventProducer.publishUserRegistered(event);
        
        return savedCredential;
    }

    private UserDto toUserDto(UserCredential credential) {
        return new UserDto(
                credential.getId(),
                credential.getEmail(),
                null,
                credential.getFirstName(),
                credential.getLastName(),
                credential.getPhoneNumber(),
                credential.isEnabled() ? "ACTIVE" : "INACTIVE",
                "USER",
                credential.getOauthProvider(),
                credential.getOauthProviderId(),
                credential.getProfileImageUrl(),
                null, null, null, null,
                credential.isEmailVerified(),
                false,
                credential.getPlan() != null ? credential.getPlan().name() : "STARTER",
                credential.getCreatedAt(),
                credential.getUpdatedAt(),
                credential.getLastLoginAt()
        );
    }

    private String extractEmail(Map<String, Object> attributes, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("email");
            case "github" -> (String) attributes.get("email");
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    private String extractFirstName(Map<String, Object> attributes, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("given_name");
            case "github" -> {
                String name = (String) attributes.get("name");
                yield name != null ? name.split(" ")[0] : (String) attributes.get("login");
            }
            default -> "User";
        };
    }

    private String extractLastName(Map<String, Object> attributes, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("family_name");
            case "github" -> {
                String name = (String) attributes.get("name");
                if (name != null && name.contains(" ")) {
                    String[] parts = name.split(" ");
                    yield parts[parts.length - 1];
                }
                yield "";
            }
            default -> "";
        };
    }

    private String extractProviderId(Map<String, Object> attributes, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("sub");
            case "github" -> String.valueOf(attributes.get("id"));
            default -> null;
        };
    }

    private String extractProfileImageUrl(Map<String, Object> attributes, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> (String) attributes.get("picture");
            case "github" -> (String) attributes.get("avatar_url");
            default -> null;
        };
    }
}
