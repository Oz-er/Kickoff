package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Tournament(
        Long id,
        String name,
        TournamentFormat format,
        TournamentStatus status,
        LocalDate startDate,
        LocalDateTime createdAt
) {
    public Tournament {
        if (id != null) {
            DomainValidation.positiveId(id, "Tournament id");
        }
        name = DomainValidation.requiredText(name, "Tournament name");
        if (format == null) {
            throw new ValidationException("Tournament format is required");
        }
        if (status == null) {
            throw new ValidationException("Tournament status is required");
        }
        if (startDate == null) {
            throw new ValidationException("Tournament start date is required");
        }
    }

    public static Tournament create(String name, TournamentFormat format, LocalDate startDate) {
        return new Tournament(null, name, format, TournamentStatus.DRAFT, startDate, null);
    }
}
