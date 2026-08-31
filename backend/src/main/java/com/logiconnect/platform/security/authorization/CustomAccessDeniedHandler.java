package com.logiconnect.platform.security.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiconnect.platform.common.exception.ErrorCode;
import com.logiconnect.platform.common.response.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Access Denied Handler invoked when an authenticated user attempts to access a resource
 * without sufficient roles or permissions.
 * Returns a standardized HTTP 403 Forbidden ApiError JSON response.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        log.warn("Forbidden access attempt to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiError apiError = ApiError.of(
                ErrorCode.FORBIDDEN,
                "Access denied. You do not possess the required permissions or role for this resource.",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), apiError);
    }
}
