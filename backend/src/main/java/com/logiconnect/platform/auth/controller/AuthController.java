package com.logiconnect.platform.auth.controller;

import com.logiconnect.platform.auth.dto.ChangePasswordRequest;
import com.logiconnect.platform.auth.dto.CurrentUserResponse;
import com.logiconnect.platform.auth.dto.LoginRequest;
import com.logiconnect.platform.auth.dto.LoginResponse;
import com.logiconnect.platform.auth.dto.RefreshTokenRequest;
import com.logiconnect.platform.auth.service.AuthService;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.security.authentication.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Enterprise authentication, token lifecycle, session management, and password control")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user by employee code or email and password, returning JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponse response = authService.login(loginRequest, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and returns a new access/refresh token pair")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest request
    ) {
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponse response = authService.refreshToken(refreshRequest, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes current refresh token session and logs out user", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest refreshRequest,
            HttpServletRequest request
    ) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUserPrincipal().orElse(null);
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        authService.logout(refreshRequest, currentUser, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns safe profile, organization unit, roles, and permissions of currently authenticated user", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUserPrincipal().orElse(null);
        CurrentUserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Current user profile retrieved successfully"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Changes password for authenticated user and revokes all other active sessions", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            HttpServletRequest request
    ) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUserPrincipal().orElse(null);
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        authService.changePassword(changePasswordRequest, currentUser, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully. All active sessions have been invalidated."));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
