package com.logiconnect.platform.user.service;

import com.logiconnect.platform.auth.dto.CurrentUserResponse;
import com.logiconnect.platform.auth.service.AuthService;
import com.logiconnect.platform.security.authentication.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AuthService authService;

    public UserService(AuthService authService) {
        this.authService = authService;
    }

    public CurrentUserResponse getCurrentUserProfile(UserPrincipal currentUser) {
        return authService.getCurrentUser(currentUser);
    }
}
