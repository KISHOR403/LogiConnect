package com.logiconnect.platform.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryDto user
) {
    public LoginResponse(String accessToken, String refreshToken, long expiresIn, UserSummaryDto user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
