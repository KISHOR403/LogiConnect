package com.logiconnect.platform.security.jwt;

import com.logiconnect.platform.security.authentication.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Production-grade JWT Token Provider for issuing, parsing, and validating Access & Refresh tokens.
 *
 * Adheres strictly to zero-sensitive-data in claims (never storing passwords, hashes, salaries, or private notes).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_EMPLOYEE_CODE = "emp_code";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TOKEN_TYPE = "typ";

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT Secret must not be null or empty.");
        }

        // If secret is at least 32 bytes (256 bits), encode directly; otherwise throw configuration exception
        keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT Secret must be at least 256 bits (32 bytes) long for HMAC-SHA256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate short-lived Access Token for API requests.
     */
    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .claim(CLAIM_USER_ID, principal.getId().toString())
                .claim(CLAIM_EMPLOYEE_CODE, principal.getEmployeeCode())
                .claim(CLAIM_EMAIL, principal.getEmail())
                .claim(CLAIM_ROLES, principal.getRoles())
                .claim(CLAIM_PERMISSIONS, principal.getPermissions())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate long-lived Refresh Token for session continuation.
     */
    public String generateRefreshToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .claim(CLAIM_USER_ID, principal.getId().toString())
                .claim(CLAIM_EMPLOYEE_CODE, principal.getEmployeeCode())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate signature, expiration, and format of JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed structure: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT parsing error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract Claims payload from verified token.
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract User ID (UUID) from token claims.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String uidStr = claims.get(CLAIM_USER_ID, String.class);
        if (uidStr == null) {
            uidStr = claims.getSubject();
        }
        return UUID.fromString(uidStr);
    }

    /**
     * Extract Employee Code from token claims.
     */
    public String getEmployeeCodeFromToken(String token) {
        return getClaimsFromToken(token).get(CLAIM_EMPLOYEE_CODE, String.class);
    }

    /**
     * Extract Token Type (ACCESS vs REFRESH).
     */
    public String getTokenTypeFromToken(String token) {
        return getClaimsFromToken(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    /**
     * Extract Roles set from token claims.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        return roles != null ? Set.copyOf(roles) : Collections.emptySet();
    }

    /**
     * Extract Email from token claims.
     */
    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).get(CLAIM_EMAIL, String.class);
    }

    /**
     * Extract Permissions set from token claims.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        List<String> perms = claims.get(CLAIM_PERMISSIONS, List.class);
        return perms != null ? Set.copyOf(perms) : Collections.emptySet();
    }
}
