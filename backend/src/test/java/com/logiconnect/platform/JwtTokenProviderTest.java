package com.logiconnect.platform;

import com.logiconnect.platform.security.authentication.UserPrincipal;
import com.logiconnect.platform.security.jwt.JwtProperties;
import com.logiconnect.platform.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setAccessExpirationMs(60000); // 1 min
        jwtProperties.setRefreshExpirationMs(600000); // 10 min
        jwtProperties.setIssuer("logiconnect-test");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Generate and validate valid access token")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.create(
                userId,
                "EMP1001",
                "john.doe@logiconnect.internal",
                "John",
                "Doe",
                null,
                true,
                Set.of("MANAGER"),
                Set.of("VIEW_EMPLOYEES", "MANAGE_DEPARTMENTS")
        );

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmployeeCodeFromToken(token)).isEqualTo("EMP1001");
        assertThat(jwtTokenProvider.getTokenTypeFromToken(token)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_ACCESS);
        assertThat(jwtTokenProvider.getRolesFromToken(token)).contains("MANAGER");
        assertThat(jwtTokenProvider.getPermissionsFromToken(token)).contains("VIEW_EMPLOYEES", "MANAGE_DEPARTMENTS");
    }

    @Test
    @DisplayName("Generate and validate refresh token")
    void testGenerateAndValidateRefreshToken() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.create(
                userId,
                "EMP2002",
                "jane.smith@logiconnect.internal",
                "Jane",
                "Smith",
                null,
                true,
                Set.of("EMPLOYEE"),
                Set.of("SEND_MESSAGES")
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmployeeCodeFromToken(refreshToken)).isEqualTo("EMP2002");
        assertThat(jwtTokenProvider.getTokenTypeFromToken(refreshToken)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_REFRESH);
    }

    @Test
    @DisplayName("Reject tampered or malformed JWT token")
    void testRejectInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
