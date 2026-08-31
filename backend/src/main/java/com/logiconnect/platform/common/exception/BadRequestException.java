package com.logiconnect.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a request fails semantic or business validation (HTTP 400).
 */
public class BadRequestException extends LogiConnectException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message, Throwable cause) {
        super(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST, cause);
    }
}
