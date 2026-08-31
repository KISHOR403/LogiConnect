package com.logiconnect.platform.employee.dto;

import com.logiconnect.platform.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for transitioning an employee's employment status.
 */
public record UpdateEmployeeStatusRequest(

        @NotNull(message = "Status is required")
        EmployeeStatus status,

        LocalDate exitDate,

        String reason
) {}
