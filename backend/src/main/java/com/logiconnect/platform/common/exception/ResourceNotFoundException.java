package com.logiconnect.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested entity or resource is not found (HTTP 404).
 */
public class ResourceNotFoundException extends LogiConnectException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                String.format("%s was not found with %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.NOT_FOUND);
    }
}
