package com.logiconnect.platform;

import com.logiconnect.platform.security.authentication.UserPrincipal;
import com.logiconnect.platform.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized with ApiError structure")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/departments").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.path", is("/departments")));
    }

    @Test
    @DisplayName("Request with invalid Bearer token returns 401 Unauthorized")
    void testInvalidBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer invalid-token-string")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Request with valid Bearer token passes security filter")
    void testValidBearerTokenPassesSecurityFilter() throws Exception {
        UserPrincipal principal = UserPrincipal.create(
                UUID.randomUUID(),
                "EMP001",
                "test.user@logiconnect.internal",
                "Test",
                "User",
                "password",
                true,
                Set.of("EMPLOYEE"),
                Set.of("VIEW_DEPARTMENTS")
        );

        String validAccessToken = jwtTokenProvider.generateAccessToken(principal);

        // Since /departments business controller is not yet implemented, a valid token will pass security and result in 404 (NoResourceFound) rather than 401 Unauthorized
        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }
}
