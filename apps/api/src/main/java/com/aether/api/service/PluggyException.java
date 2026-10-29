package com.aether.api.service;

public class PluggyException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public PluggyException(String code, String message) {
        this(code,message,0);
    }

    public PluggyException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() { return httpStatus; }

    public String getCode() {
        return code;
    }
}
