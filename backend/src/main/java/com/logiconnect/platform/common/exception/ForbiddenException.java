package com.logiconnect.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an authenticated user lacks permission for a resource or action (HTTP 403).
 */
public class ForbiddenException extends LogiConnectException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(ErrorCode.FORBIDDEN, message, HttpStatus.FORBIDDEN, cause);
    }
}
