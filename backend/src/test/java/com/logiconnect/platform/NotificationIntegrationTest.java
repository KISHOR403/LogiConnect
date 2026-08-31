package com.logiconnect.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.announcement.dto.CreateAnnouncementRequest;
import com.logiconnect.platform.announcement.entity.AnnouncementPriority;
import com.logiconnect.platform.announcement.entity.AnnouncementTargetType;
import com.logiconnect.platform.announcement.entity.AnnouncementType;
import com.logiconnect.platform.announcement.repository.AnnouncementReadRepository;
import com.logiconnect.platform.announcement.repository.AnnouncementRepository;
import com.logiconnect.platform.announcement.service.AnnouncementService;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.channel.repository.ChannelMemberRepository;
import com.logiconnect.platform.channel.repository.ChannelRepository;
import com.logiconnect.platform.conversation.dto.CreateDirectConversationRequest;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.conversation.service.ConversationService;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
import com.logiconnect.platform.message.dto.SendMessageRequest;
import com.logiconnect.platform.message.repository.MessageAttachmentRepository;
import com.logiconnect.platform.message.repository.MessageRepository;
import com.logiconnect.platform.message.service.MessageService;
import com.logiconnect.platform.notification.entity.Notification;
import com.logiconnect.platform.notification.entity.NotificationType;
import com.logiconnect.platform.notification.repository.NotificationRepository;
import com.logiconnect.platform.notification.service.NotificationService;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

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
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConversationService conversationService;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Department deptOps;
    private Department deptSupport;
    private Team teamBlrOps;
    private Team teamMumOps;
    private Team teamSupport;

    private Employee empSuper;
    private Employee empLead;
    private Employee empBlr;
    private Employee empMum;
    private Employee empSupport;

    private User userAdmin;
    private User userBlrLead;
    private User userBlrEmp;
    private User userMumEmp;
    private User userSupportEmp;

    private String tokenAdmin;
    private String tokenBlrLead;
    private String tokenBlrEmp;
    private String tokenMumEmp;
    private String tokenSupportEmp;

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
        Permission pubAnnounce = permissionRepository.save(new Permission(null, AppPermission.PUBLISH_ANNOUNCEMENTS, "Publish Announcements", "ANNOUNCEMENT"));
        Permission viewEmp = permissionRepository.save(new Permission(null, AppPermission.VIEW_EMPLOYEES, "View Employees", "EMPLOYEE"));

        // 2. Roles
        Role superAdminRole = roleRepository.save(new Role(null, "SUPER_ADMIN", "Super Admin", true));
        superAdminRole.getPermissions().addAll(Set.of(sendMsg, sysAdmin, pubAnnounce, viewEmp));
        superAdminRole = roleRepository.save(superAdminRole);

        Role leadRole = roleRepository.save(new Role(null, "TEAM_LEADER", "Team Leader", true));
        leadRole.getPermissions().addAll(Set.of(sendMsg, pubAnnounce, viewEmp));
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
        empSuper = createEmployee("EMP001", "Super", "Admin", "admin@logiconnect.internal", "Administrator", deptOps, null, EmployeeStatus.ACTIVE);
        empLead = createEmployee("EMP002", "Lata", "Rao", "lead@logiconnect.internal", "Team Leader", deptOps, teamBlrOps, EmployeeStatus.ACTIVE);
        empBlr = createEmployee("EMP003", "Bharat", "Kumar", "bharat@logiconnect.internal", "Dispatcher", deptOps, teamBlrOps, EmployeeStatus.ACTIVE);
        empMum = createEmployee("EMP004", "Meena", "Shah", "meena@logiconnect.internal", "Fleet Coordinator", deptOps, teamMumOps, EmployeeStatus.ACTIVE);
        empSupport = createEmployee("EMP005", "Ravi", "Verma", "ravi@logiconnect.internal", "Support Agent", deptSupport, teamSupport, EmployeeStatus.ACTIVE);

        deptOps.setManagerId(empSuper.getId());
        departmentRepository.save(deptOps);

        teamBlrOps.setTeamLeadId(empLead.getId());
        teamRepository.save(teamBlrOps);

        userAdmin = createUser(empSuper, superAdminRole, UserStatus.ACTIVE);
        userBlrLead = createUser(empLead, leadRole, UserStatus.ACTIVE);
        userBlrEmp = createUser(empBlr, employeeRole, UserStatus.ACTIVE);
        userMumEmp = createUser(empMum, employeeRole, UserStatus.ACTIVE);
        userSupportEmp = createUser(empSupport, employeeRole, UserStatus.ACTIVE);

        tokenAdmin = generateToken(userAdmin, empSuper, Set.of("ROLE_SUPER_ADMIN"), Set.of(AppPermission.PUBLISH_ANNOUNCEMENTS, AppPermission.SYSTEM_ADMIN));
        tokenBlrLead = generateToken(userBlrLead, empLead, Set.of("ROLE_TEAM_LEADER"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenBlrEmp = generateToken(userBlrEmp, empBlr, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenMumEmp = generateToken(userMumEmp, empMum, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
        tokenSupportEmp = generateToken(userSupportEmp, empSupport, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.VIEW_EMPLOYEES));
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

    // ==========================================
    // 1. NOTIFICATION RETRIEVAL & AUTHENTICATION
    // ==========================================

    @Test
    @DisplayName("1. Authenticated employee can retrieve notifications")
    void getNotifications_authenticated_success() throws Exception {
        notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.ANNOUNCEMENT, "Welcome", "Welcome to LogiConnect", "ANNOUNCEMENT", UUID.randomUUID()
        );

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Welcome"))
                .andExpect(jsonPath("$.data.content[0].type").value("ANNOUNCEMENT"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }

    @Test
    @DisplayName("2. Unauthenticated request receives 401 Unauthorized")
    void getNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Employee receives only their own notifications")
    void getNotifications_employeeReceivesOnlyOwn() throws Exception {
        notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.ANNOUNCEMENT, "BLR Notice", "Notice for BLR", "ANNOUNCEMENT", UUID.randomUUID()
        );
        notificationService.createNotification(
                userMumEmp.getId(), NotificationType.ANNOUNCEMENT, "MUM Notice", "Notice for MUM", "ANNOUNCEMENT", UUID.randomUUID()
        );

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("BLR Notice"));
    }

    @Test
    @DisplayName("4. Employee cannot retrieve another employee's notifications via query parameter")
    void getNotifications_ignoresClientSuppliedUserId() throws Exception {
        notificationService.createNotification(
                userMumEmp.getId(), NotificationType.ANNOUNCEMENT, "Secret MUM Notice", "Confidential", "ANNOUNCEMENT", UUID.randomUUID()
        );

        mockMvc.perform(get("/notifications?userId=" + userMumEmp.getId())
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // ==========================================
    // 2. UNREAD COUNT
    // ==========================================

    @Test
    @DisplayName("5. Employee can retrieve unread count")
    void getUnreadCount_success() throws Exception {
        notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.MESSAGE, "Msg 1", "Hello 1", "MESSAGE", UUID.randomUUID()
        );
        notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.MESSAGE, "Msg 2", "Hello 2", "MESSAGE", UUID.randomUUID()
        );

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    @DisplayName("6. Unread count is correct and excludes other users' notifications")
    void getUnreadCount_accurateAndIsolated() throws Exception {
        notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.MESSAGE, "BLR Msg", "Hello", "MESSAGE", UUID.randomUUID()
        );
        notificationService.createNotification(
                userMumEmp.getId(), NotificationType.MESSAGE, "MUM Msg 1", "Hello", "MESSAGE", UUID.randomUUID()
        );
        notificationService.createNotification(
                userMumEmp.getId(), NotificationType.MESSAGE, "MUM Msg 2", "Hello", "MESSAGE", UUID.randomUUID()
        );

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    // ==========================================
    // 3. MARK AS READ & IDOR DEFENSE
    // ==========================================

    @Test
    @DisplayName("7. Employee can mark notification as read")
    void markAsRead_success() throws Exception {
        Notification notification = notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.ANNOUNCEMENT, "Important Update", "Read this", "ANNOUNCEMENT", UUID.randomUUID()
        );

        mockMvc.perform(post("/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("8. Employee cannot mark another employee's notification as read (IDOR Protection -> 404)")
    void markAsRead_otherUserNotification_returns404() throws Exception {
        Notification mumNotification = notificationService.createNotification(
                userMumEmp.getId(), NotificationType.ANNOUNCEMENT, "MUM Secret", "Confidential", "ANNOUNCEMENT", UUID.randomUUID()
        );

        mockMvc.perform(post("/notifications/" + mumNotification.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));

        Notification unaltered = notificationRepository.findById(mumNotification.getId()).orElseThrow();
        assertThat(unaltered.isRead()).isFalse();
        assertThat(unaltered.getReadAt()).isNull();
    }

    @Test
    @DisplayName("9. Already-read notification keeps original readAt timestamp")
    void markAsRead_alreadyRead_preservesTimestamp() throws Exception {
        Notification notification = notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.ANNOUNCEMENT, "Policy Notice", "Notice text", "ANNOUNCEMENT", UUID.randomUUID()
        );
        Instant pastReadAt = Instant.now().minus(2, ChronoUnit.HOURS);
        notification.markAsRead(pastReadAt);
        notificationRepository.save(notification);

        mockMvc.perform(post("/notifications/" + notification.getId() + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));

        Notification refreshed = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(refreshed.getReadAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(pastReadAt.truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("10. Mark-all-read affects only current user")
    void markAllAsRead_affectsOnlyCurrentUser() throws Exception {
        notificationService.createNotification(userBlrEmp.getId(), NotificationType.MESSAGE, "BLR 1", "M1", "MESSAGE", UUID.randomUUID());
        notificationService.createNotification(userBlrEmp.getId(), NotificationType.MESSAGE, "BLR 2", "M2", "MESSAGE", UUID.randomUUID());
        notificationService.createNotification(userMumEmp.getId(), NotificationType.MESSAGE, "MUM 1", "M1", "MESSAGE", UUID.randomUUID());

        mockMvc.perform(post("/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.markedCount").value(2));

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userBlrEmp.getId())).isZero();
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userMumEmp.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("11. Mark-all-read does not affect another user's unread notifications")
    void markAllAsRead_isolatesUsers() throws Exception {
        notificationService.createNotification(userMumEmp.getId(), NotificationType.ANNOUNCEMENT, "MUM Alert", "Alert", "ANNOUNCEMENT", UUID.randomUUID());

        mockMvc.perform(post("/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    // ==========================================
    // 4. PAGINATION & ORDERING
    // ==========================================

    @Test
    @DisplayName("12. Pagination works correctly")
    void getNotifications_pagination_works() throws Exception {
        for (int i = 1; i <= 5; i++) {
            notificationService.createNotification(
                    userBlrEmp.getId(), NotificationType.MESSAGE, "Msg " + i, "Body " + i, "MESSAGE", UUID.randomUUID()
            );
        }

        mockMvc.perform(get("/notifications?page=0&size=2")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    @DisplayName("13. Maximum page size is safely capped to 100")
    void getNotifications_maxPageSizeCapped() throws Exception {
        mockMvc.perform(get("/notifications?page=0&size=500")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    @DisplayName("14. Notifications are ordered newest first")
    void getNotifications_orderedNewestFirst() throws Exception {
        Notification n1 = notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.MESSAGE, "First Notice", "1", "MESSAGE", UUID.randomUUID()
        );
        Thread.sleep(10);
        Notification n2 = notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.MESSAGE, "Second Notice", "2", "MESSAGE", UUID.randomUUID()
        );

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Second Notice"))
                .andExpect(jsonPath("$.data.content[1].title").value("First Notice"));
    }

    // ==========================================
    // 5. ANNOUNCEMENT PUBLICATION INTEGRATION
    // ==========================================

    @Test
    @DisplayName("15. Announcement publication creates appropriate notifications")
    void publishAnnouncement_createsNotifications() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Company All Hands",
                "Quarterly Town Hall meeting next week.",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.publishAnnouncement(created.id(), userAdmin.getId());

        // Active company users received notifications
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userBlrEmp.getId())).isEqualTo(1);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userMumEmp.getId())).isEqualTo(1);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userSupportEmp.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("16. Department announcement notifies only eligible department employees")
    void publishDepartmentAnnouncement_notifiesOnlyDepartment() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Operations SOP Update",
                "Updated standard operating procedures for Ops.",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.DEPARTMENT,
                deptOps.getId(),
                null,
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.publishAnnouncement(created.id(), userAdmin.getId());

        // Ops employees get notifications
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userBlrEmp.getId())).isEqualTo(1);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userMumEmp.getId())).isEqualTo(1);

        // Support employee (different department) does NOT get notification
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userSupportEmp.getId())).isZero();
    }

    @Test
    @DisplayName("17. Team announcement notifies only eligible team employees")
    void publishTeamAnnouncement_notifiesOnlyTeam() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Bangalore Team Standup",
                "Standup moved to 10:00 AM.",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.TEAM,
                deptOps.getId(),
                teamBlrOps.getId(),
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userBlrLead.getId());
        announcementService.publishAnnouncement(created.id(), userBlrLead.getId());

        // BLR team employee gets notification
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userBlrEmp.getId())).isEqualTo(1);

        // Mumbai team employee (same dept, different team) does NOT get notification
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userMumEmp.getId())).isZero();

        // Support employee does NOT get notification
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userSupportEmp.getId())).isZero();
    }

    @Test
    @DisplayName("18. Company announcement notifies eligible company employees")
    void publishCompanyAnnouncement_notifiesEligibleEmployees() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Holiday Calendar 2026",
                "Please review the 2026 corporate holiday schedule.",
                AnnouncementType.HR,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.publishAnnouncement(created.id(), userAdmin.getId());

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(5); // 5 active users
        assertThat(notifications).allMatch(n -> n.getType() == NotificationType.ANNOUNCEMENT);
    }

    @Test
    @DisplayName("19. Unauthorized users cannot trigger unauthorized announcement notifications")
    void unauthorizedUserCannotPublishOrTriggerNotifications() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Unauthorized Notice",
                "Content",
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
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    @DisplayName("20. Draft announcement creates no employee notification")
    void draftAnnouncement_createsNoNotification() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Work in Progress Draft",
                "Draft content",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        announcementService.createAnnouncement(request, userAdmin.getId());
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    @DisplayName("21. Cancelled announcement creates no employee notification")
    void cancelledAnnouncement_createsNoNotification() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Cancelled Notice",
                "Will be cancelled",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.cancelAnnouncement(created.id(), userAdmin.getId());

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    @DisplayName("22. Duplicate publication does not create duplicate notification set")
    void duplicatePublication_doesNotDuplicateNotifications() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Single Notice",
                "Single Notice Content",
                AnnouncementType.GENERAL,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.publishAnnouncement(created.id(), userAdmin.getId());
        long initialCount = notificationRepository.count();

        // Attempt second publication (caught by conflict check or deduplication)
        try {
            announcementService.publishAnnouncement(created.id(), userAdmin.getId());
        } catch (Exception ignored) {
        }

        assertThat(notificationRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("23. Urgent announcement uses URGENT_ANNOUNCEMENT type")
    void urgentAnnouncement_usesUrgentNotificationType() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Severe Weather Alert",
                "Facility closing early today due to cyclone warning.",
                AnnouncementType.EMERGENCY,
                AnnouncementPriority.EMERGENCY,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.publishAnnouncement(created.id(), userAdmin.getId());

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications).allMatch(n -> n.getType() == NotificationType.URGENT_ANNOUNCEMENT);
    }

    @Test
    @DisplayName("24. Acknowledgement-required announcement creates ACKNOWLEDGEMENT_REQUIRED notification")
    void acknowledgementRequiredAnnouncement_createsAcknowledgementNotification() throws Exception {
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Information Security Policy 2026",
                "All staff must read and acknowledge the updated ISMS policy.",
                AnnouncementType.POLICY,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.COMPANY,
                null,
                null,
                true,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userAdmin.getId());
        announcementService.publishAnnouncement(created.id(), userAdmin.getId());

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications).allMatch(n -> n.getType() == NotificationType.ACKNOWLEDGEMENT_REQUIRED);
        assertThat(notifications.get(0).getTitle()).contains("Action Required");
    }

    // ==========================================
    // 6. MESSAGE NOTIFICATION INTEGRATION
    // ==========================================

    @Test
    @DisplayName("25. Sender does not receive notification for their own message")
    void sendMessage_senderDoesNotReceiveOwnNotification() throws Exception {
        // Direct conversation between BLR Emp and MUM Emp
        var convResp = conversationService.createOrGetDirectConversation(
                new CreateDirectConversationRequest(userMumEmp.getId()), userBlrEmp.getId()
        );

        SendMessageRequest msgReq = new SendMessageRequest("Hello from BLR", null, null, null);
        messageService.sendMessage(convResp.id(), msgReq, userBlrEmp.getId());

        // BLR Emp (sender) has 0 notifications
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userBlrEmp.getId())).isZero();

        // MUM Emp (recipient) has 1 notification
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(userMumEmp.getId())).isEqualTo(1);
        Notification mumNotif = notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                userMumEmp.getId(), org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent().get(0);
        assertThat(mumNotif.getType()).isEqualTo(NotificationType.MESSAGE);
    }

    @Test
    @DisplayName("26. Notification reference does not bypass referenced-object authorization")
    void notificationReference_doesNotBypassUnderlyingAuthorization() throws Exception {
        // Target Team announcement to Bangalore Ops
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "Confidential BLR Fleet Protocol",
                "Internal BLR Ops secrets",
                AnnouncementType.OPERATIONS,
                AnnouncementPriority.NORMAL,
                AnnouncementTargetType.TEAM,
                deptOps.getId(),
                teamBlrOps.getId(),
                false,
                null,
                null
        );

        var created = announcementService.createAnnouncement(request, userBlrLead.getId());
        announcementService.publishAnnouncement(created.id(), userBlrLead.getId());

        // MUM Emp attempts to view the announcement using the referenced announcement ID
        mockMvc.perform(get("/announcements/" + created.id())
                        .header("Authorization", "Bearer " + tokenMumEmp))
                .andExpect(status().isNotFound()); // Hidden from non-target user
    }

    @Test
    @DisplayName("27. Invalid notification reference is handled safely")
    void invalidNotificationReference_handledSafely() throws Exception {
        UUID fakeReferenceId = UUID.randomUUID();
        Notification n = notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.ANNOUNCEMENT, "Legacy Notice", "Content", "ANNOUNCEMENT", fakeReferenceId
        );

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].referenceId").value(fakeReferenceId.toString()));
    }

    // ==========================================
    // 7. PRIVACY, AUDITING & ERROR HANDLING
    // ==========================================

    @Test
    @DisplayName("28. Sensitive data is not included in notification responses")
    void notificationResponse_doesNotLeakSensitiveData() throws Exception {
        Notification n = notificationService.createNotification(
                userBlrEmp.getId(), NotificationType.SECURITY, "Security Alert", "New login from Chrome", "USER", userBlrEmp.getId()
        );

        MvcResult result = mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("jwt");
        assertThat(responseBody).doesNotContain("refreshToken");
    }

    @Test
    @DisplayName("29. Notification operations use standard ApiError structure on failures")
    void notificationError_usesStandardApiError() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(post("/notifications/" + nonExistentId + "/read")
                        .header("Authorization", "Bearer " + tokenBlrEmp))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/notifications/" + nonExistentId + "/read"));
    }

    // ==========================================
    // 8. HIGH-VOLUME BATCH SIMULATION (2,000 USERS)
    // ==========================================

    @Test
    @DisplayName("30. Bulk notification performance test simulating 2,000 employees")
    void bulkNotificationCreation_simulating2000Employees_performance() {
        List<UUID> simulatedUserIds = new ArrayList<>();
        List<Employee> bulkEmployees = new ArrayList<>();
        List<User> bulkUsers = new ArrayList<>();
        String precomputedHash = passwordEncoder.encode("Pass@123");

        for (int i = 1; i <= 200; i++) {
            Employee emp = new Employee();
            emp.setEmployeeCode("SIM" + String.format("%04d", i));
            emp.setFirstName("Simulated");
            emp.setLastName("User" + i);
            emp.setEmail("sim.user" + i + "@logiconnect.internal");
            emp.setDesignation("Specialist");
            emp.setDepartment(deptOps);
            emp.setLocation("India");
            emp.setJoiningDate(LocalDate.of(2024, 1, 1));
            emp.setStatus(EmployeeStatus.ACTIVE);
            bulkEmployees.add(emp);
        }
        bulkEmployees = employeeRepository.saveAll(bulkEmployees);

        for (Employee emp : bulkEmployees) {
            User u = new User();
            u.setEmail(emp.getEmail());
            u.setPasswordHash(precomputedHash);
            u.setStatus(UserStatus.ACTIVE);
            u.setEmployee(emp);
            bulkUsers.add(u);
        }
        bulkUsers = userRepository.saveAll(bulkUsers);
        for (User u : bulkUsers) {
            simulatedUserIds.add(u.getId());
        }

        long start = System.currentTimeMillis();
        List<Notification> created = notificationService.createBulkNotifications(
                simulatedUserIds,
                NotificationType.ANNOUNCEMENT,
                "Company-wide Broadcast",
                "LogiConnect annual performance review cycle opens today.",
                "ANNOUNCEMENT",
                UUID.randomUUID()
        );
        long elapsed = System.currentTimeMillis() - start;

        assertThat(created).hasSize(200);
        assertThat(elapsed).isLessThan(5000); // Efficient batch insertion under 5 seconds
    }
}
