package com.logiconnect.platform.team.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ConflictException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
import com.logiconnect.platform.team.dto.CreateTeamRequest;
import com.logiconnect.platform.team.dto.TeamResponse;
import com.logiconnect.platform.team.dto.UpdateTeamRequest;
import com.logiconnect.platform.team.entity.Team;
import com.logiconnect.platform.team.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "status", "createdAt");

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public TeamService(TeamRepository teamRepository,
                       DepartmentRepository departmentRepository,
                       EmployeeRepository employeeRepository,
                       AuditService auditService) {
        this.teamRepository = teamRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> listTeams(UUID departmentId, String status, String search, int page, int size, String sortField, String sortDirection) {
        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        String resolvedSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(direction, resolvedSort));
        Page<Team> teamPage = teamRepository.findAllFiltered(departmentId, status, search, pageable);

        Page<TeamResponse> responsePage = teamPage.map(this::toTeamResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
        return toTeamResponse(team);
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, UUID actorId) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));

        if (!"ACTIVE".equals(department.getStatus())) {
            throw new BadRequestException("Cannot create team in an inactive or archived department");
        }

        if (teamRepository.existsByCode(request.code())) {
            throw new ConflictException("Team with code '" + request.code() + "' already exists");
        }
        if (teamRepository.existsByDepartmentIdAndName(request.departmentId(), request.name())) {
            throw new ConflictException("Team with name '" + request.name() + "' already exists in this department");
        }

        // Validate team lead if specified
        if (request.teamLeadId() != null) {
            employeeRepository.findById(request.teamLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (team lead)", "id", request.teamLeadId()));
        }

        Team team = new Team();
        team.setDepartment(department);
        team.setCode(request.code());
        team.setName(request.name());
        team.setDescription(request.description());
        team.setTeamLeadId(request.teamLeadId());
        team.setStatus("ACTIVE");

        team = teamRepository.save(team);

        log.info("Team created: code={}, name={}, department={}, by={}", team.getCode(), team.getName(), department.getCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.TEAM_CREATED, "TEAM", team.getId(), null, null,
                Map.of("code", team.getCode(), "name", team.getName(), "departmentCode", department.getCode(), "createdBy", String.valueOf(actorId)));

        return toTeamResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request, UUID actorId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        if (request.name() != null && !request.name().isBlank()) {
            if (!request.name().equals(team.getName()) && teamRepository.existsByDepartmentIdAndName(team.getDepartment().getId(), request.name())) {
                throw new ConflictException("Team with name '" + request.name() + "' already exists in this department");
            }
            team.setName(request.name());
        }
        if (request.description() != null) {
            team.setDescription(request.description());
        }
        if (request.teamLeadId() != null) {
            employeeRepository.findById(request.teamLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (team lead)", "id", request.teamLeadId()));
            team.setTeamLeadId(request.teamLeadId());
        }
        if (request.status() != null && !request.status().isBlank()) {
            validateTeamStatus(request.status());
            team.setStatus(request.status());
        }

        team = teamRepository.save(team);

        log.info("Team updated: id={}, code={}, by={}", team.getId(), team.getCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.TEAM_UPDATED, "TEAM", team.getId(), null, null,
                Map.of("code", team.getCode(), "updatedBy", String.valueOf(actorId)));

        return toTeamResponse(team);
    }

    @Transactional
    public void deactivateTeam(UUID id, UUID actorId) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        if ("INACTIVE".equals(team.getStatus()) || "ARCHIVED".equals(team.getStatus())) {
            throw new BadRequestException("Team is already inactive or archived");
        }

        team.setStatus("INACTIVE");
        teamRepository.save(team);

        log.info("Team deactivated: id={}, code={}, by={}", team.getId(), team.getCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.TEAM_DEACTIVATED, "TEAM", team.getId(), null, null,
                Map.of("code", team.getCode(), "deactivatedBy", String.valueOf(actorId)));
    }

    private TeamResponse toTeamResponse(Team team) {
        String teamLeadName = null;
        if (team.getTeamLeadId() != null) {
            teamLeadName = employeeRepository.findById(team.getTeamLeadId())
                    .map(Employee::getFullName)
                    .orElse(null);
        }

        long memberCount = teamRepository.countActiveEmployeesByTeamId(team.getId());

        Department dept = team.getDepartment();
        return new TeamResponse(
                team.getId(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : null,
                team.getCode(),
                team.getName(),
                team.getDescription(),
                team.getStatus(),
                team.getTeamLeadId(),
                teamLeadName,
                memberCount,
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }

    private void validateTeamStatus(String status) {
        if (!Set.of("ACTIVE", "INACTIVE", "ARCHIVED").contains(status)) {
            throw new BadRequestException("Invalid team status: " + status + ". Must be one of: ACTIVE, INACTIVE, ARCHIVED");
        }
    }
}
