package com.footballmanager.application.exception;

public final class EntityNotFoundException extends ApplicationException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
