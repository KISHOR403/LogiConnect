package com.logiconnect.platform.department.controller;

import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.department.dto.CreateDepartmentRequest;
import com.logiconnect.platform.department.dto.DepartmentResponse;
import com.logiconnect.platform.department.dto.UpdateDepartmentRequest;
import com.logiconnect.platform.department.service.DepartmentService;
import com.logiconnect.platform.employee.dto.EmployeeResponse;
import com.logiconnect.platform.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/departments")
@Tag(name = "Departments", description = "Enterprise departments, organizational units, and hierarchy management")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final EmployeeService employeeService;

    public DepartmentController(DepartmentService departmentService, EmployeeService employeeService) {
        this.departmentService = departmentService;
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "List all departments", description = "Returns a paginated list of company departments with manager and headcount summaries", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> listDepartments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        PageResponse<DepartmentResponse> response = departmentService.listDepartments(status, search, page, size, sort, direction);
        return ResponseEntity.ok(ApiResponse.success(response, "Departments retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Returns detailed department entity including manager profile and operational status", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable UUID id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Department retrieved successfully"));
    }

    @GetMapping("/{id}/employees")
    @Operation(summary = "List department employees", description = "Returns a paginated list of employees belonging to this department", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getDepartmentEmployees(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<EmployeeResponse> response = employeeService.listEmployeesByDepartment(id, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Department employees retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "Create department", description = "Creates a new organizational department", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        DepartmentResponse response = departmentService.createDepartment(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Department created successfully"));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "Update department", description = "Updates department name, description, manager, or status", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        DepartmentResponse response = departmentService.updateDepartment(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Department updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN') or hasAuthority('MANAGE_DEPARTMENTS')")
    @Operation(summary = "Deactivate department", description = "Deactivates a department (soft delete status transition)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> deactivateDepartment(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        departmentService.deactivateDepartment(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Department deactivated successfully"));
    }
}
