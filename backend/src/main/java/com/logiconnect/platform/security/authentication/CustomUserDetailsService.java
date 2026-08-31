package com.logiconnect.platform.security.authentication;

import com.logiconnect.platform.employee.entity.Employee;
import com.logiconnect.platform.role.entity.Role;
import com.logiconnect.platform.user.entity.User;
import com.logiconnect.platform.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByIdentifierWithDetails(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        return toUserPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
        User user = userRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return toUserPrincipal(user);
    }

    public static UserPrincipal toUserPrincipal(User user) {
        Employee employee = user.getEmployee();
        String employeeCode = employee != null ? employee.getEmployeeCode() : "";
        String firstName = employee != null ? employee.getFirstName() : "";
        String lastName = employee != null ? employee.getLastName() : "";

        Set<String> roles = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : new HashSet<>();

        Set<String> permissions = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(p -> permissions.add(p.getName()));
                }
            }
        }

        boolean active = user.isEligibleForLogin(Instant.now());

        return UserPrincipal.create(
                user.getId(),
                employeeCode,
                user.getEmail(),
                firstName,
                lastName,
                user.getPasswordHash(),
                active,
                roles,
                permissions
        );
    }
}
