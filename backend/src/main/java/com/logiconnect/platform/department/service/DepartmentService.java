package com.logiconnect.platform.department.service;

import com.logiconnect.platform.audit.entity.AuditAction;
import com.logiconnect.platform.audit.service.AuditService;
import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ConflictException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.department.dto.CreateDepartmentRequest;
import com.logiconnect.platform.department.dto.DepartmentResponse;
import com.logiconnect.platform.department.dto.UpdateDepartmentRequest;
import com.logiconnect.platform.department.entity.Department;
import com.logiconnect.platform.department.repository.DepartmentRepository;
import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.employee.repository.EmployeeRepository;
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
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "status", "createdAt");

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository,
                             AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> listDepartments(String status, String search, int page, int size, String sortField, String sortDirection) {
        size = Math.min(size <= 0 ? DEFAULT_PAGE_SIZE : size, MAX_PAGE_SIZE);
        String resolvedSort = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(direction, resolvedSort));
        Page<Department> departmentPage = departmentRepository.findAllFiltered(status, search, pageable);

        Page<DepartmentResponse> responsePage = departmentPage.map(this::toDepartmentResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return toDepartmentResponse(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request, UUID actorId) {
        if (departmentRepository.existsByCode(request.code())) {
            throw new ConflictException("Department with code '" + request.code() + "' already exists");
        }
        if (departmentRepository.existsByName(request.name())) {
            throw new ConflictException("Department with name '" + request.name() + "' already exists");
        }

        // Validate manager if specified
        if (request.managerId() != null) {
            employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (manager)", "id", request.managerId()));
        }

        Department department = new Department();
        department.setCode(request.code());
        department.setName(request.name());
        department.setDescription(request.description());
        department.setManagerId(request.managerId());
        department.setStatus("ACTIVE");

        department = departmentRepository.save(department);

        log.info("Department created: code={}, name={}, by={}", department.getCode(), department.getName(), actorId);
        auditService.recordAuthEvent(null, AuditAction.DEPARTMENT_CREATED, "DEPARTMENT", department.getId(), null, null,
                Map.of("code", department.getCode(), "name", department.getName(), "createdBy", String.valueOf(actorId)));

        return toDepartmentResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, UpdateDepartmentRequest request, UUID actorId) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (request.name() != null && !request.name().isBlank()) {
            if (!request.name().equals(department.getName()) && departmentRepository.existsByName(request.name())) {
                throw new ConflictException("Department with name '" + request.name() + "' already exists");
            }
            department.setName(request.name());
        }
        if (request.description() != null) {
            department.setDescription(request.description());
        }
        if (request.managerId() != null) {
            employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (manager)", "id", request.managerId()));
            department.setManagerId(request.managerId());
        }
        if (request.status() != null && !request.status().isBlank()) {
            validateDepartmentStatus(request.status());
            department.setStatus(request.status());
        }

        department = departmentRepository.save(department);

        log.info("Department updated: id={}, code={}, by={}", department.getId(), department.getCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.DEPARTMENT_UPDATED, "DEPARTMENT", department.getId(), null, null,
                Map.of("code", department.getCode(), "updatedBy", String.valueOf(actorId)));

        return toDepartmentResponse(department);
    }

    @Transactional
    public void deactivateDepartment(UUID id, UUID actorId) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if ("INACTIVE".equals(department.getStatus()) || "ARCHIVED".equals(department.getStatus())) {
            throw new BadRequestException("Department is already inactive or archived");
        }

        department.setStatus("INACTIVE");
        departmentRepository.save(department);

        log.info("Department deactivated: id={}, code={}, by={}", department.getId(), department.getCode(), actorId);
        auditService.recordAuthEvent(null, AuditAction.DEPARTMENT_DEACTIVATED, "DEPARTMENT", department.getId(), null, null,
                Map.of("code", department.getCode(), "deactivatedBy", String.valueOf(actorId)));
    }

    private DepartmentResponse toDepartmentResponse(Department department) {
        String managerName = null;
        if (department.getManagerId() != null) {
            managerName = employeeRepository.findById(department.getManagerId())
                    .map(Employee::getFullName)
                    .orElse(null);
        }

        long teamCount = departmentRepository.countActiveTeamsByDepartmentId(department.getId());
        long employeeCount = departmentRepository.countActiveEmployeesByDepartmentId(department.getId());

        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.getStatus(),
                department.getManagerId(),
                managerName,
                teamCount,
                employeeCount,
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }

    private void validateDepartmentStatus(String status) {
        if (!Set.of("ACTIVE", "INACTIVE", "ARCHIVED").contains(status)) {
            throw new BadRequestException("Invalid department status: " + status + ". Must be one of: ACTIVE, INACTIVE, ARCHIVED");
        }
    }
}
