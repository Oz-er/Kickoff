package com.footballmanager.application.dto;

import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.NextMatchSlot;

import java.time.LocalDateTime;

public record MatchDto(
        long id,
        long tournamentId,
        int roundNumber,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        Integer homeScore,
        Integer awayScore,
        MatchStatus status,
        LocalDateTime scheduledAt,
        Long nextMatchId,
        NextMatchSlot nextMatchSlot
) {
}
