package com.logiconnect.platform.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /**
     * Maximum failed login attempts before temporary account lockout.
     */
    private int maxFailedAttempts = 5;

    /**
     * Duration in minutes for temporary account lockout.
     */
    private int lockDurationMinutes = 15;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(int lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }
}
