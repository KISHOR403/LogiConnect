package com.logiconnect.platform.employee.controller;

import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.employee.dto.CreateEmployeeRequest;
import com.logiconnect.platform.employee.dto.EmployeeResponse;
import com.logiconnect.platform.employee.dto.UpdateEmployeeRequest;
import com.logiconnect.platform.employee.dto.UpdateEmployeeStatusRequest;
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
@RequestMapping("/employees")
@Tag(name = "Employees", description = "Employee directory, organizational profiles, onboarding, and lifecycle management")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_EMPLOYEES') or isAuthenticated()")
    @Operation(summary = "Search company directory", description = "Search and filter company directory with pagination across departments, teams, locations, and status", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> listEmployees(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        PageResponse<EmployeeResponse> response = employeeService.listEmployees(
                departmentId, teamId, status, location, search, page, size, sort, direction
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Employees retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_EMPLOYEES') or isAuthenticated()")
    @Operation(summary = "Get employee profile", description = "Returns detailed profile of an employee including organizational relationships", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable UUID id) {
        EmployeeResponse response = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Employee retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN') or hasAuthority('MANAGE_EMPLOYEES')")
    @Operation(summary = "Onboard new employee", description = "Creates a new employee record in the corporate directory", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        EmployeeResponse response = employeeService.createEmployee(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Employee created successfully"));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN') or hasAuthority('MANAGE_EMPLOYEES')")
    @Operation(summary = "Update employee profile", description = "Updates employee contact, designation, team, manager, or location", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        EmployeeResponse response = employeeService.updateEmployee(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Employee updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN') or hasAuthority('MANAGE_EMPLOYEES')")
    @Operation(summary = "Update employee status", description = "Transitions employee status (ACTIVE, PROBATION, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployeeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        EmployeeResponse response = employeeService.updateEmployeeStatus(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Employee status updated successfully"));
    }
}
