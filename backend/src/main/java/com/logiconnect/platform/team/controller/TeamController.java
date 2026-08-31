package com.logiconnect.platform.team.controller;

import com.logiconnect.platform.common.pagination.PageResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.employee.dto.EmployeeResponse;
import com.logiconnect.platform.employee.service.EmployeeService;
import com.logiconnect.platform.team.dto.CreateTeamRequest;
import com.logiconnect.platform.team.dto.TeamResponse;
import com.logiconnect.platform.team.dto.UpdateTeamRequest;
import com.logiconnect.platform.team.service.TeamService;
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
@RequestMapping("/teams")
@Tag(name = "Teams", description = "Operational teams, hub units, and department sub-units")
public class TeamController {

    private final TeamService teamService;
    private final EmployeeService employeeService;

    public TeamController(TeamService teamService, EmployeeService employeeService) {
        this.teamService = teamService;
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "List all teams", description = "Returns a paginated list of teams, optionally filtered by department", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<TeamResponse>>> listTeams(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        PageResponse<TeamResponse> response = teamService.listTeams(departmentId, status, search, page, size, sort, direction);
        return ResponseEntity.ok(ApiResponse.success(response, "Teams retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID", description = "Returns detailed team view including department and team lead", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        TeamResponse response = teamService.getTeamById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Team retrieved successfully"));
    }

    @GetMapping("/{id}/employees")
    @Operation(summary = "List team employees", description = "Returns a paginated list of employees belonging to this team", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getTeamEmployees(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<EmployeeResponse> response = employeeService.listEmployeesByTeam(id, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Team employees retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER') or hasAuthority('MANAGE_TEAMS')")
    @Operation(summary = "Create team", description = "Creates a new operational team within a department", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @Valid @RequestBody CreateTeamRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TeamResponse response = teamService.createTeam(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Team created successfully"));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER') or hasAuthority('MANAGE_TEAMS')")
    @Operation(summary = "Update team", description = "Updates team name, description, team lead, or status", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        TeamResponse response = teamService.updateTeam(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Team updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_ADMIN', 'MANAGER') or hasAuthority('MANAGE_TEAMS')")
    @Operation(summary = "Deactivate team", description = "Deactivates a team (soft delete status transition)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> deactivateTeam(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        teamService.deactivateTeam(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Team deactivated successfully"));
    }
}
