package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

final class DomainValidation {
    private DomainValidation() {
    }

    static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    static String optionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static long positiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new ValidationException(fieldName + " must be a positive number");
        }
        return value;
    }
}
