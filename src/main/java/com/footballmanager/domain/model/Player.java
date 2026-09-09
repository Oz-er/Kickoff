package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

import java.time.LocalDateTime;

public record Player(Long id, long teamId, String fullName, int shirtNumber, String position, LocalDateTime createdAt) {
    public Player {
        if (id != null) {
            DomainValidation.positiveId(id, "Player id");
        }
        DomainValidation.positiveId(teamId, "Team id");
        fullName = DomainValidation.requiredText(fullName, "Player name");
        if (shirtNumber < 1 || shirtNumber > 99) {
            throw new ValidationException("Shirt number must be between 1 and 99");
        }
        position = DomainValidation.optionalText(position);
    }

    public static Player create(long teamId, String fullName, int shirtNumber, String position) {
        return new Player(null, teamId, fullName, shirtNumber, position, null);
    }
}
