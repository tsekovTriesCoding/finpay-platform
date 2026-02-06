package com.finpay.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${cookie.domain}")
    private String cookieDomain;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Sets HTTP-only cookies for access and refresh tokens
     */
    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, (int) (accessTokenExpiration / 1000));
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, (int) (refreshTokenExpiration / 1000));
    }

    /**
     * Clears authentication cookies
     */
    public void clearAuthCookies(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN_COOKIE);
        deleteCookie(response, REFRESH_TOKEN_COOKIE);
    }

    /**
     * Sets the access token cookie only (used for token refresh)
     */
    public void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, (int) (accessTokenExpiration / 1000));
    }

    /**
     * Sets the refresh token cookie only
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, (int) (refreshTokenExpiration / 1000));
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        
        // Set domain only if not localhost (for local development)
        if (!"localhost".equals(cookieDomain)) {
            cookie.setDomain(cookieDomain);
        }
        
        // Add SameSite attribute via header (Cookie class doesn't support SameSite directly)
        String cookieHeader = String.format(
            "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=%s%s%s",
            name, value, maxAgeSeconds, cookieSameSite,
            cookieSecure ? "; Secure" : "",
            !"localhost".equals(cookieDomain) ? "; Domain=" + cookieDomain : ""
        );
        
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        String cookieHeader = String.format(
            "%s=; Max-Age=0; Path=/; HttpOnly; SameSite=%s%s%s",
            name, cookieSameSite,
            cookieSecure ? "; Secure" : "",
            !"localhost".equals(cookieDomain) ? "; Domain=" + cookieDomain : ""
        );
        
        response.addHeader("Set-Cookie", cookieHeader);
    }
}
