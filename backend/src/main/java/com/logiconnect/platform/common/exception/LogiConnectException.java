package com.logiconnect.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base abstract runtime exception for all business and domain exceptions in LogiConnect.
 */
public abstract class LogiConnectException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected LogiConnectException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected LogiConnectException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
