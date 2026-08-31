package com.logiconnect.platform.auth.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.auth.dto.ChangePasswordRequest;
import com.logiconnect.platform.auth.dto.CurrentUserResponse;
import com.logiconnect.platform.auth.dto.DepartmentSummaryDto;
import com.logiconnect.platform.auth.dto.LoginRequest;
import com.logiconnect.platform.auth.dto.LoginResponse;
import com.logiconnect.platform.auth.dto.RefreshTokenRequest;
import com.logiconnect.platform.auth.dto.TeamSummaryDto;
import com.logiconnect.platform.auth.dto.UserSummaryDto;
import com.logiconnect.platform.auth.entity.UserSession;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.exception.UnauthorizedException;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.role.entity.Role;
import com.logiconnect.platform.security.authentication.CustomUserDetailsService;
import com.logiconnect.platform.security.authentication.UserPrincipal;
import com.logiconnect.platform.security.config.AuthProperties;
import com.logiconnect.platform.security.jwt.JwtProperties;
import com.logiconnect.platform.security.jwt.JwtTokenProvider;
import com.logiconnect.platform.user.entity.User;
import com.logiconnect.platform.user.entity.UserStatus;
import com.logiconnect.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(noRollbackFor = {UnauthorizedException.class, BadRequestException.class, ResourceNotFoundException.class})
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            AuthProperties authProperties,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.auditService = auditService;
    }

    /**
     * Authenticates user, validates lockouts & status, issues access/refresh tokens, and registers active session.
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String identifier = request.getEmployeeCode() != null ? request.getEmployeeCode().trim() : "";
        Instant now = Instant.now();

        User user = userRepository.findByIdentifierWithDetails(identifier).orElse(null);

        if (user == null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("identifier", identifier);
            meta.put("reason", "USER_NOT_FOUND");
            auditService.recordAuthEvent(null, AuditAction.LOGIN_FAILED, "USER", null, ipAddress, userAgent, meta);
            throw new UnauthorizedException("Invalid credentials.");
        }

        // Check account lock status
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("reason", "ACCOUNT_LOCKED");
                meta.put("lockedUntil", user.getLockedUntil().toString());
                auditService.recordAuthEvent(user, AuditAction.LOGIN_FAILED, "USER", user.getId(), ipAddress, userAgent, meta);
                throw new UnauthorizedException("Account is temporarily locked due to repeated failed login attempts. Please try again later.");
            } else {
                // Lockout window expired, unlock account
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }
        }

        // Check user active status and employee employment eligibility
        if (user.getStatus() != UserStatus.ACTIVE ||
                (user.getEmployee() != null && !user.getEmployee().getStatus().isEligibleForLogin())) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("reason", "ACCOUNT_INACTIVE_OR_SUSPENDED");
            meta.put("userStatus", user.getStatus().name());
            if (user.getEmployee() != null) {
                meta.put("employeeStatus", user.getEmployee().getStatus().name());
            }
            auditService.recordAuthEvent(user, AuditAction.LOGIN_FAILED, "USER", user.getId(), ipAddress, userAgent, meta);
            throw new UnauthorizedException("Account is inactive or disabled. Please contact system administration.");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            Map<String, Object> meta = new HashMap<>();
            meta.put("failedAttempts", failedAttempts);

            if (failedAttempts >= authProperties.getMaxFailedAttempts()) {
                Instant lockUntil = now.plus(Duration.ofMinutes(authProperties.getLockDurationMinutes()));
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(lockUntil);
                userRepository.saveAndFlush(user);

                meta.put("lockedUntil", lockUntil.toString());
                auditService.recordAuthEvent(user, AuditAction.ACCOUNT_LOCKED, "USER", user.getId(), ipAddress, userAgent, meta);
                auditService.recordAuthEvent(user, AuditAction.LOGIN_FAILED, "USER", user.getId(), ipAddress, userAgent, meta);

                throw new UnauthorizedException("Invalid credentials. Account is now temporarily locked.");
            } else {
                userRepository.saveAndFlush(user);
                auditService.recordAuthEvent(user, AuditAction.LOGIN_FAILED, "USER", user.getId(), ipAddress, userAgent, meta);
                throw new UnauthorizedException("Invalid credentials.");
            }
        }

        // Reset failed login counters upon successful authentication
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.saveAndFlush(user);

        UserPrincipal principal = CustomUserDetailsService.toUserPrincipal(user);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        // Store secure hash of refresh token in user_sessions
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setDeviceInfo(request.getDeviceInfo());
        session.setIpAddress(ipAddress);
        session.setExpiresAt(now.plusMillis(jwtProperties.getRefreshExpirationMs()));
        session.setLastUsedAt(now);
        userSessionRepository.save(session);

        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionId", session.getId().toString());
        auditService.recordAuthEvent(user, AuditAction.LOGIN_SUCCESS, "USER", user.getId(), ipAddress, userAgent, meta);

        long expiresInSeconds = jwtProperties.getAccessExpirationMs() / 1000;
        UserSummaryDto userSummary = buildUserSummaryDto(user);

        return new LoginResponse(accessToken, refreshToken, expiresInSeconds, userSummary);
    }

    /**
     * Rotates refresh token session, generates new access token & refresh token, and enforces replay protection.
     */
    public LoginResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String rawRefreshToken = request.getRefreshToken();
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required.");
        }

        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        String tokenType = jwtTokenProvider.getTokenTypeFromToken(rawRefreshToken);
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new UnauthorizedException("Provided token is not a refresh token.");
        }

        String tokenHash = hashToken(rawRefreshToken);
        UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash).orElse(null);

        if (session == null) {
            throw new UnauthorizedException("Refresh session not found.");
        }

        Instant now = Instant.now();

        // Detect Token Replay Attack: session was previously revoked
        if (session.getRevokedAt() != null) {
            log.warn("SECURITY ALERT: Refresh token replay detected for user {}. Revoking all active sessions.", session.getUser().getId());
            userSessionRepository.revokeAllActiveSessionsForUser(session.getUser().getId(), now);

            Map<String, Object> meta = new HashMap<>();
            meta.put("reason", "TOKEN_REPLAY_DETECTED");
            meta.put("replayedSessionId", session.getId().toString());
            auditService.recordAuthEvent(session.getUser(), AuditAction.ACCOUNT_LOCKED, "USER", session.getUser().getId(), ipAddress, userAgent, meta);

            throw new UnauthorizedException("Invalid session state. All sessions terminated for security reasons.");
        }

        // Check session expiration
        if (session.getExpiresAt().isBefore(now)) {
            session.setRevokedAt(now);
            userSessionRepository.save(session);
            throw new UnauthorizedException("Refresh session has expired. Please login again.");
        }

        User user = userRepository.findByIdWithDetails(session.getUser().getId()).orElse(null);
        if (user == null || !user.isEligibleForLogin(now)) {
            throw new UnauthorizedException("User account is no longer eligible for login.");
        }

        // Invalidate old session
        session.setRevokedAt(now);
        session.setLastUsedAt(now);
        userSessionRepository.save(session);

        // Issue new rotated pair
        UserPrincipal principal = CustomUserDetailsService.toUserPrincipal(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        UserSession newSession = new UserSession();
        newSession.setUser(user);
        newSession.setRefreshTokenHash(hashToken(newRefreshToken));
        newSession.setDeviceInfo(request.getDeviceInfo() != null ? request.getDeviceInfo() : session.getDeviceInfo());
        newSession.setIpAddress(ipAddress);
        newSession.setExpiresAt(now.plusMillis(jwtProperties.getRefreshExpirationMs()));
        newSession.setLastUsedAt(now);
        userSessionRepository.save(newSession);

        Map<String, Object> meta = new HashMap<>();
        meta.put("oldSessionId", session.getId().toString());
        meta.put("newSessionId", newSession.getId().toString());
        auditService.recordAuthEvent(user, AuditAction.TOKEN_REFRESHED, "USER", user.getId(), ipAddress, userAgent, meta);

        long expiresInSeconds = jwtProperties.getAccessExpirationMs() / 1000;
        UserSummaryDto userSummary = buildUserSummaryDto(user);

        return new LoginResponse(newAccessToken, newRefreshToken, expiresInSeconds, userSummary);
    }

    /**
     * Revokes active refresh token session.
     */
    public void logout(RefreshTokenRequest request, UserPrincipal currentUser, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String tokenHash = hashToken(request.getRefreshToken());
            userSessionRepository.findByRefreshTokenHash(tokenHash).ifPresent(session -> {
                session.setRevokedAt(now);
                userSessionRepository.saveAndFlush(session);
            });
        }

        if (currentUser != null) {
            User user = userRepository.findById(currentUser.getId()).orElse(null);
            auditService.recordAuthEvent(user, AuditAction.LOGOUT, "USER", currentUser.getId(), ipAddress, userAgent, null);
        }
    }

    /**
     * Changes password, revokes all existing refresh sessions, and audits event.
     */
    public void changePassword(ChangePasswordRequest request, UserPrincipal currentUser, String ipAddress, String userAgent) {
        if (currentUser == null) {
            throw new UnauthorizedException("User must be authenticated to change password.");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as the current password.");
        }

        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(now);
        userRepository.saveAndFlush(user);

        // Revoke all active sessions for this user
        userSessionRepository.revokeAllActiveSessionsForUser(user.getId(), now);

        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionsRevoked", true);
        auditService.recordAuthEvent(user, AuditAction.PASSWORD_CHANGED, "USER", user.getId(), ipAddress, userAgent, meta);
    }

    /**
     * Returns full safe CurrentUserResponse for authenticated user.
     */
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthenticated request.");
        }

        User user = userRepository.findByIdWithDetails(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Employee emp = user.getEmployee();

        DepartmentSummaryDto deptDto = null;
        TeamSummaryDto teamDto = null;

        if (emp != null) {
            if (emp.getDepartment() != null) {
                deptDto = new DepartmentSummaryDto(
                        emp.getDepartment().getId(),
                        emp.getDepartment().getCode(),
                        emp.getDepartment().getName()
                );
            }
            if (emp.getTeam() != null) {
                teamDto = new TeamSummaryDto(
                        emp.getTeam().getId(),
                        emp.getTeam().getCode(),
                        emp.getTeam().getName()
                );
            }
        }

        List<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList())
                : Collections.emptyList();

        Set<String> permissions = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(r -> {
                if (r.getPermissions() != null) {
                    r.getPermissions().forEach(p -> permissions.add(p.getName()));
                }
            });
        }
        List<String> sortedPerms = new ArrayList<>(permissions);
        Collections.sort(sortedPerms);

        return new CurrentUserResponse(
                user.getId(),
                emp != null ? emp.getEmployeeCode() : "",
                emp != null ? emp.getFullName() : "",
                emp != null ? emp.getFirstName() : "",
                emp != null ? emp.getLastName() : "",
                user.getEmail(),
                emp != null ? emp.getDesignation() : "",
                emp != null ? emp.getLocation() : "",
                user.getStatus().name(),
                deptDto,
                teamDto,
                roles,
                sortedPerms
        );
    }

    public static UserSummaryDto buildUserSummaryDto(User user) {
        Employee emp = user.getEmployee();
        String employeeCode = emp != null ? emp.getEmployeeCode() : "";
        String fullName = emp != null ? emp.getFullName() : "";

        List<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList())
                : Collections.emptyList();

        Set<String> perms = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(r -> {
                if (r.getPermissions() != null) {
                    r.getPermissions().forEach(p -> perms.add(p.getName()));
                }
            });
        }
        List<String> sortedPerms = new ArrayList<>(perms);
        Collections.sort(sortedPerms);

        return new UserSummaryDto(
                user.getId(),
                employeeCode,
                fullName,
                user.getEmail(),
                roles,
                sortedPerms
        );
    }

    /**
     * Compute SHA-256 hash of token string.
     */
    public static String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in JVM", e);
        }
    }
}
