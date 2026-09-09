package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

public record TournamentRegistration(long tournamentId, long teamId, int seedNumber) {
    public TournamentRegistration {
        DomainValidation.positiveId(tournamentId, "Tournament id");
        DomainValidation.positiveId(teamId, "Team id");
        if (seedNumber <= 0) {
            throw new ValidationException("Seed number must be a positive number");
        }
    }
}
