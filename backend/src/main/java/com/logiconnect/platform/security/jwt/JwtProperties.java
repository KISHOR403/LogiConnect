package com.logiconnect.platform.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for JWT authentication parameters.
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret HMAC key string (minimum 256 bits for HS256/HS384/HS512).
     */
    private String secret;

    /**
     * Access token expiration duration in milliseconds (default: 15 minutes = 900000ms).
     */
    private long accessExpirationMs = 900000L;

    /**
     * Refresh token expiration duration in milliseconds (default: 7 days = 604800000ms).
     */
    private long refreshExpirationMs = 604800000L;

    /**
     * Token issuer identifier claim.
     */
    private String issuer = "logiconnect-platform";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public void setAccessExpirationMs(long accessExpirationMs) {
        this.accessExpirationMs = accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public void setRefreshExpirationMs(long refreshExpirationMs) {
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
