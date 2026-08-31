package com.logiconnect.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.audit.entity.AuditLog;
import com.logiconnect.platform.audit.repository.AuditLogRepository;
import com.logiconnect.platform.auth.repository.UserSessionRepository;
import com.logiconnect.platform.conversation.dto.AddConversationMemberRequest;
import com.logiconnect.platform.conversation.dto.CreateDirectConversationRequest;
import com.logiconnect.platform.conversation.dto.CreateGroupConversationRequest;
import com.logiconnect.platform.conversation.entity.Conversation;
import com.logiconnect.platform.conversation.entity.ConversationMemberRole;
import com.logiconnect.platform.conversation.entity.ConversationType;
import com.logiconnect.platform.conversation.repository.ConversationMemberRepository;
import com.logiconnect.platform.conversation.repository.ConversationRepository;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
import com.logiconnect.platform.message.dto.AttachmentRequest;
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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessagingIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private User userC;
    private User userD;

    private Employee empA;
    private Employee empB;
    private Employee empC;
    private Employee empD;

    private String tokenA;
    private String tokenB;
    private String tokenC;
    private String tokenD;

    private Department deptOps;

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

        // 1. Roles & Permissions
        Permission sendMsg = permissionRepository.save(new Permission(null, AppPermission.SEND_MESSAGES, "Send Messages", "MESSAGING"));
        Permission viewEmp = permissionRepository.save(new Permission(null, AppPermission.VIEW_EMPLOYEES, "View Employees", "EMPLOYEE"));

        Role employeeRole = new Role(null, "EMPLOYEE", "Standard Employee", true);
        employeeRole.getPermissions().addAll(Set.of(sendMsg, viewEmp));
        employeeRole = roleRepository.save(employeeRole);

        // 2. Department
        deptOps = new Department(null, "OPS", "Operations");
        deptOps = departmentRepository.save(deptOps);

        // 3. Employees & Users
        empA = createEmployee("EMP101", "Alice", "Smith", "alice@logiconnect.internal", "Dispatcher");
        empB = createEmployee("EMP102", "Bob", "Jones", "bob@logiconnect.internal", "Fleet Manager");
        empC = createEmployee("EMP103", "Charlie", "Brown", "charlie@logiconnect.internal", "Support Agent");
        empD = createEmployee("EMP104", "Diana", "Prince", "diana@logiconnect.internal", "Logistics Lead");

        userA = createUser(empA, employeeRole);
        userB = createUser(empB, employeeRole);
        userC = createUser(empC, employeeRole);
        userD = createUser(empD, employeeRole);

        tokenA = generateToken(userA, empA);
        tokenB = generateToken(userB, empB);
        tokenC = generateToken(userC, empC);
        tokenD = generateToken(userD, empD);
    }

    private Employee createEmployee(String code, String first, String last, String email, String designation) {
        Employee e = new Employee();
        e.setEmployeeCode(code);
        e.setFirstName(first);
        e.setLastName(last);
        e.setEmail(email);
        e.setDesignation(designation);
        e.setDepartment(deptOps);
        e.setLocation("Bangalore Hub");
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

    private String generateToken(User user, Employee emp) {
        UserPrincipal principal = UserPrincipal.create(
                user.getId(), emp.getEmployeeCode(), user.getEmail(),
                emp.getFirstName(), emp.getLastName(), "Password@1234",
                true, Set.of("ROLE_EMPLOYEE"), Set.of(AppPermission.SEND_MESSAGES, AppPermission.VIEW_EMPLOYEES)
        );
        return jwtTokenProvider.generateAccessToken(principal);
    }

    // ==========================================
    // 1. CONVERSATION TESTS (1 - 10)
    // ==========================================

    @Test
    @DisplayName("1. Create direct conversation returns new conversation with 2 members")
    void testCreateDirectConversation() throws Exception {
        CreateDirectConversationRequest req = new CreateDirectConversationRequest(userB.getId());

        mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.type", is("DIRECT")))
                .andExpect(jsonPath("$.data.memberCount", is(2)))
                .andExpect(jsonPath("$.data.members", hasSize(2)));
    }

    @Test
    @DisplayName("2. Re-requesting same direct conversation returns existing conversation without duplicates")
    void testDirectConversationIdempotency() throws Exception {
        CreateDirectConversationRequest req = new CreateDirectConversationRequest(userB.getId());

        // First creation
        MvcResult res1 = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String convId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("data").get("id").asText();

        // Second creation from User B to User A -> returns existing conversation
        CreateDirectConversationRequest reverseReq = new CreateDirectConversationRequest(userA.getId());
        MvcResult res2 = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reverseReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String convId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("data").get("id").asText();

        assertThat(convId1).isEqualTo(convId2);
        assertThat(conversationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("3. Concurrent direct conversation creation does not create duplicate conversations")
    void testConcurrentDirectConversationCreation() throws Exception {
        int threads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final boolean fromA = (i % 2 == 0);
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    CreateDirectConversationRequest req = new CreateDirectConversationRequest(fromA ? userB.getId() : userA.getId());
                    MvcResult result = mockMvc.perform(post("/conversations/direct")
                                    .header("Authorization", "Bearer " + (fromA ? tokenA : tokenB))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    return 500;
                }
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        for (Future<Integer> f : futures) {
            assertThat(f.get()).isIn(200, 201);
        }

        List<Conversation> directConvs = conversationRepository.findDirectConversationsBetween(userA.getId(), userB.getId());
        assertThat(directConvs).hasSize(1);
    }

    @Test
    @DisplayName("4. Create group conversation creates room with creator as ADMIN")
    void testCreateGroupConversation() throws Exception {
        CreateGroupConversationRequest req = new CreateGroupConversationRequest(
                "Fleet Operations War Room",
                "Daily dispatch coordination",
                null,
                List.of(userB.getId(), userC.getId())
        );

        mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.type", is("GROUP")))
                .andExpect(jsonPath("$.data.name", is("Fleet Operations War Room")))
                .andExpect(jsonPath("$.data.memberCount", is(3)))
                .andExpect(jsonPath("$.data.members[?(@.userId == '" + userA.getId() + "')].role", hasItem("ADMIN")));
    }

    @Test
    @DisplayName("5. Duplicate group members in request are deduplicated")
    void testDuplicateGroupMembersDeduplicated() throws Exception {
        CreateGroupConversationRequest req = new CreateGroupConversationRequest(
                "Support Sync",
                null,
                null,
                List.of(userB.getId(), userB.getId(), userA.getId(), userB.getId())
        );

        mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.memberCount", is(2)));
    }

    @Test
    @DisplayName("6. Invalid target user in conversation creation is rejected (404 Not Found)")
    void testInvalidUserRejected() throws Exception {
        CreateDirectConversationRequest req = new CreateDirectConversationRequest(UUID.randomUUID());

        mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7. List conversations returns only rooms where caller is an active member")
    void testListUserConversationsOnly() throws Exception {
        // Conv 1: A and B
        CreateDirectConversationRequest directAB = new CreateDirectConversationRequest(userB.getId());
        mockMvc.perform(post("/conversations/direct")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(directAB)));

        // Conv 2: C and D
        CreateDirectConversationRequest directCD = new CreateDirectConversationRequest(userD.getId());
        mockMvc.perform(post("/conversations/direct")
                .header("Authorization", "Bearer " + tokenC)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(directCD)));

        // User A lists conversations -> only Conv 1
        mockMvc.perform(get("/conversations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].type", is("DIRECT")));

        // User C lists conversations -> only Conv 2
        mockMvc.perform(get("/conversations")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    @Test
    @DisplayName("8. Conversation list pagination works and respects bounds")
    void testConversationPagination() throws Exception {
        // Create 2 conversations for User A
        mockMvc.perform(post("/conversations/direct")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))));

        mockMvc.perform(post("/conversations/direct")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userC.getId()))));

        mockMvc.perform(get("/conversations?page=0&size=1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements", is(2)))
                .andExpect(jsonPath("$.data.totalPages", is(2)));
    }

    @Test
    @DisplayName("9. Unauthorized conversation access returns 403 Forbidden")
    void testUnauthorizedConversationAccess() throws Exception {
        // Conversation between A and B
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // User C attempts to read -> 403 Forbidden
        mockMvc.perform(get("/conversations/" + convId)
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Non-member cannot inspect conversation membership (403 Forbidden)")
    void testNonMemberCannotReadMembers() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // User D attempts to list members -> 403 Forbidden
        mockMvc.perform(get("/conversations/" + convId + "/members")
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 2. MEMBERSHIP GOVERNANCE TESTS (11 - 15)
    // ==========================================

    @Test
    @DisplayName("11. Group Admin can add new members to group")
    void testGroupAdminCanAddMember() throws Exception {
        // A creates group with B
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Ops Team", null, null, List.of(userB.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // A (Admin) adds C
        AddConversationMemberRequest addReq = new AddConversationMemberRequest(userC.getId(), ConversationMemberRole.MEMBER);
        mockMvc.perform(post("/conversations/" + convId + "/members")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId", is(userC.getId().toString())));
    }

    @Test
    @DisplayName("12. Group Admin can remove members from group")
    void testGroupAdminCanRemoveMember() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Ops Team", null, null, List.of(userB.getId(), userC.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // A removes C
        mockMvc.perform(delete("/conversations/" + convId + "/members/" + userC.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify C can no longer access conversation
        mockMvc.perform(get("/conversations/" + convId)
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. Non-admin member cannot add or remove group members (403 Forbidden)")
    void testNonAdminCannotModifyMembership() throws Exception {
        // A creates group with B
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Ops Team", null, null, List.of(userB.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // B (regular member) tries to add C -> 403 Forbidden
        AddConversationMemberRequest addReq = new AddConversationMemberRequest(userC.getId(), ConversationMemberRole.MEMBER);
        mockMvc.perform(post("/conversations/" + convId + "/members")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("14. User can leave a group conversation")
    void testUserCanLeaveGroup() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Ops Team", null, null, List.of(userB.getId(), userC.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // B leaves group
        mockMvc.perform(delete("/conversations/" + convId + "/members/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Verify B cannot access conversation anymore
        mockMvc.perform(get("/conversations/" + convId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("15. Last-admin protection prevents lone administrator from leaving while other members remain")
    void testLastAdminProtection() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Ops Team", null, null, List.of(userB.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // A is the only ADMIN -> tries to leave -> 400 Bad Request
        mockMvc.perform(delete("/conversations/" + convId + "/members/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message", containsString("only group administrator")));
    }

    // ==========================================
    // 3. MESSAGE OPERATIONS & SECURITY (16 - 30)
    // ==========================================

    @Test
    @DisplayName("16. Member can send message to conversation")
    void testMemberCanSendMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        SendMessageRequest msgReq = new SendMessageRequest("Hello Bob, dispatch is ready.", null, null, null);
        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", is("Hello Bob, dispatch is ready.")))
                .andExpect(jsonPath("$.data.sender.employeeCode", is("EMP101")));
    }

    @Test
    @DisplayName("17. Non-member cannot send message to conversation (403 Forbidden)")
    void testNonMemberCannotSendMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        SendMessageRequest msgReq = new SendMessageRequest("Intruder message", null, null, null);
        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("18. Sender identity is strictly derived from authenticated context")
    void testSenderDerivedFromAuthContext() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        SendMessageRequest msgReq = new SendMessageRequest("Valid payload", null, null, null);
        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sender.id", is(userA.getId().toString())));
    }

    @Test
    @DisplayName("19. Client-provided fake sender ID cannot impersonate another employee")
    void testClientCannotImpersonateSender() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Pass arbitrary extra payload with fake senderId -> server must attribute to tokenA
        String rawJson = "{\"content\":\"Spoofed message\",\"senderId\":\"" + userD.getId() + "\"}";

        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sender.id", is(userA.getId().toString())))
                .andExpect(jsonPath("$.data.sender.employeeCode", is("EMP101")));
    }

    @Test
    @DisplayName("20. Empty message without attachments is rejected (400 Bad Request)")
    void testEmptyMessageRejected() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        SendMessageRequest emptyReq = new SendMessageRequest("   ", null, null, null);
        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("21. Message history pagination works properly")
    void testMessageHistoryPagination() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/conversations/" + convId + "/messages")
                    .header("Authorization", "Bearer " + tokenA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SendMessageRequest("Message #" + i, null, null, null))));
        }

        mockMvc.perform(get("/conversations/" + convId + "/messages?page=0&size=2")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(3)));
    }

    @Test
    @DisplayName("22. Message ordering is deterministic and stable")
    void testMessageOrderingStable() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(post("/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessageRequest("First", null, null, null))));

        mockMvc.perform(post("/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessageRequest("Second", null, null, null))));

        mockMvc.perform(get("/conversations/" + convId + "/messages?direction=asc")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content", is("First")))
                .andExpect(jsonPath("$.data.content[1].content", is("Second")));
    }

    @Test
    @DisplayName("23. Member can retrieve message by ID")
    void testMemberCanGetMessageById() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Specific msg", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(get("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", is("Specific msg")));
    }

    @Test
    @DisplayName("24. Non-member cannot retrieve message by ID (403 Forbidden, no IDOR)")
    void testNonMemberCannotGetMessageById() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Confidential msg", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // User C (not in conversation) tries to access message by ID -> 403 Forbidden
        mockMvc.perform(get("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("25. Sender can edit own message")
    void testSenderCanEditOwnMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Original text", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        EditMessageRequest editReq = new EditMessageRequest("Edited updated text");
        mockMvc.perform(put("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", is("Edited updated text")))
                .andExpect(jsonPath("$.data.editedAt", notNullValue()));
    }

    @Test
    @DisplayName("26. User cannot edit another employee's message (403 Forbidden)")
    void testUserCannotEditAnotherMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Alice original", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Bob tries to edit Alice's message -> 403 Forbidden
        EditMessageRequest editReq = new EditMessageRequest("Bob tampering text");
        mockMvc.perform(put("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("27. Sender can soft-delete own message")
    void testSenderCanDeleteOwnMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("To be deleted", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(delete("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDeleted", is(true)));

        // Record still exists in database with deletedAt timestamp
        Message dbMsg = messageRepository.findById(UUID.fromString(msgId)).orElseThrow();
        assertThat(dbMsg.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("28. User cannot delete another employee's message (403 Forbidden)")
    void testUserCannotDeleteAnotherMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Alice message", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Bob tries to delete Alice's message -> 403 Forbidden
        mockMvc.perform(delete("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("29. Soft-deleted message content is masked in responses")
    void testDeletedMessageContentMasked() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Top secret payload", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Delete message
        mockMvc.perform(delete("/messages/" + msgId)
                .header("Authorization", "Bearer " + tokenA));

        // Get single message -> content should be null
        mockMvc.perform(get("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDeleted", is(true)))
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    @Test
    @DisplayName("30. Message thread reply reference foundation correctly links parent message")
    void testMessageReplyThreadReference() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Parent message
        MvcResult msg1Res = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Initial Question?", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String parentMsgId = objectMapper.readTree(msg1Res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Reply message
        SendMessageRequest replyReq = new SendMessageRequest("Here is the answer.", null, UUID.fromString(parentMsgId), null);
        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.replyToMessageId", is(parentMsgId)))
                .andExpect(jsonPath("$.data.replyToMessage.content", is("Initial Question?")));
    }

    // ==========================================
    // 4. SECURITY & BOUNDARY TESTS (31 - 35)
    // ==========================================

    @Test
    @DisplayName("31. IDOR Defense: Direct conversation cannot be accessed by external user")
    void testIdorOnConversation() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(get("/conversations/" + convId)
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("32. IDOR Defense: Message cannot be accessed across conversations")
    void testIdorOnMessage() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Secret text", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(get("/messages/" + msgId)
                        .header("Authorization", "Bearer " + tokenD))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("33. Unauthenticated requests to messaging endpoints return 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/messages/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("34. Unauthorized member operations return 403 Forbidden")
    void testUnauthorizedMemberOperation() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Group", null, null, List.of(userB.getId(), userC.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Regular member B attempts to remove member C -> 403 Forbidden
        mockMvc.perform(delete("/conversations/" + convId + "/members/" + userC.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("35. Zero sensitive security fields exposed in conversation/member responses")
    void testZeroSensitiveFieldsExposed() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(get("/conversations/" + convId + "/members")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].failedLoginAttempts").doesNotExist())
                .andExpect(jsonPath("$.data[0].refreshTokenHash").doesNotExist());
    }

    // ==========================================
    // 5. DATA INTEGRITY & AUDIT (36 - 40)
    // ==========================================

    @Test
    @DisplayName("36. Direct conversation uniqueness in database")
    void testDirectConversationDatabaseUniqueness() throws Exception {
        CreateDirectConversationRequest req = new CreateDirectConversationRequest(userB.getId());

        mockMvc.perform(post("/conversations/direct")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(post("/conversations/direct")
                .header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userA.getId()))));

        List<Conversation> directList = conversationRepository.findDirectConversationsBetween(userA.getId(), userB.getId());
        assertThat(directList).hasSize(1);
    }

    @Test
    @DisplayName("37. Conversation membership composite key uniqueness prevents duplicate membership")
    void testMembershipUniqueness() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Group", null, null, List.of(userB.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Adding already active member B -> 409 Conflict
        AddConversationMemberRequest addReq = new AddConversationMemberRequest(userB.getId(), ConversationMemberRole.MEMBER);
        mockMvc.perform(post("/conversations/" + convId + "/members")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("CONFLICT")));
    }

    @Test
    @DisplayName("38. Message attachment metadata correctly persisted with cascade")
    void testMessageAttachmentMetadata() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        AttachmentRequest attReq = new AttachmentRequest(
                "s3://logiconnect/attachments/manifest_123.pdf",
                "https://storage.logiconnect.internal/manifest_123.pdf",
                "manifest_123.pdf",
                "application/pdf",
                1048576L
        );

        SendMessageRequest msgReq = new SendMessageRequest(
                "Attached shipping manifest",
                null,
                null,
                List.of(attReq)
        );

        mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.attachments", hasSize(1)))
                .andExpect(jsonPath("$.data.attachments[0].fileName", is("manifest_123.pdf")))
                .andExpect(jsonPath("$.data.attachments[0].fileSize", is(1048576)));

        assertThat(attachmentRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("39. Message soft deletion preserves database audit record and history")
    void testSoftDeletionIntegrity() throws Exception {
        MvcResult res = mockMvc.perform(post("/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest(userB.getId()))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Compliance record message", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        mockMvc.perform(delete("/messages/" + msgId)
                .header("Authorization", "Bearer " + tokenA));

        // Ensure database row was NOT physically removed
        Optional<Message> dbMsgOpt = messageRepository.findById(UUID.fromString(msgId));
        assertThat(dbMsgOpt).isPresent();
        assertThat(dbMsgOpt.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("40. Security audit trail records messaging lifecycle events without leaking raw message bodies")
    void testMessagingAuditEvents() throws Exception {
        // 1. Create conversation -> CONVERSATION_CREATED
        MvcResult res = mockMvc.perform(post("/conversations/group")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupConversationRequest("Audited Group", null, null, List.of(userB.getId())))))
                .andExpect(status().isCreated())
                .andReturn();

        String convId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // 2. Add member -> GROUP_MEMBER_ADDED
        mockMvc.perform(post("/conversations/" + convId + "/members")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddConversationMemberRequest(userC.getId(), ConversationMemberRole.MEMBER))));

        // 3. Send message
        MvcResult msgRes = mockMvc.perform(post("/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Original sensitive text", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 4. Edit message -> MESSAGE_EDITED
        mockMvc.perform(put("/messages/" + msgId)
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EditMessageRequest("Modified sensitive text"))));

        // 5. Delete message -> MESSAGE_DELETED
        mockMvc.perform(delete("/messages/" + msgId)
                .header("Authorization", "Bearer " + tokenA));

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        Set<String> actions = auditLogs.stream().map(AuditLog::getAction).collect(java.util.stream.Collectors.toSet());

        assertThat(actions).contains("CONVERSATION_CREATED", "GROUP_MEMBER_ADDED", "MESSAGE_EDITED", "MESSAGE_DELETED");

        // Verify zero private message bodies leaked in audit logs metadata
        for (AuditLog log : auditLogs) {
            if (log.getMetadata() != null) {
                assertThat(log.getMetadata().toString()).doesNotContain("Original sensitive text");
                assertThat(log.getMetadata().toString()).doesNotContain("Modified sensitive text");
            }
        }
    }
}
