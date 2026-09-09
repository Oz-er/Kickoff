package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

public record FixtureDraft(
        int fixtureNumber,
        long tournamentId,
        int roundNumber,
        Long homeTeamId,
        Long awayTeamId,
        MatchStatus status,
        Integer nextFixtureNumber,
        NextMatchSlot nextMatchSlot
) {
    public FixtureDraft {
        if (fixtureNumber <= 0) {
            throw new ValidationException("Fixture number must be positive");
        }
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament id must be positive");
        }
        if (roundNumber <= 0) {
            throw new ValidationException("Round number must be positive");
        }
        if (status == null) {
            throw new ValidationException("Match status is required");
        }
        if ((nextFixtureNumber == null) != (nextMatchSlot == null)) {
            throw new ValidationException("Next fixture and slot must either both be set or both be empty");
        }
        if (nextFixtureNumber != null && nextFixtureNumber <= 0) {
            throw new ValidationException("Next fixture number must be positive");
        }
        if ((homeTeamId != null && homeTeamId <= 0) || (awayTeamId != null && awayTeamId <= 0)) {
            throw new ValidationException("Team ids must be positive");
        }
        if (homeTeamId != null && homeTeamId.equals(awayTeamId)) {
            throw new ValidationException("A team cannot play itself");
        }
        if (status == MatchStatus.PENDING && (homeTeamId != null || awayTeamId != null)) {
            throw new ValidationException("Pending fixtures cannot have assigned teams");
        }
        if (status == MatchStatus.SCHEDULED && (homeTeamId == null || awayTeamId == null)) {
            throw new ValidationException("Scheduled fixtures require both teams");
        }
        if (status == MatchStatus.COMPLETED) {
            throw new ValidationException("Generated fixtures cannot already be completed");
        }
    }
}
