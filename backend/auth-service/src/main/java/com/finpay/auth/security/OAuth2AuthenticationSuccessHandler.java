package com.finpay.auth.security;

import com.finpay.auth.client.UserServiceClient;
import com.finpay.auth.dto.CreateUserRequest;
import com.finpay.auth.dto.UserDto;
import com.finpay.auth.entity.RefreshToken;
import com.finpay.auth.repository.RefreshTokenRepository;
import feign.FeignException;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;
    private final RefreshTokenRepository refreshTokenRepository;

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
            
            UserDto user = processOAuth2User(oAuth2User, provider);
            
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
            
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken", refreshTokenValue)
                    .build().toUriString();
            
            log.info("OAuth2 login successful for user: {}", user.email());
            
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }

    private UserDto processOAuth2User(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        String email = extractEmail(attributes, provider);
        String firstName = extractFirstName(attributes, provider);
        String lastName = extractLastName(attributes, provider);
        String providerId = extractProviderId(attributes, provider);
        String profileImageUrl = extractProfileImageUrl(attributes, provider);
        
        String authProvider = provider.toUpperCase();
        
        try {
            // Try to get existing user
            UserDto existingUser = userServiceClient.getUserByEmail(email);
            
            // Update OAuth info if needed
            if (existingUser.authProvider() == null || "LOCAL".equals(existingUser.authProvider())) {
                return userServiceClient.linkOAuthProvider(
                        existingUser.id(), 
                        authProvider, 
                        providerId, 
                        profileImageUrl
                );
            }
            return existingUser;
            
        } catch (FeignException.NotFound e) {
            // Create new user
            CreateUserRequest createRequest = CreateUserRequest.forOAuth2User(
                    email, firstName, lastName, authProvider, providerId, profileImageUrl
            );
            return userServiceClient.createUser(createRequest);
        }
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
