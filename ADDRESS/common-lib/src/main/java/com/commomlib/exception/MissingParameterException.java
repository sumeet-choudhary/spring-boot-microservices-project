package com.commomlib.exception;
public class MissingParameterException extends BadRequestException {
    public MissingParameterException(String message) { super(message); }
}
