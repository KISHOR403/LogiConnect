package com.logiconnect.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails or is missing (HTTP 401).
 */
public class UnauthorizedException extends LogiConnectException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, HttpStatus.UNAUTHORIZED, cause);
    }
}
