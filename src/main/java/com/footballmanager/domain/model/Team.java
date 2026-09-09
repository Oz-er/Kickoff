package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Locale;

public record Team(Long id, String name, String shortCode, String coachName, LocalDateTime createdAt) {
    public Team {
        if (id != null) {
            DomainValidation.positiveId(id, "Team id");
        }
        name = DomainValidation.requiredText(name, "Team name");
        shortCode = DomainValidation.requiredText(shortCode, "Short code").toUpperCase(Locale.ROOT);
        if (shortCode.length() < 2 || shortCode.length() > 5) {
            throw new ValidationException("Short code must contain 2 to 5 characters");
        }
        coachName = DomainValidation.optionalText(coachName);
    }

    public static Team create(String name, String shortCode, String coachName) {
        return new Team(null, name, shortCode, coachName, null);
    }
}
