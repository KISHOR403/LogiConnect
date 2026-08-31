package com.logiconnect.platform.employee.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ConflictException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.dto.CreateEmployeeRequest;
import com.logiconnect.platform.employee.dto.EmployeeResponse;
import com.logiconnect.platform.employee.dto.UpdateEmployeeRequest;
import com.logiconnect.platform.employee.dto.UpdateEmployeeStatusRequest;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.entity.EmployeeStatus;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
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
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("firstName", "lastName", "employeeCode", "email", "designation", "location", "joiningDate", "status", "createdAt");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final AuditService auditService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           TeamRepository teamRepository,
                           AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> listEmployees(UUID departmentId, UUID teamId, String status, String location, String search,
                                                        int page, int size, String sortField, String sortDirection) {
        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        String resolvedSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "lastName";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        EmployeeStatus statusEnum = parseEmployeeStatus(status);

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(direction, resolvedSort));
        Page<Employee> employeePage = employeeRepository.findAllFiltered(departmentId, teamId, statusEnum, location, search, pageable);

        Page<EmployeeResponse> responsePage = employeePage.map(this::toEmployeeResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> listEmployeesByDepartment(UUID departmentId, String status, int page, int size) {
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));

        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("lastName"));

        EmployeeStatus statusEnum = parseEmployeeStatus(status);

        Page<Employee> employeePage = employeeRepository.findByDepartmentId(departmentId, statusEnum, pageable);
        Page<EmployeeResponse> responsePage = employeePage.map(this::toEmployeeResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> listEmployeesByTeam(UUID teamId, String status, int page, int size) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("lastName"));

        EmployeeStatus statusEnum = parseEmployeeStatus(status);

        Page<Employee> employeePage = employeeRepository.findByTeamId(teamId, statusEnum, pageable);
        Page<EmployeeResponse> responsePage = employeePage.map(this::toEmployeeResponse);
        return PageResponse.from(responsePage);
    }

    private EmployeeStatus parseEmployeeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EmployeeStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid employee status: " + status + ". Allowed values: ACTIVE, PROBATION, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED");
        }
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(UUID id) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return toEmployeeResponse(employee);
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request, UUID actorId) {
        // Uniqueness validation
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new ConflictException("Employee with code '" + request.employeeCode() + "' already exists");
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new ConflictException("Employee with email '" + request.email() + "' already exists");
        }

        // Department validation
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));

        // Team validation (must belong to the same department)
        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.teamId()));
            if (!team.getDepartment().getId().equals(department.getId())) {
                throw new BadRequestException("Team '" + team.getName() + "' does not belong to department '" + department.getName() + "'");
            }
        }

        // Manager validation
        Employee manager = null;
        if (request.managerId() != null) {
            manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (manager)", "id", request.managerId()));
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(request.employeeCode());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setDesignation(request.designation());
        employee.setDepartment(department);
        employee.setTeam(team);
        employee.setManager(manager);
        employee.setLocation(request.location());
        employee.setJoiningDate(request.joiningDate());
        employee.setStatus(EmployeeStatus.ACTIVE);

        employee = employeeRepository.save(employee);

        log.info("Employee created: code={}, name={} {}, department={}, by={}", employee.getEmployeeCode(), employee.getFirstName(), employee.getLastName(), department.getCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", employee.getId(), null, null,
                Map.of("employeeCode", employee.getEmployeeCode(), "email", employee.getEmail(), "department", department.getCode(), "createdBy", String.valueOf(actorId)));

        return toEmployeeResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest request, UUID actorId) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            employee.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            employee.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().isBlank()) {
            if (!request.email().equals(employee.getEmail()) && employeeRepository.existsByEmail(request.email())) {
                throw new ConflictException("Employee with email '" + request.email() + "' already exists");
            }
            employee.setEmail(request.email());
        }
        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }
        if (request.profilePhotoUrl() != null) {
            employee.setProfilePhotoUrl(request.profilePhotoUrl());
        }
        if (request.designation() != null && !request.designation().isBlank()) {
            employee.setDesignation(request.designation());
        }
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));
            employee.setDepartment(department);
        }
        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.teamId()));
            // Ensure team belongs to employee's department
            UUID deptId = employee.getDepartment().getId();
            if (!team.getDepartment().getId().equals(deptId)) {
                throw new BadRequestException("Team '" + team.getName() + "' does not belong to employee's department");
            }
            employee.setTeam(team);
        }
        if (request.managerId() != null) {
            Employee manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (manager)", "id", request.managerId()));
            if (manager.getId().equals(employee.getId())) {
                throw new BadRequestException("Employee cannot be their own manager");
            }
            employee.setManager(manager);
        }
        if (request.location() != null && !request.location().isBlank()) {
            employee.setLocation(request.location());
        }

        employee = employeeRepository.save(employee);

        log.info("Employee updated: id={}, code={}, by={}", employee.getId(), employee.getEmployeeCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.EMPLOYEE_UPDATED, "EMPLOYEE", employee.getId(), null, null,
                Map.of("employeeCode", employee.getEmployeeCode(), "updatedBy", String.valueOf(actorId)));

        return toEmployeeResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployeeStatus(UUID id, UpdateEmployeeStatusRequest request, UUID actorId) {
        Employee employee = employeeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        EmployeeStatus previousStatus = employee.getStatus();
        employee.setStatus(request.status());

        if (request.exitDate() != null) {
            employee.setExitDate(request.exitDate());
        }

        employee = employeeRepository.save(employee);

        log.info("Employee status changed: id={}, code={}, from={}, to={}, by={}", employee.getId(), employee.getEmployeeCode(), previousStatus, request.status(), actorId);

        AuditAction action = (request.status() == EmployeeStatus.TERMINATED || request.status() == EmployeeStatus.RESIGNED)
                ? AuditAction.EMPLOYEE_DEACTIVATED
                : AuditAction.EMPLOYEE_STATUS_CHANGED;

        auditService.recordAuthEvent(null, action, "EMPLOYEE", employee.getId(), null, null,
                Map.of("employeeCode", employee.getEmployeeCode(), "previousStatus", previousStatus.name(), "newStatus", request.status().name(),
                        "reason", request.reason() != null ? request.reason() : "", "changedBy", String.valueOf(actorId)));

        return toEmployeeResponse(employee);
    }

    private EmployeeResponse toEmployeeResponse(Employee employee) {
        Department dept = employee.getDepartment();
        Team team = employee.getTeam();
        Employee manager = employee.getManager();

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getProfilePhotoUrl(),
                employee.getDesignation(),
                dept != null ? dept.getId().toString() : null,
                dept != null ? dept.getName() : null,
                team != null ? team.getId().toString() : null,
                team != null ? team.getName() : null,
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null,
                employee.getLocation(),
                employee.getStatus() != null ? employee.getStatus().name() : null,
                employee.getJoiningDate(),
                employee.getExitDate(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
