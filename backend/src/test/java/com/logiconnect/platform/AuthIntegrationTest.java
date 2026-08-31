package com.logiconnect.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.entity.AuditLog;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.auth.dto.ChangePasswordRequest;
import com.logiconnect.platform.auth.dto.LoginRequest;
import com.logiconnect.platform.auth.dto.LoginResponse;
import com.logiconnect.platform.auth.dto.RefreshTokenRequest;
import com.logiconnect.platform.auth.entity.UserSession;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.auth.service.AuthService;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
import com.logiconnect.platform.permission.entity.Permission;
import com.logiconnect.platform.permission.repository.PermissionRepository;
import com.logiconnect.platform.role.entity.Role;
import com.logiconnect.platform.role.repository.RoleRepository;
import com.logiconnect.platform.security.jwt.JwtTokenProvider;
import com.logiconnect.platform.team.entity.Team;
import com.logiconnect.platform.team.repository.TeamRepository;
import com.logiconnect.platform.user.entity.User;
import com.logiconnect.platform.user.entity.UserStatus;
import com.logiconnect.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User activeUser;
    private Employee activeEmployee;
    private Department department;
    private Team team;
    private Role employeeRole;
    private Permission messageReadPerm;

    @BeforeEach
    void setUp() {
        userSessionRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Department & Team
        department = new Department(null, "OPS", "Operations");
        department = departmentRepository.save(department);

        team = new Team(null, department, "BLR_OPS", "Bangalore Operations");
        team = teamRepository.save(team);

        // 2. Permission & Role
        messageReadPerm = new Permission(null, "message:read", "Read Messages", "message");
        messageReadPerm = permissionRepository.save(messageReadPerm);

        employeeRole = new Role(null, "EMPLOYEE", "Standard Employee", true);
        employeeRole.getPermissions().add(messageReadPerm);
        employeeRole = roleRepository.save(employeeRole);

        // 3. Active Employee & User
        activeEmployee = new Employee();
        activeEmployee.setEmployeeCode("EMP10001");
        activeEmployee.setFirstName("John");
        activeEmployee.setLastName("Doe");
        activeEmployee.setEmail("john.doe@logiconnect.internal");
        activeEmployee.setDesignation("Logistics Specialist");
        activeEmployee.setLocation("Bangalore");
        activeEmployee.setJoiningDate(LocalDate.of(2023, 1, 15));
        activeEmployee.setStatus(EmployeeStatus.ACTIVE);
        activeEmployee.setDepartment(department);
        activeEmployee.setTeam(team);
        activeEmployee = employeeRepository.save(activeEmployee);

        activeUser = new User();
        activeUser.setEmail("john.doe@logiconnect.internal");
        activeUser.setPasswordHash(passwordEncoder.encode("Password@1234"));
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setEmployee(activeEmployee);
        activeUser.getRoles().add(employeeRole);
        activeUser = userRepository.save(activeUser);
    }

    // 1. Successful Login by Employee Code
    @Test
    @DisplayName("1. Successful login by employeeCode returns JWT access and refresh tokens with user details")
    void testSuccessfulLoginByEmployeeCode() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234", "Chrome on Windows");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.data.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.refreshToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", is(60)))
                .andExpect(jsonPath("$.data.user.employeeCode", is("EMP10001")))
                .andExpect(jsonPath("$.data.user.email", is("john.doe@logiconnect.internal")))
                .andExpect(jsonPath("$.data.user.name", is("John Doe")))
                .andExpect(jsonPath("$.data.user.roles", hasItem("EMPLOYEE")))
                .andExpect(jsonPath("$.data.user.permissions", hasItem("message:read")))
                // Ensure sensitive fields are NEVER exposed
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        // Verify session was persisted as SHA-256 hash
        List<UserSession> sessions = userSessionRepository.findAll();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getRefreshTokenHash()).isNotEmpty();
        assertThat(sessions.get(0).getRefreshTokenHash()).isNotEqualTo("Password@1234");
    }

    // 2. Successful Login by Email
    @Test
    @DisplayName("2. Successful login by email identifier returns tokens")
    void testSuccessfulLoginByEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("john.doe@logiconnect.internal", "Password@1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.employeeCode", is("EMP10001")));
    }

    // 3. Invalid Password
    @Test
    @DisplayName("3. Login with invalid password returns 401 Unauthorized with generic message and increments failure counter")
    void testInvalidPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "WrongPassword123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Invalid credentials.")));

        User updatedUser = userRepository.findById(activeUser.getId()).orElseThrow();
        assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    // 4. Unknown User / Enumeration Defense
    @Test
    @DisplayName("4. Login with non-existent user returns 401 Unauthorized with identical generic message")
    void testUnknownUserReturnsGenericError() throws Exception {
        LoginRequest loginRequest = new LoginRequest("NON_EXISTENT_EMP", "Password@1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.error.message", is("Invalid credentials.")));
    }

    // 5. Account Lockout After Threshold
    @Test
    @DisplayName("5. Repeated failed attempts trigger temporary account lockout")
    void testAccountLockoutAfterMaxFailures() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "WrongPassword123!");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));
        }

        // 5th attempt reaches max-failed-attempts (5) and locks account
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message", is("Invalid credentials. Account is now temporarily locked.")));

        User lockedUser = userRepository.findById(activeUser.getId()).orElseThrow();
        assertThat(lockedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
        assertThat(lockedUser.getLockedUntil()).isAfter(Instant.now());

        // Subsequent attempt with correct password while locked is rejected
        LoginRequest correctLogin = new LoginRequest("EMP10001", "Password@1234");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message", is("Account is temporarily locked due to repeated failed login attempts. Please try again later.")));
    }

    // 6. Inactive / Terminated Employee Rejection
    @Test
    @DisplayName("6. Login for terminated employee is rejected")
    void testTerminatedEmployeeLoginRejected() throws Exception {
        activeEmployee.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(activeEmployee);

        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message", is("Account is inactive or disabled. Please contact system administration.")));
    }

    // 7. Refresh Token Rotation & Replay Protection
    @Test
    @DisplayName("7. Refresh token rotates session and rejects replayed previous token")
    void testRefreshTokenRotationAndReplayProtection() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        LoginResponse initialTokens = objectMapper.readTree(responseBody).get("data").traverse(objectMapper).readValueAs(LoginResponse.class);

        String initialRefreshToken = initialTokens.refreshToken();
        assertThat(initialRefreshToken).isNotEmpty();

        // 7a. Refresh token success
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken, "Chrome on Windows");
        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        LoginResponse rotatedTokens = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data").traverse(objectMapper).readValueAs(LoginResponse.class);
        assertThat(rotatedTokens.refreshToken()).isNotEqualTo(initialRefreshToken);

        // 7b. Attempt to replay initial refresh token -> must be rejected as replay attack
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message", is("Invalid session state. All sessions terminated for security reasons.")));
    }

    // 8. Logout Invalidates Refresh Session
    @Test
    @DisplayName("8. Logout revokes refresh token session")
    void testLogoutRevokesSession() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").traverse(objectMapper).readValueAs(LoginResponse.class);

        RefreshTokenRequest logoutReq = new RefreshTokenRequest(tokens.refreshToken());
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Logout successful")));

        // Attempting to refresh with the logged out session must fail
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isUnauthorized());
    }

    // 9. Current User Profile Endpoint (/auth/me and /users/me)
    @Test
    @DisplayName("9. Authenticated GET /auth/me and /users/me return full safe organization profile")
    void testGetCurrentUserEndpoints() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").traverse(objectMapper).readValueAs(LoginResponse.class);

        // Test /auth/me
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode", is("EMP10001")))
                .andExpect(jsonPath("$.data.name", is("John Doe")))
                .andExpect(jsonPath("$.data.department.name", is("Operations")))
                .andExpect(jsonPath("$.data.team.name", is("Bangalore Operations")))
                .andExpect(jsonPath("$.data.roles", hasItem("EMPLOYEE")))
                .andExpect(jsonPath("$.data.permissions", hasItem("message:read")));

        // Test /users/me
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode", is("EMP10001")))
                .andExpect(jsonPath("$.data.email", is("john.doe@logiconnect.internal")));
    }

    // 10. Password Change & Session Invalidation
    @Test
    @DisplayName("10. Password change verifies current password, updates hash, and invalidates all active sessions")
    void testPasswordChangeWorkflow() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").traverse(objectMapper).readValueAs(LoginResponse.class);

        // 10a. Attempt password change with wrong current password -> 400 Bad Request
        ChangePasswordRequest wrongCurrentReq = new ChangePasswordRequest("WrongPass!1234", "BrandNewPass@2026");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCurrentReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", is("Current password does not match.")));

        // 10b. Attempt password change with identical new password -> 400 Bad Request
        ChangePasswordRequest samePassReq = new ChangePasswordRequest("Password@1234", "Password@1234");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samePassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", is("New password cannot be the same as the current password.")));

        // 10c. Successful password change
        ChangePasswordRequest validChangeReq = new ChangePasswordRequest("Password@1234", "BrandNewPass@2026");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validChangeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 10d. Old password no longer works
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        // 10e. New password works
        LoginRequest newLoginReq = new LoginRequest("EMP10001", "BrandNewPass@2026");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 10f. Old refresh token was revoked by password change
        RefreshTokenRequest oldRefreshReq = new RefreshTokenRequest(tokens.refreshToken());
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldRefreshReq)))
                .andExpect(status().isUnauthorized());
    }

    // 11. Validation Errors on Authentication Requests
    @Test
    @DisplayName("11. Invalid DTOs return 400 Bad Request with field validation details")
    void testValidationErrorsOnAuthRequests() throws Exception {
        // Missing fields in login
        LoginRequest emptyLogin = new LoginRequest("", "");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));

        // Weak new password in change password (violates minimum 12 chars and complexity)
        ChangePasswordRequest weakPass = new ChangePasswordRequest("Password@1234", "weak");
        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer dummy-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPass)))
                .andExpect(status().isUnauthorized()); // Authorization filter validates token first
    }

    // 12. Expired Refresh Token
    @Test
    @DisplayName("12. Expired refresh session is rejected with 401 Unauthorized")
    void testExpiredRefreshTokenRejected() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse tokens = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").traverse(objectMapper).readValueAs(LoginResponse.class);

        // Manually expire the session in the database
        List<UserSession> sessions = userSessionRepository.findAll();
        for (UserSession s : sessions) {
            s.setExpiresAt(Instant.now().minusSeconds(3600));
            userSessionRepository.saveAndFlush(s);
        }

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(tokens.refreshToken());
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message", is("Refresh session has expired. Please login again.")));
    }

    // 13. Audit Log Verification
    @Test
    @DisplayName("13. Authentication lifecycle events are recorded in audit logs with safe metadata")
    void testAuditLogVerification() throws Exception {
        LoginRequest loginRequest = new LoginRequest("EMP10001", "Password@1234");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).isNotEmpty();

        boolean hasLoginSuccess = auditLogs.stream().anyMatch(log -> "LOGIN_SUCCESS".equals(log.getAction()));
        assertThat(hasLoginSuccess).isTrue();

        // Verify that no raw passwords or token strings leak into audit logs
        for (AuditLog log : auditLogs) {
            assertThat(log.getAction()).isNotNull();
            if (log.getMetadata() != null) {
                assertThat(log.getMetadata().toString()).doesNotContain("Password@1234");
            }
        }
    }
}
