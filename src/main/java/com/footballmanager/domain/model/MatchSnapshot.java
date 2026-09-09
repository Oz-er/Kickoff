package com.footballmanager.domain.model;

public record MatchSnapshot(
        long matchId,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status,
        Long nextMatchId,
        NextMatchSlot nextMatchSlot,
        Long progressedTeamId,
        MatchStatus nextMatchPreviousStatus,
        Long nextMatchPreviousHomeTeamId,
        Long nextMatchPreviousAwayTeamId
) {
}
