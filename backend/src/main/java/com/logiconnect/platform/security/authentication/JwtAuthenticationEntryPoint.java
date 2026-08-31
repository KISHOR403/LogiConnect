package com.logiconnect.platform.security.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.common.exception.ErrorCode;
import com.logiconnect.platform.common.response.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Authentication Entry Point invoked when an unauthenticated request attempts to access a protected resource.
 * Returns a standardized HTTP 401 Unauthorized ApiError JSON response.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        log.warn("Unauthorized access attempt to {}: {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiError apiError = ApiError.of(
                ErrorCode.UNAUTHORIZED,
                "Authentication required. Full authentication is required to access this resource.",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), apiError);
    }
}
