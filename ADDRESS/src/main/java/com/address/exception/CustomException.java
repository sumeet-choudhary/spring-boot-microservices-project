package com.address.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
    private final HttpStatus status;
    public CustomException(String message, HttpStatus status) { super(message); this.status = status; }
    public CustomException(String message) { this(message, HttpStatus.INTERNAL_SERVER_ERROR); }
    public HttpStatus getStatus() { return status; }
}
