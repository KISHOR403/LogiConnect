package com.logiconnect.platform.common.util;

import com.logiconnect.platform.security.authentication.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe utility methods for accessing authenticated user identity and authorities.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Retrieve the current authentication object from SecurityContext.
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Retrieve the current authenticated UserPrincipal.
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getAuthentication()
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserPrincipal)
                .map(principal -> (UserPrincipal) principal);
    }

    /**
     * Retrieve current authenticated User ID (UUID).
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getId);
    }

    /**
     * Retrieve current authenticated employee code.
     */
    public static Optional<String> getCurrentEmployeeCode() {
        return getCurrentUserPrincipal().map(UserPrincipal::getEmployeeCode);
    }

    /**
     * Check if a user is currently authenticated (non-anonymous).
     */
    public static boolean isAuthenticated() {
        return getAuthentication()
                .map(auth -> auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal())))
                .orElse(false);
    }

    /**
     * Check if current authenticated user has a specific role (with or without 'ROLE_' prefix).
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthentication()
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(roleWithPrefix::equals))
                .orElse(false);
    }

    /**
     * Check if current authenticated user possesses a specific permission authority.
     */
    public static boolean hasAuthority(String authority) {
        return getAuthentication()
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority::equals))
                .orElse(false);
    }
}
