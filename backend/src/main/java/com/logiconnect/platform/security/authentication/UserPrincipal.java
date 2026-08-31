package com.logiconnect.platform.security.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails principal representing the authenticated user in the security context.
 */
public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String employeeCode;
    private final String email;
    private final String firstName;
    private final String lastName;

    @JsonIgnore
    private final String password;

    private final boolean active;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            UUID id,
            String employeeCode,
            String email,
            String firstName,
            String lastName,
            String password,
            boolean active,
            Set<String> roles,
            Set<String> permissions,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.active = active;
        this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        this.permissions = permissions != null ? Collections.unmodifiableSet(permissions) : Collections.emptySet();
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    public static UserPrincipal create(
            UUID id,
            String employeeCode,
            String email,
            String firstName,
            String lastName,
            String password,
            boolean active,
            Set<String> roles,
            Set<String> permissions
    ) {
        List<GrantedAuthority> authorities = (roles != null ? roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .collect(Collectors.toList()) : new java.util.ArrayList<>());

        if (permissions != null) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }

        return new UserPrincipal(
                id,
                employeeCode,
                email,
                firstName,
                lastName,
                password,
                active,
                roles,
                permissions,
                Collections.unmodifiableList(authorities)
        );
    }

    public UUID getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
