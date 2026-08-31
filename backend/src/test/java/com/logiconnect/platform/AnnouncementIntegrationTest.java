package com.logiconnect.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.announcement.dto.CreateAnnouncementRequest;
import com.logiconnect.platform.announcement.dto.ScheduleAnnouncementRequest;
import com.logiconnect.platform.announcement.dto.UpdateAnnouncementRequest;
import com.logiconnect.platform.announcement.entity.AnnouncementPriority;
import com.logiconnect.platform.announcement.entity.AnnouncementStatus;
import com.logiconnect.platform.announcement.entity.AnnouncementTargetType;
import com.logiconnect.platform.announcement.entity.AnnouncementType;
import com.logiconnect.platform.announcement.repository.AnnouncementReadRepository;
import com.logiconnect.platform.announcement.repository.AnnouncementRepository;
import com.logiconnect.platform.audit.entity.AuditLog;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.channel.repository.ChannelMemberRepository;
import com.logiconnect.platform.channel.repository.ChannelRepository;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
class AnnouncementIntegrationTest {

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
    private AnnouncementRepository announcementRepository;

    @Autowired
    private AnnouncementReadRepository announcementReadRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageAttachmentRepository attachmentRepository;

    @Autowired
    private com.logiconnect.platform.notification.repository.NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Department deptOps;
    private Department deptSupport;
    private Team teamBlrOps;
    private Team teamMumOps;
    private Team teamSupport;

    private User userSuperAdmin;
    private User userHrAdmin;
    private User userOpsManager;
    private User userBlrLead;
    private User userBlrEmp;
    private User userMumEmp;
    private User userSupportEmp;
    private User userTerminatedEmp;

    private String tokenSuperAdmin;
    private String tokenHrAdmin;
    private String tokenOpsManager;
    private String tokenBlrLead;
    private String tokenBlrEmp;
    private String tokenMumEmp;
    private String tokenSupportEmp;
    private String tokenTerminatedEmp;

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

        // 1. Permissions
        Permission sendMsg = permissionRepository.save(new Permission(null, AppPermission.SEND_MESSAGES, "Send Messages", "MESSAGING"));
        Permission sysAdmin = permissionRepository.save(new Permission(null, AppPermission.SYSTEM_ADMIN, "System Administration", "SYSTEM"));
        Permission publishAnnounce = permissionRepository.save(new Permission(null, AppPermission.PUBLISH_ANNOUNCEMENTS, "Publish Announcements", "ANNOUNCEMENT"));
        Permission viewEmp = permissionRepository.save(new Permission(null, AppPermission.VIEW_EMPLOYEES, "View Employees", "EMPLOYEE"));

        // 2. Roles
        Role superAdminRole = roleRepository.save(new Role(null, "SUPER_ADMIN", "Super Admin", true));
        superAdminRole.getPermissions().addAll(Set.of(sendMsg, sysAdmin, publishAnnounce, viewEmp));
        superAdminRole = roleRepository.save(superAdminRole);

        Role hrAdminRole = roleRepository.save(new Role(null, "HR_ADMIN", "HR Admin", true));
        hrAdminRole.getPermissions().addAll(Set.of(sendMsg, publishAnnounce, viewEmp));
        hrAdminRole = roleRepository.save(hrAdminRole);

        Role managerRole = roleRepository.save(new Role(null, "MANAGER", "Department Manager", true));
        managerRole.getPermissions().addAll(Set.of(sendMsg, viewEmp));
        managerRole = roleRepository.save(managerRole);

        Role leadRole = roleRepository.save(new Role(null, "TEAM_LEADER", "Team Leader", true));
        leadRole.getPermissions().addAll(Set.of(sendMsg, viewEmp));
        leadRole = roleRepository.save(leadRole);

        Role employeeRole = roleRepository.save(new Role(null, "EMPLOYEE", "Standard Employee", true));
        employeeRole.getPermissions().addAll(Set.of(sendMsg, viewEmp));
        employeeRole = roleRepository.save(employeeRole);

        // 3. Departments & Teams
        deptOps = departmentRepository.save(new Department(null, "OPS", "Operations"));
        deptSupport = departmentRepository.save(new Department(null, "SUPPORT", "Customer Support"));

        teamBlrOps = teamRepository.save(new Team(null, deptOps, "BLR-OPS", "Bangalore Operations"));
        teamMumOps = teamRepository.save(new Team(null, deptOps, "MUM-OPS", "Mumbai Operations"));
        teamSupport = teamRepository.save(new Team(null, deptSupport, "SUP-DESK", "Support Desk"));

        // 4. Employees & Users
        Employee empSuper = createEmployee("EMP001", "Super", "Admin", "admin@logiconnect.internal", "Administrator", deptOps, null, EmployeeStatus.ACTIVE);
        Employee empHr = createEmployee("EMP002", "Helen", "Rogers", "hr@logiconnect.internal", "HR Lead", deptOps, null, EmployeeStatus.ACTIVE);
        Employee empMgr = createEmployee("EMP003", "Manoj", "Gupta", "manager@logiconnect.internal", "Ops Manager", deptOps, null, EmployeeStatus.ACTIVE);
        Employee empLead = createEmployee("EMP004", "Lata", "Rao", "lead@logiconnect.internal", "Team Leader", deptOps, teamBlrOps, EmployeeStatus.ACTIVE);
        Employee empBlr = createEmployee("EMP005", "Bharat", "Kumar", "bharat@logiconnect.internal", "Dispatcher", deptOps, teamBlrOps, EmployeeStatus.ACTIVE);
        Employee empMum = createEmployee("EMP006", "Meena", "Shah", "meena@logiconnect.internal", "Fleet Coordinator", deptOps, teamMumOps, EmployeeStatus.ACTIVE);
        Employee empSupport = createEmployee("EMP007", "Ravi", "Verma", "ravi@logiconnect.internal", "Support Agent", deptSupport, teamSupport, EmployeeStatus.ACTIVE);
        Employee empTerminated = createEmployee("EMP008", "Kiran", "Das", "kiran@logiconnect.internal", "Ex Employee", deptOps, null, EmployeeStatus.TERMINATED);

        // Link manager & lead to department & team
        deptOps.setManagerId(empMgr.getId());
        departmentRepository.save(deptOps);

        teamBlrOps.setTeamLeadId(empLead.getId());
        teamRepository.save(teamBlrOps);

        userSuperAdmin = createUser(empSuper, superAdminRole, UserStatus.ACTIVE);
        userHrAdmin = createUser(empHr, hrAdminRole, UserStatus.ACTIVE);
        userOpsManager = createUser(empMgr, managerRole, UserStatus.ACTIVE);
        userBlrLead = createUser(empLead, leadRole, UserStatus.ACTIVE);
        userBlrEmp = createUser(empBlr, employeeRole, UserStatus.ACTIVE);
        userMumEmp = createUser(empMum, employeeRole, UserStatus.ACTIVE);
        userSupportEmp = createUser(empSupport, employeeRole, UserStatus.ACTIVE);
        userTerminatedEmp = createUser(empTerminated, employeeRole, UserStatus.ACTIVE);

        tokenSuperAdmin = generateToken(userSuperAdmin, empSuper, Set.of("ROLE_SUPER_ADMIN"), Set.of(AppPermission.PUBLISH_ANNOUNCEMENTS, AppPermission.SYSTEM_ADMIN));
        tokenHrAdmin = generateToken(userHrAdmin, empHr, Set.of("ROLE_HR_ADMIN"), Set.of(AppPermission.PUBLISH_ANNOUNCEMENTS));
        tokenOpsManager = generateToken(userOpsManager, empMgr, Set.of("ROLE_MANAGER"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenBlrLead = generateToken(userBlrLead, empLead, Set.of("ROLE_TEAM_LEADER"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenBlrEmp = generateToken(userBlrEmp, empBlr, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenMumEmp = generateToken(userMumEmp, empMum, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenSupportEmp = generateToken(userSupportEmp, empSupport, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenTerminatedEmp = generateToken(userTerminatedEmp, empTerminated, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
    }

    private Employee createEmployee(String code, String first, String last, String email, String designation, Department dept, Team team, EmployeeStatus status) {
        Employee e = new Employee();
        e.setEmployeeCode(code);
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail(email);
        e.setDesignation(designation);
        e.setDepartment(dept);
        e.setTeam(team);
        e.setLocation("India");
        e.setJoiningDate(LocalDate.of(2023, 1, 15));
        e.setStatus(status);
        return employeeRepository.save(e);
    }

    private User createUser(Employee emp, Role role, UserStatus status) {
        User u = new User();
        u.setEmail(emp.getEmail());
        u.setPasswordHash(passwordEncoder.encode("Password@1234"));
        u.setEmployee(emp);
        u.setStatus(status);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private String generateToken(User user, Employee emp, Set<String> roles, Set<String> perms) {
        UserPrincipal principal = UserPrincipal.create(
                user.getId(),
                emp.getEmployeeCode(),
                user.getEmail(),
                emp.getFirstName(),
                emp.getLastName(),
                "Password@1234",
                user.getStatus() == UserStatus.ACTIVE,
                roles,
                perms
        );
        return jwtTokenProvider.generateAccessToken(principal);
    }

    private UUID createAndPublishAnnouncement(String title, AnnouncementTargetType targetType, UUID deptId, UUID teamId, boolean reqAck, String token) throws Exception {
        CreateAnnouncementRequest createReq = new CreateAnnouncementRequest(
                title,
                "Official notice content for " + title,
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.HIGH,
                targetType,
                deptId,
                teamId,
                reqAck,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();
        UUID id = UUID.fromString(idStr);

        mockMvc.perform(post("/announcements/" + id + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return id;
    }

    // =========================================================================
    // TESTS: TARGETING & AUDIENCE READ VISIBILITY
    // =========================================================================

    @Test
    @DisplayName("1. Employee can read company announcement")
    void employeeCanReadCompanyAnnouncement() throws Exception {
        UUID annId = createAndPublishAnnouncement("Annual Town Hall", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Annual Town Hall"))
                .andExpect(jsonPath("$.data.targetType").value("COMPANY"))
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    @DisplayName("2. Employee can read their department announcement")
    void employeeCanReadTheirDepartmentAnnouncement() throws Exception {
        UUID annId = createAndPublishAnnouncement("Ops Shift SOP", AnnouncementTargetType.DEPARTMENT, deptOps.getId(), null, false, tokenOpsManager);

        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Ops Shift SOP"))
                .andExpect(jsonPath("$.data.departmentId").value(deptOps.getId().toString()));
    }

    @Test
    @DisplayName("3. Employee can read their team announcement")
    void employeeCanReadTheirTeamAnnouncement() throws Exception {
        UUID annId = createAndPublishAnnouncement("BLR Morning Timing", AnnouncementTargetType.TEAM, null, teamBlrOps.getId(), false, tokenBlrLead);

        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("BLR Morning Timing"))
                .andExpect(jsonPath("$.data.teamId").value(teamBlrOps.getId().toString()));
    }

    @Test
    @DisplayName("4. Employee cannot read another department announcement")
    void employeeCannotReadAnotherDepartmentAnnouncement() throws Exception {
        UUID annId = createAndPublishAnnouncement("Support SLA Update", AnnouncementTargetType.DEPARTMENT, deptSupport.getId(), null, false, tokenHrAdmin);

        // BLR employee belongs to OPS department, not SUPPORT
        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Employee cannot read another team announcement")
    void employeeCannotReadAnotherTeamAnnouncement() throws Exception {
        UUID annId = createAndPublishAnnouncement("Mumbai Hub Fleet Maintenance", AnnouncementTargetType.TEAM, null, teamMumOps.getId(), false, tokenOpsManager);

        // Bharat is BLR ops team, not MUM ops
        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Private/targeted announcement does not leak existence")
    void privateTargetedAnnouncementDoesNotLeakExistence() throws Exception {
        UUID annId = createAndPublishAnnouncement("Confidential Support Restructuring", AnnouncementTargetType.DEPARTMENT, deptSupport.getId(), null, false, tokenHrAdmin);

        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // TESTS: CREATION & TARGETING AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("7. Unauthorized employee cannot create company announcement")
    void unauthorizedEmployeeCannotCreateCompanyAnnouncement() throws Exception {
        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Unauthorized Company Broadcast",
                "Some text",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Unauthorized employee cannot create department announcement")
    void unauthorizedEmployeeCannotCreateDepartmentAnnouncement() throws Exception {
        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Unauthorized Dept Notice",
                "Some text",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.DEPARTMENT,
                deptOps.getId(),
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Unauthorized employee cannot create team announcement")
    void unauthorizedEmployeeCannotCreateTeamAnnouncement() throws Exception {
        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Unauthorized Team Notice",
                "Some text",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.TEAM,
                null,
                teamBlrOps.getId(),
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Authorized manager can create appropriate department and team announcement")
    void authorizedManagerCanCreateAppropriateDepartmentAndTeamAnnouncement() throws Exception {
        CreateAnnouncementRequest deptReq = new CreateAnnouncementRequest(
                "Ops Dept Guidelines",
                "Guidelines text",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.HIGH,
                AnnouncementTargetType.DEPARTMENT,
                deptOps.getId(),
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenOpsManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Ops Dept Guidelines"));

        // Manager of Ops can also create for BLR-OPS team
        CreateAnnouncementRequest teamReq = new CreateAnnouncementRequest(
                "BLR Team Handoff",
                "Team handoff instructions",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.TEAM,
                null,
                teamBlrOps.getId(),
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenOpsManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teamReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("BLR Team Handoff"));

        // But Ops manager cannot create for Customer Support department!
        CreateAnnouncementRequest supportReq = new CreateAnnouncementRequest(
                "Support Forbidden Announcement",
                "Text",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.DEPARTMENT,
                deptSupport.getId(),
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenOpsManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(supportReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // TESTS: PUBLISHING & STATE LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("11. Unauthorized employee cannot publish")
    void unauthorizedEmployeeCannotPublish() throws Exception {
        CreateAnnouncementRequest createReq = new CreateAnnouncementRequest(
                "Draft by Admin",
                "Content",
                AnnouncementType.POLICY,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        // Regular employee cannot publish
        mockMvc.perform(post("/announcements/" + idStr + "/publish")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Authorized user can publish")
    void authorizedUserCanPublish() throws Exception {
        CreateAnnouncementRequest createReq = new CreateAnnouncementRequest(
                "Fire Safety SOP",
                "Content",
                AnnouncementType.SAFETY,
                AnnouncementPriority.URGENT,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                true,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/announcements/" + idStr + "/publish")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.publishedById").value(userHrAdmin.getId().toString()));
    }

    @Test
    @DisplayName("13. Target authorization is rechecked during publication")
    void targetAuthorizationIsRecheckedDuringPublication() throws Exception {
        CreateAnnouncementRequest createReq = new CreateAnnouncementRequest(
                "BLR Team Note",
                "Content",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.TEAM,
                null,
                teamBlrOps.getId(),
                false,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrLead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        // Mum employee cannot publish BLR team announcement
        mockMvc.perform(post("/announcements/" + idStr + "/publish")
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. Invalid target combination returns 400")
    void invalidTargetCombinationReturns400() throws Exception {
        // Company target with departmentId
        CreateAnnouncementRequest req1 = new CreateAnnouncementRequest(
                "Invalid 1",
                "Content",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                deptOps.getId(),
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest());

        // Department target without departmentId
        CreateAnnouncementRequest req2 = new CreateAnnouncementRequest(
                "Invalid 2",
                "Content",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.DEPARTMENT,
                null,
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest());

        // Team target without teamId
        CreateAnnouncementRequest req3 = new CreateAnnouncementRequest(
                "Invalid 3",
                "Content",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.TEAM,
                null,
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("15. Draft is invisible to ordinary employees")
    void draftIsInvisibleToOrdinaryEmployees() throws Exception {
        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Secret Draft",
                "Draft text",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        // Feed does not return draft to employee
        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(hasItem(hasEntry("id", idStr)))));

        // Direct get returns 404
        mockMvc.perform(get("/announcements/" + idStr)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("16. Scheduled announcement is invisible before publication")
    void scheduledAnnouncementIsInvisibleBeforePublication() throws Exception {
        ScheduleAnnouncementRequest schedReq = new ScheduleAnnouncementRequest(Instant.now().plus(2, ChronoUnit.DAYS));

        CreateAnnouncementRequest createReq = new CreateAnnouncementRequest(
                "Future Broadcast",
                "Content",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                schedReq.scheduledAt(),
                null
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        // Invisible in normal feed
        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(hasItem(hasEntry("id", idStr)))));

        // Direct get returns 404 for ordinary employee
        mockMvc.perform(get("/announcements/" + idStr)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("17. Cancelled announcement is not shown in normal feed")
    void cancelledAnnouncementIsNotShownInNormalFeed() throws Exception {
        UUID annId = createAndPublishAnnouncement("Cancelled Event", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/cancel")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(hasItem(hasEntry("id", annId.toString())))));
    }

    @Test
    @DisplayName("18. Archived announcement is not shown in active feed")
    void archivedAnnouncementIsNotShownInActiveFeed() throws Exception {
        UUID annId = createAndPublishAnnouncement("Archived Notice", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/archive")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(hasItem(hasEntry("id", annId.toString())))));
    }

    // =========================================================================
    // TESTS: READ TRACKING & MANDATORY ACKNOWLEDGEMENT
    // =========================================================================

    @Test
    @DisplayName("19. Employee can mark announcement as read")
    void employeeCanMarkAnnouncementAsRead() throws Exception {
        UUID annId = createAndPublishAnnouncement("Important Policy", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
    }

    @Test
    @DisplayName("20. Read timestamp is not incorrectly overwritten")
    void readTimestampIsNotIncorrectlyOverwritten() throws Exception {
        UUID annId = createAndPublishAnnouncement("Stable Read Stamp Notice", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        MvcResult firstRead = mockMvc.perform(post("/announcements/" + annId + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andReturn();

        String readAt1 = objectMapper.readTree(firstRead.getResponse().getContentAsString()).get("data").get("readAt").asText();

        // Second read call
        MvcResult secondRead = mockMvc.perform(post("/announcements/" + annId + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andReturn();

        String readAt2 = objectMapper.readTree(secondRead.getResponse().getContentAsString()).get("data").get("readAt").asText();

        Instant instant1 = Instant.parse(readAt1).truncatedTo(ChronoUnit.MILLIS);
        Instant instant2 = Instant.parse(readAt2).truncatedTo(ChronoUnit.MILLIS);
        assertThat(instant1).isEqualTo(instant2);
    }

    @Test
    @DisplayName("21. Employee can acknowledge required announcement")
    void employeeCanAcknowledgeRequiredAnnouncement() throws Exception {
        UUID annId = createAndPublishAnnouncement("Mandatory Health Policy", AnnouncementTargetType.COMPANY, null, null, true, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/acknowledge")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.acknowledged").value(true))
                .andExpect(jsonPath("$.data.acknowledgedAt").isNotEmpty());

        // Check announcement details reflection
        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acknowledged").value(true))
                .andExpect(jsonPath("$.data.acknowledgedAt").isNotEmpty());
    }

    @Test
    @DisplayName("22. Employee cannot acknowledge for another employee")
    void employeeCannotAcknowledgeForAnotherEmployee() throws Exception {
        UUID annId = createAndPublishAnnouncement("Personal Ack Requirement", AnnouncementTargetType.COMPANY, null, null, true, tokenHrAdmin);

        // Bharat acknowledges
        mockMvc.perform(post("/announcements/" + annId + "/acknowledge")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk());

        // Meena's view must still reflect unacknowledged
        mockMvc.perform(get("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acknowledged").value(false));
    }

    @Test
    @DisplayName("23. Non-required announcement cannot be falsely acknowledged")
    void nonRequiredAnnouncementCannotBeFalselyAcknowledged() throws Exception {
        UUID annId = createAndPublishAnnouncement("Optional Social Gathering", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/acknowledge")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("24. Employee cannot see another employee's acknowledgement status")
    void employeeCannotSeeAnotherEmployeesAcknowledgementStatus() throws Exception {
        UUID annId = createAndPublishAnnouncement("HR Secret Policy", AnnouncementTargetType.COMPANY, null, null, true, tokenHrAdmin);

        // Ordinary employee Bharat is denied viewing acknowledgement reporting
        mockMvc.perform(get("/announcements/" + annId + "/acknowledgements")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("25. Authorized admin can view acknowledgement statistics")
    void authorizedAdminCanViewAcknowledgementStatistics() throws Exception {
        UUID annId = createAndPublishAnnouncement("Safety Sign-off", AnnouncementTargetType.COMPANY, null, null, true, tokenHrAdmin);

        // Bharat acknowledges
        mockMvc.perform(post("/announcements/" + annId + "/acknowledge")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk());

        // HR Admin views compliance statistics
        mockMvc.perform(get("/announcements/" + annId + "/acknowledgements")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEligible").isNumber())
                .andExpect(jsonPath("$.data.acknowledgedCount").value(1))
                .andExpect(jsonPath("$.data.employeeStatuses", hasSize(greaterThanOrEqualTo(1))));
    }

    // =========================================================================
    // TESTS: PAGINATION, EXPIRATION, AUDIT, & SECURITY
    // =========================================================================

    @Test
    @DisplayName("26. Pagination works")
    void paginationWorks() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createAndPublishAnnouncement("Batch Notice " + i, AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);
        }

        mockMvc.perform(get("/announcements?page=0&size=2")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("27. Maximum page size is enforced")
    void maximumPageSizeIsEnforced() throws Exception {
        mockMvc.perform(get("/announcements?page=0&size=500")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("28. Expired announcement is excluded from active feed")
    void expiredAnnouncementIsExcludedFromActiveFeed() throws Exception {
        CreateAnnouncementRequest expReq = new CreateAnnouncementRequest(
                "Expired Notice",
                "Content",
                AnnouncementType.MAINTENANCE,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        MvcResult result = mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String idStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();
        UUID id = UUID.fromString(idStr);

        mockMvc.perform(post("/announcements/" + id + "/publish")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk());

        // Artificially expire the announcement in repository
        var ann = announcementRepository.findById(id).orElseThrow();
        ann.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        announcementRepository.save(ann);

        // Excluded from employee active feed
        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(hasItem(hasEntry("id", id.toString())))));
    }

    @Test
    @DisplayName("29. Announcement audit events are generated")
    void announcementAuditEventsAreGenerated() throws Exception {
        UUID annId = createAndPublishAnnouncement("Audited Broadcast", AnnouncementTargetType.COMPANY, null, null, true, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/acknowledge")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        List<String> actions = logs.stream().map(AuditLog::getAction).toList();

        assertThat(actions).contains(
                "ANNOUNCEMENT_CREATED",
                "ANNOUNCEMENT_PUBLISHED",
                "ANNOUNCEMENT_ACKNOWLEDGED"
        );
    }

    @Test
    @DisplayName("30. Announcement content is not leaked into audit logs")
    void announcementContentIsNotLeakedIntoAuditLogs() throws Exception {
        String secretBody = "TopSecretExecutiveContent12345";
        CreateAnnouncementRequest createReq = new CreateAnnouncementRequest(
                "Audit Privacy Test",
                secretBody,
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        mockMvc.perform(post("/announcements")
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findAll();
        for (AuditLog l : logs) {
            if (l.getMetadata() != null) {
                assertThat(l.getMetadata().toString()).doesNotContain(secretBody);
            }
        }
    }

    @Test
    @DisplayName("31. IDOR protection for announcement IDs")
    void idorProtectionForAnnouncementIds() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/announcements/" + randomId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("32. Inactive/terminated employee cannot access announcements")
    void inactiveOrTerminatedEmployeeCannotAccessAnnouncements() throws Exception {
        mockMvc.perform(get("/announcements")
                        .header("Authorization", "Bearer " + tokenTerminatedEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("33. Invalid status transitions are rejected")
    void invalidStatusTransitionsAreRejected() throws Exception {
        UUID annId = createAndPublishAnnouncement("Lifecycle Test", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        // Cancel it
        mockMvc.perform(post("/announcements/" + annId + "/cancel")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk());

        // Cannot re-publish CANCELLED announcement
        mockMvc.perform(post("/announcements/" + annId + "/publish")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("34. Published announcement cannot be dangerously modified")
    void publishedAnnouncementCannotBeDangerouslyModified() throws Exception {
        UUID annId = createAndPublishAnnouncement("Immutable Broadcast", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        UpdateAnnouncementRequest updateReq = new UpdateAnnouncementRequest(
                "Tampered Title",
                "Tampered Content",
                AnnouncementType.EMERGENCY,
                AnnouncementPriority.EMERGENCY,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        mockMvc.perform(put("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("35. Archived announcement is immutable")
    void archivedAnnouncementIsImmutable() throws Exception {
        UUID annId = createAndPublishAnnouncement("Archived Notice", AnnouncementTargetType.COMPANY, null, null, false, tokenHrAdmin);

        mockMvc.perform(post("/announcements/" + annId + "/archive")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isOk());

        UpdateAnnouncementRequest updateReq = new UpdateAnnouncementRequest(
                "Modified Archived Title",
                "Content",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(put("/announcements/" + annId)
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/announcements/" + annId + "/cancel")
                        .header("Authorization", "Bearer " + tokenHrAdmin))
                .andExpect(status().isBadRequest());
    }
}
