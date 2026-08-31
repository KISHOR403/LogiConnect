package com.logiconnect.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Login identifier (employee code or email) is required")
    @JsonAlias({"email", "username", "loginId", "usernameOrEmail"})
    private String employeeCode;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    private String deviceInfo;

    public LoginRequest() {
    }

    public LoginRequest(String employeeCode, String password) {
        this.employeeCode = employeeCode;
        this.password = password;
    }

    public LoginRequest(String employeeCode, String password, String deviceInfo) {
        this.employeeCode = employeeCode;
        this.password = password;
        this.deviceInfo = deviceInfo;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
