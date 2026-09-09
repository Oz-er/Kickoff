package com.footballmanager.application.exception;

public final class DataIntegrityException extends ApplicationException {
    public DataIntegrityException(String message) {
        super(message);
    }

    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
