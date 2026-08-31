package com.logiconnect.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a resource conflict occurs (e.g. duplicate unique key, email, employee code) (HTTP 409).
 */
public class ConflictException extends LogiConnectException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message, HttpStatus.CONFLICT);
    }

    public ConflictException(String message, Throwable cause) {
        super(ErrorCode.CONFLICT, message, HttpStatus.CONFLICT, cause);
    }
}
