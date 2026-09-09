package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

import java.time.LocalDateTime;

public record Match(
        long id,
        long tournamentId,
        int roundNumber,
        Long homeTeamId,
        Long awayTeamId,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status,
        LocalDateTime scheduledAt,
        Long nextMatchId,
        NextMatchSlot nextMatchSlot
) {
    public Match {
        if (id <= 0 || tournamentId <= 0 || roundNumber <= 0) {
            throw new ValidationException("Match identifiers and round number must be positive");
        }
        if (status == null) {
            throw new ValidationException("Match status is required");
        }
        if ((nextMatchId == null) != (nextMatchSlot == null)) {
            throw new ValidationException("Next match and slot must either both be set or both be empty");
        }
        if ((homeTeamId != null && homeTeamId <= 0) || (awayTeamId != null && awayTeamId <= 0)) {
            throw new ValidationException("Team ids must be positive");
        }
        if (nextMatchId != null && nextMatchId <= 0) {
            throw new ValidationException("Next match id must be positive");
        }
        if (homeTeamId != null && homeTeamId.equals(awayTeamId)) {
            throw new ValidationException("A team cannot play itself");
        }
        if (status == MatchStatus.PENDING
                && homeTeamId != null && awayTeamId != null) {
            throw new ValidationException("Pending matches cannot have both teams assigned");
        }
        if (status == MatchStatus.PENDING && (homeScore != null || awayScore != null)) {
            throw new ValidationException("Pending matches cannot have scores");
        }
        if (status == MatchStatus.SCHEDULED
                && (homeTeamId == null || awayTeamId == null || homeScore != null || awayScore != null)) {
            throw new ValidationException("Scheduled matches require teams and no scores");
        }
        if (status == MatchStatus.COMPLETED
                && (homeTeamId == null || awayTeamId == null || homeScore == null || awayScore == null)) {
            throw new ValidationException("Completed matches require teams and scores");
        }
        if ((homeScore != null && homeScore < 0) || (awayScore != null && awayScore < 0)) {
            throw new ValidationException("Scores cannot be negative");
        }
    }
}
