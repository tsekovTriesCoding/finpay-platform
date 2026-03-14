package com.finpay.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurable rate limiting properties.
 *
 * rate-limiting:
 *   enabled: true
 *   default-rate: 100        # requests per window
 *   default-window-seconds: 60
 *   auth-rate: 20            # stricter for auth endpoints
 *   auth-window-seconds: 60
 *   admin-rate: 200          # higher for admin endpoints
 *   admin-window-seconds: 60
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limiting")
@Getter
@Setter
public class RateLimitProperties {
    private boolean enabled = true;

    /** Default requests allowed per window for general API calls. */
    private int defaultRate = 100;
    private int defaultWindowSeconds = 60;

    /** Stricter limit for auth endpoints (login/register). */
    private int authRate = 20;
    private int authWindowSeconds = 60;

    /** Higher limit for admin endpoints. */
    private int adminRate = 200;
    private int adminWindowSeconds = 60;
}
