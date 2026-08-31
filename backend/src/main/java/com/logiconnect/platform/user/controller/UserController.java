package com.logiconnect.platform.user.controller;

import com.logiconnect.platform.auth.dto.CurrentUserResponse;
import com.logiconnect.platform.common.response.ApiResponse;
import com.logiconnect.platform.common.util.SecurityUtils;
import com.logiconnect.platform.security.authentication.UserPrincipal;
import com.logiconnect.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User profile and identity endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Returns safe profile information for the authenticated user", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUserPrincipal().orElse(null);
        CurrentUserResponse response = userService.getCurrentUserProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "User profile retrieved successfully"));
    }
}
