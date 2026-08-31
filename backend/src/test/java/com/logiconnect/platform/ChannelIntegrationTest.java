package com.logiconnect.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.audit.entity.AuditLog;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.channel.dto.AddChannelMemberRequest;
import com.logiconnect.platform.channel.dto.CreateChannelRequest;
import com.logiconnect.platform.channel.dto.UpdateChannelRequest;
import com.logiconnect.platform.channel.entity.Channel;
import com.logiconnect.platform.channel.entity.ChannelMemberRole;
import com.logiconnect.platform.channel.entity.ChannelStatus;
import com.logiconnect.platform.channel.entity.ChannelType;
import com.logiconnect.platform.channel.repository.ChannelMemberRepository;
import com.logiconnect.platform.channel.repository.ChannelRepository;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
import com.logiconnect.platform.message.dto.EditMessageRequest;
import com.logiconnect.platform.message.dto.SendMessageRequest;
import com.logiconnect.platform.message.entity.Message;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChannelIntegrationTest {

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
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageAttachmentRepository attachmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Department deptOps;
    private Department deptSupport;
    private Team teamBlrOps;
    private Team teamMumOps;

    private User userSuperAdmin;
    private User userHrAdmin;
    private User userOpsManager;
    private User userBlrLead;
    private User userBlrEmp;
    private User userMumEmp;

    private String tokenSuperAdmin;
    private String tokenHrAdmin;
    private String tokenOpsManager;
    private String tokenBlrLead;
    private String tokenBlrEmp;
    private String tokenMumEmp;

    @BeforeEach
    void setUp() {
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
        Permission manageChan = permissionRepository.save(new Permission(null, AppPermission.MANAGE_CHANNELS, "Manage Channels", "CHANNELS"));
        Permission sysAdmin = permissionRepository.save(new Permission(null, AppPermission.SYSTEM_ADMIN, "System Administration", "SYSTEM"));
        Permission viewEmp = permissionRepository.save(new Permission(null, AppPermission.VIEW_EMPLOYEES, "View Employees", "EMPLOYEE"));

        // 2. Roles
        Role superAdminRole = roleRepository.save(new Role(null, "SUPER_ADMIN", "Super Admin", true));
        superAdminRole.getPermissions().addAll(Set.of(sendMsg, manageChan, sysAdmin, viewEmp));
        superAdminRole = roleRepository.save(superAdminRole);

        Role hrAdminRole = roleRepository.save(new Role(null, "HR_ADMIN", "HR Admin", true));
        hrAdminRole.getPermissions().addAll(Set.of(sendMsg, viewEmp));
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

        // 4. Employees & Users
        Employee empSuper = createEmployee("EMP001", "Super", "Admin", "admin@logiconnect.internal", "Administrator", deptOps, null);
        Employee empHr = createEmployee("EMP002", "Helen", "Rogers", "hr@logiconnect.internal", "HR Lead", deptOps, null);
        Employee empMgr = createEmployee("EMP003", "Manoj", "Gupta", "manager@logiconnect.internal", "Ops Manager", deptOps, null);
        Employee empLead = createEmployee("EMP004", "Lata", "Rao", "lead@logiconnect.internal", "Team Leader", deptOps, teamBlrOps);
        Employee empBlr = createEmployee("EMP005", "Bharat", "Kumar", "bharat@logiconnect.internal", "Dispatcher", deptOps, teamBlrOps);
        Employee empMum = createEmployee("EMP006", "Meena", "Shah", "meena@logiconnect.internal", "Fleet Coordinator", deptOps, teamMumOps);

        userSuperAdmin = createUser(empSuper, superAdminRole);
        userHrAdmin = createUser(empHr, hrAdminRole);
        userOpsManager = createUser(empMgr, managerRole);
        userBlrLead = createUser(empLead, leadRole);
        userBlrEmp = createUser(empBlr, employeeRole);
        userMumEmp = createUser(empMum, employeeRole);

        tokenSuperAdmin = generateToken(userSuperAdmin, empSuper, Set.of("ROLE_SUPER_ADMIN"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.MANAGE_CHANNELS, AppPermission.SYSTEM_ADMIN));
        tokenHrAdmin = generateToken(userHrAdmin, empHr, Set.of("ROLE_HR_ADMIN"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.VIEW_EMPLOYEES));
        tokenOpsManager = generateToken(userOpsManager, empMgr, Set.of("ROLE_MANAGER"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.VIEW_EMPLOYEES));
        tokenBlrLead = generateToken(userBlrLead, empLead, Set.of("ROLE_TEAM_LEADER"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.VIEW_EMPLOYEES));
        tokenBlrEmp = generateToken(userBlrEmp, empBlr, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.VIEW_EMPLOYEES));
        tokenMumEmp = generateToken(userMumEmp, empMum, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.VIEW_EMPLOYEES));
    }

    private Employee createEmployee(String code, String first, String last, String email, String designation, Department dept, Team team) {
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
        e.setStatus(EmployeeStatus.ACTIVE);
        return employeeRepository.save(e);
    }

    private User createUser(Employee emp, Role role) {
        User u = new User();
        u.setEmail(emp.getEmail());
        u.setPasswordHash(passwordEncoder.encode("Password@1234"));
        u.setStatus(UserStatus.ACTIVE);
        u.setEmployee(emp);
        u.getRoles().add(role);
        return userRepository.save(u);
    }

    private String generateToken(User user, Employee emp, Set<String> roles, Set<String> perms) {
        UserPrincipal principal = UserPrincipal.create(
                user.getId(), emp.getEmployeeCode(), user.getEmail(),
                emp.getFirstName(), emp.getLastName(), "Password@1234",
                true, roles, perms
        );
        return jwtTokenProvider.generateAccessToken(principal);
    }

    // ==========================================
    // 1. DISCOVERY & ACCESS TESTS (1 - 7)
    // ==========================================

    @Test
    @DisplayName("1. Employee can discover company-wide public channel")
    void testDiscoverCompanyChannel() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("General Announcements", null, "Company broadcast", ChannelType.COMPANY, null, null, false);
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Any active employee discovers company channel
        mockMvc.perform(get("/channels")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'General Announcements')]", not(empty())));
    }

    @Test
    @DisplayName("2. Employee can discover eligible department channel")
    void testDiscoverDepartmentChannel() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Operations General", null, "Ops department room", ChannelType.DEPARTMENT, deptOps.getId(), null, false);
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Employee in Operations discovers it
        mockMvc.perform(get("/channels")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Operations General')]", not(empty())));
    }

    @Test
    @DisplayName("3. Employee can discover eligible team channel")
    void testDiscoverTeamChannel() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Bangalore Hub Operations", null, "BLR team room", ChannelType.TEAM, null, teamBlrOps.getId(), false);
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenBlrLead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // BLR Employee discovers BLR Team channel
        mockMvc.perform(get("/channels")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Bangalore Hub Operations')]", not(empty())));

        // MUM Employee CANNOT discover BLR Team channel
        mockMvc.perform(get("/channels")
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Bangalore Hub Operations')]", empty()));
    }

    @Test
    @DisplayName("4. Employee cannot discover unauthorized private channel")
    void testPrivateChannelHiddenFromNonMembers() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Secret Leadership", null, "Confidential room", ChannelType.PRIVATE, null, null, false);
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Regular employee does not discover private channel
        mockMvc.perform(get("/channels")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Secret Leadership')]", empty()));
    }

    @Test
    @DisplayName("5. Cross-department access denial returns 403 Forbidden")
    void testCrossDepartmentAccessDenied() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Support Hotline", null, "Customer support only", ChannelType.DEPARTMENT, deptSupport.getId(), null, false);
        MvcResult res = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Employee in Operations tries to get Customer Support channel -> 403 Forbidden
        mockMvc.perform(get("/channels/" + chanId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Cross-team access denial returns 403 Forbidden")
    void testCrossTeamAccessDenied() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("BLR Ops Internal", null, "BLR only", ChannelType.TEAM, null, teamBlrOps.getId(), false);
        MvcResult res = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenBlrLead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Mumbai Employee tries to read BLR Team channel -> 403 Forbidden
        mockMvc.perform(get("/channels/" + chanId)
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Private channel requires explicit membership for access")
    void testPrivateChannelRequiresMembership() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Audit Room", null, "Auditors only", ChannelType.PRIVATE, null, null, false);
        MvcResult res = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Non-member access -> 403 Forbidden
        mockMvc.perform(get("/channels/" + chanId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isForbidden());

        // Super Admin adds employee to private channel
        mockMvc.perform(post("/channels/" + chanId + "/members")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddChannelMemberRequest(userBlrEmp.getId(), ChannelMemberRole.MEMBER))))
                .andExpect(status().isOk());

        // Now employee can access private channel
        mockMvc.perform(get("/channels/" + chanId)
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Audit Room")));
    }

    // ==========================================
    // 2. CREATION AUTHORIZATION (8 - 10)
    // ==========================================

    @Test
    @DisplayName("8. SUPER_ADMIN, HR_ADMIN, MANAGER, and TEAM_LEADER can create authorized channels")
    void testChannelCreationRoleMatrix() throws Exception {
        // HR Admin creates COMPANY channel
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenHrAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("HR Announcements", null, null, ChannelType.COMPANY, null, null, false))))
                .andExpect(status().isCreated());

        // Manager creates DEPARTMENT channel for own department
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenOpsManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("Ops Strategy", null, null, ChannelType.DEPARTMENT, deptOps.getId(), null, false))))
                .andExpect(status().isCreated());

        // Team Leader creates TEAM channel for own team
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenBlrLead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("BLR Daily Dispatch", null, null, ChannelType.TEAM, null, teamBlrOps.getId(), false))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("9. Regular EMPLOYEE cannot create organizational channels (403 Forbidden)")
    void testEmployeeCannotCreateChannel() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Unauthorized Channel", null, null, ChannelType.COMPANY, null, null, false);
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Manager cannot create department channel for another department")
    void testManagerCannotCreateOtherDeptChannel() throws Exception {
        CreateChannelRequest req = new CreateChannelRequest("Support Strategy", null, null, ChannelType.DEPARTMENT, deptSupport.getId(), null, false);
        mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenOpsManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. CHANNEL MESSAGES (11 - 20)
    // ==========================================

    @Test
    @DisplayName("11. Eligible member can post message to channel")
    void testMemberCanPostMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("BLR Ops Chat", null, null, ChannelType.TEAM, null, teamBlrOps.getId(), false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenBlrLead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        SendMessageRequest msgReq = new SendMessageRequest("Morning fleet schedule ready.", null, null, null);
        mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content", is("Morning fleet schedule ready.")))
                .andExpect(jsonPath("$.data.channelId", is(chanId)))
                .andExpect(jsonPath("$.data.sender.employeeCode", is("EMP005")));
    }

    @Test
    @DisplayName("12. Unauthorized user cannot send message to channel (403 Forbidden)")
    void testUnauthorizedUserCannotPostMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("BLR Ops Chat 2", null, null, ChannelType.TEAM, null, teamBlrOps.getId(), false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenBlrLead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Mumbai employee attempts to post in BLR channel -> 403 Forbidden
        SendMessageRequest msgReq = new SendMessageRequest("Intruder text", null, null, null);
        mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenMumEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. Sender identity is strictly derived from security context (impersonation rejected)")
    void testSenderIdentityIntegrity() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Company All", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Pass spoofed senderId
        String spoofJson = "{\"content\":\"Authentic text\",\"senderId\":\"" + userSuperAdmin.getId() + "\"}";
        mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sender.id", is(userBlrEmp.getId().toString())))
                .andExpect(jsonPath("$.data.sender.employeeCode", is("EMP005")));
    }

    @Test
    @DisplayName("14. Channel message history pagination and stable ordering")
    void testChannelMessagePagination() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Company General Feed", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/channels/" + chanId + "/messages")
                    .header("Authorization", "Bearer " + tokenSuperAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SendMessageRequest("Announcement #" + i, null, null, null))));
        }

        mockMvc.perform(get("/channels/" + chanId + "/messages?page=0&size=2&direction=asc")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(4)))
                .andExpect(jsonPath("$.data.content[0].content", is("Announcement #1")))
                .andExpect(jsonPath("$.data.content[1].content", is("Announcement #2")));
    }

    @Test
    @DisplayName("15. Sender can edit own channel message")
    void testSenderCanEditOwnChannelMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Fleet Updates", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Initial message", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(put("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditMessageRequest("Edited message text"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", is("Edited message text")))
                .andExpect(jsonPath("$.data.editedAt", notNullValue()));
    }

    @Test
    @DisplayName("16. User cannot edit another user's channel message (403 Forbidden)")
    void testUserCannotEditAnotherChannelMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Ops Hub", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Bharat original", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Meena tries to edit Bharat's message -> 403 Forbidden
        mockMvc.perform(put("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenMumEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EditMessageRequest("Tampered text"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("17. Channel Moderator/Admin can soft-delete any message (Moderation Delete)")
    void testModeratorCanDeleteChannelMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Moderated Room", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Bharat posts message
        MvcResult msgRes = mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Inappropriate text", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Super Admin (Moderator/Admin) deletes Bharat's message
        mockMvc.perform(delete("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDeleted", is(true)));

        // Verify message is soft-deleted
        Message dbMsg = messageRepository.findById(UUID.fromString(msgId)).orElseThrow();
        assertThat(dbMsg.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("18. Non-moderator cannot delete another employee's channel message (403 Forbidden)")
    void testNonModeratorCannotDeleteAnotherMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Public Chat", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Bharat post", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Meena (normal member) attempts to delete Bharat's post -> 403 Forbidden
        mockMvc.perform(delete("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("19. Pinned messages foundation: Channel Admin/Moderator can pin and unpin message")
    void testPinAndUnpinMessage() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Guidelines Channel", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Safety Protocol v1", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Pin message
        mockMvc.perform(post("/channels/" + chanId + "/messages/" + msgId + "/pin")
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned", is(true)));

        // Unpin message
        mockMvc.perform(delete("/channels/" + chanId + "/messages/" + msgId + "/pin")
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned", is(false)));
    }

    @Test
    @DisplayName("20. Archived channel rejects new messages (400 Bad Request)")
    void testArchivedChannelRejectsMessages() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Old Shift Log", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Archive channel
        mockMvc.perform(put("/channels/" + chanId)
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateChannelRequest(null, null, null, ChannelStatus.ARCHIVED))))
                .andExpect(status().isOk());

        // Attempt to post message -> 400 Bad Request
        mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Late text", null, null, null))))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 4. MEMBERSHIP GOVERNANCE & AUDIT (21 - 25)
    // ==========================================

    @Test
    @DisplayName("21. Eligible employee can self-join public department channel")
    void testSelfJoinPublicDepartmentChannel() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Ops Social", null, null, ChannelType.DEPARTMENT, deptOps.getId(), null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenOpsManager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Ops employee self-joins
        mockMvc.perform(post("/channels/" + chanId + "/join")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(userBlrEmp.getId().toString())));

        // Duplicate join -> 409 Conflict
        mockMvc.perform(post("/channels/" + chanId + "/join")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("22. Channel Admin can add and remove members")
    void testAdminAddAndRemoveMember() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Dispatch Coordination", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Add Meena
        mockMvc.perform(post("/channels/" + chanId + "/members")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddChannelMemberRequest(userMumEmp.getId(), ChannelMemberRole.MODERATOR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("MODERATOR")));

        // Remove Meena
        mockMvc.perform(delete("/channels/" + chanId + "/members/" + userMumEmp.getId())
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("23. Member can leave channel")
    void testMemberCanLeaveChannel() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Voluntary Activity", null, null, ChannelType.COMPANY, null, null, false);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Join
        mockMvc.perform(post("/channels/" + chanId + "/join")
                .header("Authorization", "Bearer " + tokenBlrEmp));

        // Leave
        mockMvc.perform(delete("/channels/" + chanId + "/members/me")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("24. Read-only channel prohibits regular members from posting messages")
    void testReadOnlyChannelRestrictions() throws Exception {
        CreateChannelRequest chanReq = new CreateChannelRequest("Official Broadcast Only", null, null, ChannelType.COMPANY, null, null, true);
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chanReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Normal employee tries to post -> 403 Forbidden
        mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Unauthorized reply", null, null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message", containsString("read-only")));

        // Super Admin can post
        mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Official Broadcast Notice", null, null, null))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("25. Security audit trail records channel lifecycle and moderation without leaking private message bodies")
    void testChannelAuditLogging() throws Exception {
        // 1. Create channel -> CHANNEL_CREATED
        MvcResult chanRes = mockMvc.perform(post("/channels")
                        .header("Authorization", "Bearer " + tokenSuperAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChannelRequest("Audited Channel", null, null, ChannelType.COMPANY, null, null, false))))
                .andExpect(status().isCreated())
                .andReturn();

        String chanId = objectMapper.readTree(chanRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 2. Add member -> CHANNEL_MEMBER_ADDED
        mockMvc.perform(post("/channels/" + chanId + "/members")
                .header("Authorization", "Bearer " + tokenSuperAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddChannelMemberRequest(userBlrEmp.getId(), ChannelMemberRole.MEMBER))));

        // 3. Post message
        MvcResult msgRes = mockMvc.perform(post("/channels/" + chanId + "/messages")
                        .header("Authorization", "Bearer " + tokenBlrEmp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Confidential operations note", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 4. Pin message -> CHANNEL_MESSAGE_PINNED
        mockMvc.perform(post("/channels/" + chanId + "/messages/" + msgId + "/pin")
                .header("Authorization", "Bearer " + tokenSuperAdmin));

        // 5. Unpin message -> CHANNEL_MESSAGE_UNPINNED
        mockMvc.perform(delete("/channels/" + chanId + "/messages/" + msgId + "/pin")
                .header("Authorization", "Bearer " + tokenSuperAdmin));

        // 6. Delete message -> MESSAGE_DELETED
        mockMvc.perform(delete("/messages/" + msgId)
                .header("Authorization", "Bearer " + tokenSuperAdmin));

        // 7. Update channel -> CHANNEL_UPDATED
        mockMvc.perform(put("/channels/" + chanId)
                .header("Authorization", "Bearer " + tokenSuperAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateChannelRequest("Audited Channel Renamed", null, null, null))));

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        Set<String> actions = auditLogs.stream().map(AuditLog::getAction).collect(Collectors.toSet());

        assertThat(actions).contains(
                "CHANNEL_CREATED",
                "CHANNEL_MEMBER_ADDED",
                "CHANNEL_MESSAGE_PINNED",
                "CHANNEL_MESSAGE_UNPINNED",
                "MESSAGE_DELETED",
                "CHANNEL_UPDATED"
        );

        // Verify zero private message bodies leaked in audit logs metadata
        for (AuditLog log : auditLogs) {
            if (log.getMetadata() != null) {
                assertThat(log.getMetadata().toString()).doesNotContain("Confidential operations note");
            }
        }
    }
}
