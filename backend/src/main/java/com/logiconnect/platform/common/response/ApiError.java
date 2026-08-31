package com.logiconnect.platform.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Standard API Error envelope for all error conditions across LogiConnect platform.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success = false;
    private ErrorDetail error;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    private String path;

    public ApiError() {
        this.timestamp = Instant.now();
    }

    public ApiError(String code, String message, String path) {
        this.success = false;
        this.error = new ErrorDetail(code, message, null);
        this.timestamp = Instant.now();
        this.path = path;
    }

    public ApiError(String code, String message, List<FieldErrorDetail> details, String path) {
        this.success = false;
        this.error = new ErrorDetail(code, message, details);
        this.timestamp = Instant.now();
        this.path = path;
    }

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, path);
    }

    public static ApiError of(String code, String message, List<FieldErrorDetail> details, String path) {
        return new ApiError(code, message, details, path);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Nested class representing error code and message.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        private String code;
        private String message;
        private List<FieldErrorDetail> details;

        public ErrorDetail() {
        }

        public ErrorDetail(String code, String message, List<FieldErrorDetail> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<FieldErrorDetail> getDetails() {
            return details;
        }

        public void setDetails(List<FieldErrorDetail> details) {
            this.details = details;
        }
    }

    /**
     * Nested class for field-specific validation errors.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldErrorDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        private String field;
        private Object rejectedValue;
        private String message;

        public FieldErrorDetail() {
        }

        public FieldErrorDetail(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
