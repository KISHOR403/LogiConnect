package com.logiconnect.platform.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating an employee profile (PATCH semantics).
 * All fields optional — only non-null values are applied.
 */
public record UpdateEmployeeRequest(

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phone,

        @Size(max = 1000, message = "Profile photo URL must not exceed 1000 characters")
        String profilePhotoUrl,

        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designation,

        UUID departmentId,

        UUID teamId,

        UUID managerId,

        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location
) {}
