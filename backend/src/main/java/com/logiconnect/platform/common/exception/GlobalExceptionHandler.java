package com.logiconnect.platform.common.exception;

import com.logiconnect.platform.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Exception Handler for centralized REST controller error management.
 *
 * Ensures consistent ApiError schema formatting, appropriate HTTP status codes,
 * and zero internal stack trace or database implementation detail leakage to clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle domain-specific application exceptions.
     */
    @ExceptionHandler(LogiConnectException.class)
    public ResponseEntity<ApiError> handleLogiConnectException(LogiConnectException ex, HttpServletRequest request) {
        log.warn("Application exception occurred: [code={}, status={}, message={}]",
                ex.getErrorCode(), ex.getHttpStatus(), ex.getMessage());

        ApiError error = ApiError.of(ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * Handle Jakarta Bean Validation failures on @Valid request bodies (@RequestBody).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> fieldErrors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ApiError.FieldErrorDetail(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            ));
        }

        log.warn("Validation failed for request to {}: {} field error(s)", request.getRequestURI(), fieldErrors.size());

        ApiError error = ApiError.of(
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed. Check field details.",
                fieldErrors,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle ConstraintViolationException on path variables, query params, etc.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> fieldErrors = new ArrayList<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.add(new ApiError.FieldErrorDetail(
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue(),
                    violation.getMessage()
            ));
        }

        log.warn("Constraint violation for request to {}: {} error(s)", request.getRequestURI(), fieldErrors.size());

        ApiError error = ApiError.of(
                ErrorCode.VALIDATION_FAILED,
                "Constraint validation failed.",
                fieldErrors,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle general ValidationException from validation constraints.
     */
    @ExceptionHandler(jakarta.validation.ValidationException.class)
    public ResponseEntity<ApiError> handleGeneralValidationException(jakarta.validation.ValidationException ex, HttpServletRequest request) {
        log.warn("Validation failure for request to {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.of(
                ErrorCode.VALIDATION_FAILED,
                ex.getMessage() != null ? ex.getMessage() : "Validation failed.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle malformed JSON body or unparseable payloads.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed HTTP request payload for {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.of(
                ErrorCode.BAD_REQUEST,
                "Malformed JSON request body or unreadable message payload.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle HTTP Method Not Supported (e.g. POST on GET endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HTTP method not supported: {} for {}", ex.getMethod(), request.getRequestURI());

        ApiError error = ApiError.of(
                ErrorCode.METHOD_NOT_ALLOWED,
                String.format("HTTP method '%s' is not supported for this endpoint.", ex.getMethod()),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * Handle Media Type Not Supported.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Media type not supported: {} for {}", ex.getContentType(), request.getRequestURI());

        ApiError error = ApiError.of(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                String.format("Content-Type '%s' is not supported.", ex.getContentType()),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    /**
     * Handle missing resource routes (404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No resource found for URL: {}", request.getRequestURI());

        ApiError error = ApiError.of(
                ErrorCode.RESOURCE_NOT_FOUND,
                String.format("The requested URL '%s' was not found.", request.getRequestURI()),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle bad credentials during authentication.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failure (BadCredentials) on {}", request.getRequestURI());

        ApiError error = ApiError.of(
                ErrorCode.UNAUTHORIZED,
                "Invalid credentials provided.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle disabled/locked user account exceptions.
     */
    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiError> handleAccountStatusException(Exception ex, HttpServletRequest request) {
        log.warn("Authentication blocked due to account state on {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.of(
                ErrorCode.UNAUTHORIZED,
                "Account is disabled, suspended, or locked. Please contact HR administration.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle general Spring Security AuthenticationException.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication exception on {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.of(
                ErrorCode.UNAUTHORIZED,
                "Authentication is required to access this resource.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle Spring Security AccessDeniedException (403 Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.of(
                ErrorCode.FORBIDDEN,
                "Access is denied. You do not possess the required permissions or role for this resource.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Catch-all fallback for unexpected server errors (500 Internal Server Error).
     * Never exposes raw internal exception details or SQL traces to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled unexpected exception during request to {}", request.getRequestURI(), ex);

        ApiError error = ApiError.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact system support if the issue persists.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
