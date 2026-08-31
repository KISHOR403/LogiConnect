package com.logiconnect.platform;

import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.security.authentication.UserPrincipal;
import com.logiconnect.platform.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @TestConfiguration
    @RestController
    @RequestMapping("/test/rbac")
    public static class TestRbacController {

        @GetMapping("/admin-only")
        @PreAuthorize("hasRole('SUPER_ADMIN')")
        public ApiResponse<String> adminOnly() {
            return ApiResponse.success("Admin access granted");
        }

        @GetMapping("/permission-required")
        @PreAuthorize("hasAuthority('employee:write')")
        public ApiResponse<String> permissionRequired() {
            return ApiResponse.success("Permission access granted");
        }
    }

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
    @DisplayName("Role-based authorization: User without SUPER_ADMIN role receives 403 Forbidden")
    void testRoleAuthorizationForbidden() throws Exception {
        UserPrincipal employeePrincipal = UserPrincipal.create(
                UUID.randomUUID(),
                "EMP001",
                "test.user@logiconnect.internal",
                "Test",
                "User",
                "password",
                true,
                Set.of("EMPLOYEE"),
                Set.of("employee:read")
        );

        String token = jwtTokenProvider.generateAccessToken(employeePrincipal);

        mockMvc.perform(get("/test/rbac/admin-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Role-based authorization: User with SUPER_ADMIN role receives 200 OK")
    void testRoleAuthorizationGranted() throws Exception {
        UserPrincipal adminPrincipal = UserPrincipal.create(
                UUID.randomUUID(),
                "ADMIN001",
                "admin@logiconnect.internal",
                "Super",
                "Admin",
                "password",
                true,
                Set.of("SUPER_ADMIN"),
                Set.of("employee:read", "employee:write")
        );

        String token = jwtTokenProvider.generateAccessToken(adminPrincipal);

        mockMvc.perform(get("/test/rbac/admin-only")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is("Admin access granted")));
    }

    @Test
    @DisplayName("Permission-based authorization: User with employee:write receives 200 OK")
    void testPermissionAuthorizationGranted() throws Exception {
        UserPrincipal userPrincipal = UserPrincipal.create(
                UUID.randomUUID(),
                "HR001",
                "hr@logiconnect.internal",
                "HR",
                "Admin",
                "password",
                true,
                Set.of("HR_ADMIN"),
                Set.of("employee:write")
        );

        String token = jwtTokenProvider.generateAccessToken(userPrincipal);

        mockMvc.perform(get("/test/rbac/permission-required")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is("Permission access granted")));
    }
}
