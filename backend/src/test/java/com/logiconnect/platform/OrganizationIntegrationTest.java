package com.logiconnect.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.audit.entity.AuditLog;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.department.dto.CreateDepartmentRequest;
import com.logiconnect.platform.department.dto.UpdateDepartmentRequest;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.dto.CreateEmployeeRequest;
import com.logiconnect.platform.employee.dto.UpdateEmployeeRequest;
import com.logiconnect.platform.employee.dto.UpdateEmployeeStatusRequest;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
import com.logiconnect.platform.message.repository.MessageAttachmentRepository;
import com.logiconnect.platform.message.repository.MessageRepository;
import com.logiconnect.platform.permission.AppPermission;
import com.logiconnect.platform.permission.entity.Permission;
import com.logiconnect.platform.permission.repository.PermissionRepository;
import com.logiconnect.platform.role.entity.Role;
import com.logiconnect.platform.role.repository.RoleRepository;
import com.logiconnect.platform.security.authentication.UserPrincipal;
import com.logiconnect.platform.security.jwt.JwtTokenProvider;
import com.logiconnect.platform.team.dto.CreateTeamRequest;
import com.logiconnect.platform.team.dto.UpdateTeamRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationIntegrationTest {

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
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private com.logiconnect.platform.channel.repository.ChannelRepository channelRepository;

    @Autowired
    private com.logiconnect.platform.channel.repository.ChannelMemberRepository channelMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageAttachmentRepository attachmentRepository;

    @Autowired
    private com.logiconnect.platform.announcement.repository.AnnouncementRepository announcementRepository;

    @Autowired
    private com.logiconnect.platform.announcement.repository.AnnouncementReadRepository announcementReadRepository;

    @Autowired
    private com.logiconnect.platform.notification.repository.NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String employeeToken;
    private String hrAdminToken;
    private String superAdminToken;
    private String managerToken;

    private Department deptOps;
    private Department deptEngineering;
    private Team teamBlrOps;
    private Team teamMumOps;
    private Employee empAdmin;
    private Employee empHr;
    private Employee empManager;
    private Employee empRegular;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        announcementReadRepository.deleteAll();
        announcementRepository.deleteAll();
        attachmentRepository.deleteAll();
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        conversationRepository.deleteAll();
        channelMemberRepository.deleteAll();
        channelRepository.deleteAll();
        userSessionRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // 1. Create Permissions
        Permission viewEmp = permissionRepository.save(new Permission(null, AppPermission.VIEW_EMPLOYEES, "View Employees", "EMPLOYEE"));
        Permission manageEmp = permissionRepository.save(new Permission(null, AppPermission.MANAGE_EMPLOYEES, "Manage Employees", "EMPLOYEE"));
        Permission viewDept = permissionRepository.save(new Permission(null, AppPermission.VIEW_DEPARTMENTS, "View Departments", "DEPARTMENT"));
        Permission manageDept = permissionRepository.save(new Permission(null, AppPermission.MANAGE_DEPARTMENTS, "Manage Departments", "DEPARTMENT"));
        Permission viewTeam = permissionRepository.save(new Permission(null, AppPermission.VIEW_TEAMS, "View Teams", "TEAM"));
        Permission manageTeam = permissionRepository.save(new Permission(null, AppPermission.MANAGE_TEAMS, "Manage Teams", "TEAM"));

        // 2. Create Roles
        Role employeeRole = new Role(null, "EMPLOYEE", "Standard Employee", true);
        employeeRole.getPermissions().addAll(Set.of(viewEmp, viewDept, viewTeam));
        employeeRole = roleRepository.save(employeeRole);

        Role hrAdminRole = new Role(null, "HR_ADMIN", "HR Administrator", true);
        hrAdminRole.getPermissions().addAll(Set.of(viewEmp, manageEmp, viewDept, manageDept, viewTeam, manageTeam));
        hrAdminRole = roleRepository.save(hrAdminRole);

        Role managerRole = new Role(null, "MANAGER", "Department Manager", true);
        managerRole.getPermissions().addAll(Set.of(viewEmp, viewDept, viewTeam, manageTeam));
        managerRole = roleRepository.save(managerRole);

        Role superAdminRole = new Role(null, "SUPER_ADMIN", "Super Administrator", true);
        superAdminRole.getPermissions().addAll(Set.of(viewEmp, manageEmp, viewDept, manageDept, viewTeam, manageTeam));
        superAdminRole = roleRepository.save(superAdminRole);

        // 3. Create Departments
        deptOps = new Department(null, "OPS", "Operations");
        deptOps.setDescription("Logistics operations and transport hub fleet");
        deptOps = departmentRepository.save(deptOps);

        deptEngineering = new Department(null, "ENG", "Engineering");
        deptEngineering.setDescription("Platform software engineering");
        deptEngineering = departmentRepository.save(deptEngineering);

        // 4. Create Teams
        teamBlrOps = new Team(null, deptOps, "BLR-OPS", "Bangalore Operations");
        teamBlrOps.setDescription("Bangalore fulfillment center team");
        teamBlrOps = teamRepository.save(teamBlrOps);

        teamMumOps = new Team(null, deptOps, "MUM-OPS", "Mumbai Operations");
        teamMumOps.setDescription("Mumbai seaport transit team");
        teamMumOps = teamRepository.save(teamMumOps);

        // 5. Create Employees
        empAdmin = createEmployeeEntity("EMP0001", "System", "Admin", "admin@logiconnect.internal", "Super Admin", deptEngineering, null, null, "Bangalore HQ");
        empHr = createEmployeeEntity("EMP0002", "Sarah", "Connor", "hr.sarah@logiconnect.internal", "HR Lead", deptOps, null, null, "Bangalore HQ");
        empManager = createEmployeeEntity("EMP0003", "Robert", "Walsh", "manager.robert@logiconnect.internal", "Operations Manager", deptOps, null, null, "Bangalore HQ");
        empRegular = createEmployeeEntity("EMP1001", "Kavita", "Sharma", "kavita.sharma@logiconnect.internal", "Dispatch Coordinator", deptOps, teamBlrOps, empManager, "Bangalore Hub");

        // Link manager to department
        deptOps.setManagerId(empManager.getId());
        departmentRepository.save(deptOps);

        // 6. Create Users & Generate Tokens
        User userAdmin = createUserEntity(empAdmin, "admin@logiconnect.internal", superAdminRole);
        User userHr = createUserEntity(empHr, "hr.sarah@logiconnect.internal", hrAdminRole);
        User userManager = createUserEntity(empManager, "manager.robert@logiconnect.internal", managerRole);
        User userRegular = createUserEntity(empRegular, "kavita.sharma@logiconnect.internal", employeeRole);

        UserPrincipal adminPrincipal = UserPrincipal.create(userAdmin.getId(), empAdmin.getEmployeeCode(), userAdmin.getEmail(), empAdmin.getFirstName(), empAdmin.getLastName(), "Password@1234", true, Set.of("ROLE_SUPER_ADMIN"), Set.of(AppPermission.MANAGE_DEPARTMENTS, AppPermission.MANAGE_TEAMS, AppPermission.MANAGE_EMPLOYEES, AppPermission.VIEW_EMPLOYEES, AppPermission.VIEW_DEPARTMENTS, AppPermission.VIEW_TEAMS));
        UserPrincipal hrPrincipal = UserPrincipal.create(userHr.getId(), empHr.getEmployeeCode(), userHr.getEmail(), empHr.getFirstName(), empHr.getLastName(), "Password@1234", true, Set.of("ROLE_HR_ADMIN"), Set.of(AppPermission.MANAGE_DEPARTMENTS, AppPermission.MANAGE_TEAMS, AppPermission.MANAGE_EMPLOYEES, AppPermission.VIEW_EMPLOYEES, AppPermission.VIEW_DEPARTMENTS, AppPermission.VIEW_TEAMS));
        UserPrincipal managerPrincipal = UserPrincipal.create(userManager.getId(), empManager.getEmployeeCode(), userManager.getEmail(), empManager.getFirstName(), empManager.getLastName(), "Password@1234", true, Set.of("ROLE_MANAGER"), Set.of(AppPermission.MANAGE_TEAMS, AppPermission.VIEW_EMPLOYEES, AppPermission.VIEW_DEPARTMENTS, AppPermission.VIEW_TEAMS));
        UserPrincipal employeePrincipal = UserPrincipal.create(userRegular.getId(), empRegular.getEmployeeCode(), userRegular.getEmail(), empRegular.getFirstName(), empRegular.getLastName(), "Password@1234", true, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES, AppPermission.VIEW_DEPARTMENTS, AppPermission.VIEW_TEAMS));

        superAdminToken = jwtTokenProvider.generateAccessToken(adminPrincipal);
        hrAdminToken = jwtTokenProvider.generateAccessToken(hrPrincipal);
        managerToken = jwtTokenProvider.generateAccessToken(managerPrincipal);
        employeeToken = jwtTokenProvider.generateAccessToken(employeePrincipal);
    }

    private Employee createEmployeeEntity(String code, String first, String last, String email, String designation, Department dept, Team team, Employee mgr, String loc) {
        Employee e = new Employee();
        e.setEmployeeCode(code);
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail(email);
        e.setDesignation(designation);
        e.setDepartment(dept);
        e.setTeam(team);
        e.setManager(mgr);
        e.setLocation(loc);
        e.setJoiningDate(LocalDate.of(2023, 1, 15));
        e.setStatus(EmployeeStatus.ACTIVE);
        return employeeRepository.save(e);
    }

    private User createUserEntity(Employee emp, String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Password@1234"));
        u.setStatus(UserStatus.ACTIVE);
        u.setEmployee(emp);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    // ==========================================
    // 1. DEPARTMENT TESTS (1 - 8)
    // ==========================================

    @Test
    @DisplayName("1. List departments returns paginated results with computed team and employee counts")
    void testListDepartments() throws Exception {
        mockMvc.perform(get("/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.content[?(@.code == 'OPS')].employeeCount", hasItem(3)))
                .andExpect(jsonPath("$.data.content[?(@.code == 'OPS')].teamCount", hasItem(2)));
    }

    @Test
    @DisplayName("2. Get department by ID returns detailed department record")
    void testGetDepartmentById() throws Exception {
        mockMvc.perform(get("/departments/" + deptOps.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("OPS")))
                .andExpect(jsonPath("$.data.name", is("Operations")))
                .andExpect(jsonPath("$.data.managerName", is("Robert Walsh")));
    }

    @Test
    @DisplayName("3. Create department by HR_ADMIN succeeds (201 Created)")
    void testCreateDepartmentAuthorized() throws Exception {
        CreateDepartmentRequest req = new CreateDepartmentRequest("FIN", "Finance", "Corporate Finance & Accounts", null);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("FIN")))
                .andExpect(jsonPath("$.data.name", is("Finance")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        assertThat(departmentRepository.existsByCode("FIN")).isTrue();
    }

    @Test
    @DisplayName("4. Create department by regular EMPLOYEE is rejected (403 Forbidden)")
    void testCreateDepartmentUnauthorized() throws Exception {
        CreateDepartmentRequest req = new CreateDepartmentRequest("LEGAL", "Legal", "Corporate Legal", null);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Update department updates name and description successfully")
    void testUpdateDepartment() throws Exception {
        UpdateDepartmentRequest req = new UpdateDepartmentRequest("Operations & Logistics", "Updated description", null, null);

        mockMvc.perform(put("/departments/" + deptOps.getId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Operations & Logistics")));

        Department updated = departmentRepository.findById(deptOps.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Operations & Logistics");
    }

    @Test
    @DisplayName("6. Deactivate department transitions status to INACTIVE")
    void testDeactivateDepartment() throws Exception {
        mockMvc.perform(delete("/departments/" + deptEngineering.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Department updated = departmentRepository.findById(deptEngineering.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("7. Get non-existent department returns 404 Not Found")
    void testDepartmentNotFound() throws Exception {
        mockMvc.perform(get("/departments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("8. Create department validation failure returns 400 Bad Request")
    void testCreateDepartmentValidationFailure() throws Exception {
        CreateDepartmentRequest invalidReq = new CreateDepartmentRequest("", "", null, null);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
    }

    // ==========================================
    // 2. TEAM TESTS (9 - 16)
    // ==========================================

    @Test
    @DisplayName("9. List teams returns paginated list of teams")
    void testListTeams() throws Exception {
        mockMvc.perform(get("/teams")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    @DisplayName("10. Filter teams by departmentId returns only department teams")
    void testFilterTeamsByDepartment() throws Exception {
        mockMvc.perform(get("/teams?departmentId=" + deptOps.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].departmentName", is("Operations")));
    }

    @Test
    @DisplayName("11. Get team by ID returns team details with member count")
    void testGetTeamById() throws Exception {
        mockMvc.perform(get("/teams/" + teamBlrOps.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("BLR-OPS")))
                .andExpect(jsonPath("$.data.name", is("Bangalore Operations")))
                .andExpect(jsonPath("$.data.memberCount", is(1)));
    }

    @Test
    @DisplayName("12. Create team by MANAGER in department succeeds")
    void testCreateTeamByManager() throws Exception {
        CreateTeamRequest req = new CreateTeamRequest(deptOps.getId(), "DEL-OPS", "Delhi Operations", "Delhi transport hub", null);

        mockMvc.perform(post("/teams")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("DEL-OPS")))
                .andExpect(jsonPath("$.data.departmentName", is("Operations")));
    }

    @Test
    @DisplayName("13. Create team by regular EMPLOYEE is rejected (403 Forbidden)")
    void testCreateTeamUnauthorized() throws Exception {
        CreateTeamRequest req = new CreateTeamRequest(deptOps.getId(), "PUN-OPS", "Pune Operations", null, null);

        mockMvc.perform(post("/teams")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Create team in non-existent department returns 404 Not Found")
    void testCreateTeamInvalidDepartment() throws Exception {
        CreateTeamRequest req = new CreateTeamRequest(UUID.randomUUID(), "HYD-OPS", "Hyderabad Operations", null, null);

        mockMvc.perform(post("/teams")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Update team modifies team attributes successfully")
    void testUpdateTeam() throws Exception {
        UpdateTeamRequest req = new UpdateTeamRequest("Bangalore Hub Operations", "Expanded fulfillment", null, null);

        mockMvc.perform(put("/teams/" + teamBlrOps.getId())
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Bangalore Hub Operations")));
    }

    @Test
    @DisplayName("16. Deactivate team transitions status to INACTIVE")
    void testDeactivateTeam() throws Exception {
        mockMvc.perform(delete("/teams/" + teamMumOps.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Team updated = teamRepository.findById(teamMumOps.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");
    }

    // ==========================================
    // 3. EMPLOYEE DIRECTORY TESTS (17 - 25)
    // ==========================================

    @Test
    @DisplayName("17. List employees directory returns safe employee profiles")
    void testListEmployeesDirectory() throws Exception {
        mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(4)))
                .andExpect(jsonPath("$.data.totalElements", is(4)));
    }

    @Test
    @DisplayName("18. Search directory by query matches employeeCode, name, email, or designation")
    void testSearchEmployeesDirectory() throws Exception {
        mockMvc.perform(get("/employees?search=Kavita")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].employeeCode", is("EMP1001")))
                .andExpect(jsonPath("$.data.content[0].firstName", is("Kavita")));

        mockMvc.perform(get("/employees?search=Coordinator")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].designation", is("Dispatch Coordinator")));
    }

    @Test
    @DisplayName("19. Filter directory by department and team")
    void testFilterEmployeesByDeptAndTeam() throws Exception {
        mockMvc.perform(get("/employees?departmentId=" + deptOps.getId() + "&teamId=" + teamBlrOps.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].employeeCode", is("EMP1001")));
    }

    @Test
    @DisplayName("20. Filter directory by location and status")
    void testFilterEmployeesByLocationAndStatus() throws Exception {
        mockMvc.perform(get("/employees?location=Bangalore&status=ACTIVE")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(4)));
    }

    @Test
    @DisplayName("21. Get employee by ID returns profile with resolved department, team, and manager")
    void testGetEmployeeById() throws Exception {
        mockMvc.perform(get("/employees/" + empRegular.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.employeeCode", is("EMP1001")))
                .andExpect(jsonPath("$.data.fullName", is("Kavita Sharma")))
                .andExpect(jsonPath("$.data.departmentName", is("Operations")))
                .andExpect(jsonPath("$.data.teamName", is("Bangalore Operations")))
                .andExpect(jsonPath("$.data.managerName", is("Robert Walsh")));
    }

    @Test
    @DisplayName("22. Security verification: Employee directory NEVER leaks passwords, hashes, salts, or sessions")
    void testSecurityZeroSensitiveFieldsLeaked() throws Exception {
        mockMvc.perform(get("/employees/" + empRegular.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.failedLoginAttempts").doesNotExist())
                .andExpect(jsonPath("$.data.lockedUntil").doesNotExist())
                .andExpect(jsonPath("$.data.refreshTokenHash").doesNotExist());
    }

    @Test
    @DisplayName("23. Onboard new employee by HR_ADMIN creates directory entry (201 Created)")
    void testCreateEmployeeAuthorized() throws Exception {
        CreateEmployeeRequest req = new CreateEmployeeRequest(
                "EMP2001", "Arun", "Kumar", "arun.kumar@logiconnect.internal",
                "+919876543210", "Warehouse Associate", deptOps.getId(),
                teamBlrOps.getId(), empManager.getId(), "Bangalore Hub",
                LocalDate.of(2024, 2, 1)
        );

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.employeeCode", is("EMP2001")))
                .andExpect(jsonPath("$.data.firstName", is("Arun")))
                .andExpect(jsonPath("$.data.departmentName", is("Operations")))
                .andExpect(jsonPath("$.data.teamName", is("Bangalore Operations")));

        assertThat(employeeRepository.existsByEmployeeCode("EMP2001")).isTrue();
    }

    @Test
    @DisplayName("24. Update employee profile modifies designation and location")
    void testUpdateEmployee() throws Exception {
        UpdateEmployeeRequest req = new UpdateEmployeeRequest(
                null, null, null, null, null, "Senior Dispatch Coordinator",
                null, null, null, "Bangalore Central Hub"
        );

        mockMvc.perform(put("/employees/" + empRegular.getId())
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation", is("Senior Dispatch Coordinator")))
                .andExpect(jsonPath("$.data.location", is("Bangalore Central Hub")));
    }

    @Test
    @DisplayName("25. Update employee status transitions employee to ON_LEAVE or TERMINATED")
    void testUpdateEmployeeStatus() throws Exception {
        UpdateEmployeeStatusRequest req = new UpdateEmployeeStatusRequest(
                EmployeeStatus.ON_LEAVE, null, "Maternity leave"
        );

        mockMvc.perform(patch("/employees/" + empRegular.getId() + "/status")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ON_LEAVE")));

        Employee updated = employeeRepository.findById(empRegular.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
    }

    // ==========================================
    // 4. SECURITY & RBAC MATRIX TESTS (26 - 32)
    // ==========================================

    @Test
    @DisplayName("26. Standard employee cannot onboard new employees (403 Forbidden)")
    void testRegularEmployeeCannotCreateEmployee() throws Exception {
        CreateEmployeeRequest req = new CreateEmployeeRequest(
                "EMP9999", "Hacker", "User", "hacker@logiconnect.internal",
                null, "Imposter", deptOps.getId(), null, null, "Remote", LocalDate.now()
        );

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("27. Standard employee cannot update employee status (403 Forbidden)")
    void testRegularEmployeeCannotUpdateStatus() throws Exception {
        UpdateEmployeeStatusRequest req = new UpdateEmployeeStatusRequest(EmployeeStatus.TERMINATED, LocalDate.now(), "Fired");

        mockMvc.perform(patch("/employees/" + empAdmin.getId() + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("28. Unauthenticated requests to /departments return 401 Unauthorized")
    void testUnauthenticatedDepartmentAccess() throws Exception {
        mockMvc.perform(get("/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("29. Unauthenticated requests to /teams return 401 Unauthorized")
    void testUnauthenticatedTeamAccess() throws Exception {
        mockMvc.perform(get("/teams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("30. Unauthenticated requests to /employees return 401 Unauthorized")
    void testUnauthenticatedEmployeeAccess() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("31. SUPER_ADMIN has full permissions to create and manage departments and teams")
    void testSuperAdminAccess() throws Exception {
        CreateDepartmentRequest req = new CreateDepartmentRequest("CORP", "Corporate", "Corporate Office", null);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("32. Get department employees sub-resource returns paginated employee list")
    void testGetDepartmentEmployees() throws Exception {
        mockMvc.perform(get("/departments/" + deptOps.getId() + "/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(3)));
    }

    // ==========================================
    // 5. DATA INTEGRITY & HIERARCHY TESTS (33 - 36)
    // ==========================================

    @Test
    @DisplayName("33. Employee team MUST belong to employee's department (rejected with 400 Bad Request)")
    void testEmployeeTeamMustBelongToDepartment() throws Exception {
        // Create an Engineering team
        Team engTeam = teamRepository.save(new Team(null, deptEngineering, "ENG-CORE", "Core Platform"));

        // Attempt to create an employee with department = OPS, but team = ENG-CORE
        CreateEmployeeRequest invalidReq = new CreateEmployeeRequest(
                "EMP3001", "Vijay", "Nair", "vijay.nair@logiconnect.internal",
                null, "DevOps", deptOps.getId(), engTeam.getId(), null, "Bangalore", LocalDate.now()
        );

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("does not belong to department")));
    }

    @Test
    @DisplayName("34. Duplicate department code is rejected with 409 Conflict")
    void testDuplicateDepartmentCodeRejected() throws Exception {
        CreateDepartmentRequest dupReq = new CreateDepartmentRequest("OPS", "Operations Duplicate", null, null);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")));
    }

    @Test
    @DisplayName("35. Duplicate team code is rejected with 409 Conflict")
    void testDuplicateTeamCodeRejected() throws Exception {
        CreateTeamRequest dupReq = new CreateTeamRequest(deptOps.getId(), "BLR-OPS", "Bangalore Secondary", null, null);

        mockMvc.perform(post("/teams")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")));
    }

    @Test
    @DisplayName("36. Duplicate employee code is rejected with 409 Conflict")
    void testDuplicateEmployeeCodeRejected() throws Exception {
        CreateEmployeeRequest dupReq = new CreateEmployeeRequest(
                "EMP1001", "Duplicate", "Employee", "dup.emp@logiconnect.internal",
                null, "Driver", deptOps.getId(), null, null, "Bangalore", LocalDate.now()
        );

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")));
    }

    // ==========================================
    // 6. AUDIT LOGGING VERIFICATION (37 - 39)
    // ==========================================

    @Test
    @DisplayName("37. Department lifecycle actions generate audit log entries")
    void testDepartmentAuditLogging() throws Exception {
        CreateDepartmentRequest req = new CreateDepartmentRequest("QA", "Quality Assurance", "QA Testing", null);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasDeptCreated = logs.stream().anyMatch(l -> "DEPARTMENT_CREATED".equals(l.getAction()));
        assertThat(hasDeptCreated).isTrue();
    }

    @Test
    @DisplayName("38. Team lifecycle actions generate audit log entries")
    void testTeamAuditLogging() throws Exception {
        CreateTeamRequest req = new CreateTeamRequest(deptOps.getId(), "HYD-OPS", "Hyderabad Ops", null, null);

        mockMvc.perform(post("/teams")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasTeamCreated = logs.stream().anyMatch(l -> "TEAM_CREATED".equals(l.getAction()));
        assertThat(hasTeamCreated).isTrue();
    }

    @Test
    @DisplayName("39. Employee lifecycle actions generate audit log entries")
    void testEmployeeAuditLogging() throws Exception {
        CreateEmployeeRequest req = new CreateEmployeeRequest(
                "EMP5001", "Pooja", "Patel", "pooja.patel@logiconnect.internal",
                null, "Fleet Supervisor", deptOps.getId(), null, null, "Mumbai", LocalDate.now()
        );

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + hrAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasEmpCreated = logs.stream().anyMatch(l -> "EMPLOYEE_CREATED".equals(l.getAction()));
        assertThat(hasEmpCreated).isTrue();
    }
}
