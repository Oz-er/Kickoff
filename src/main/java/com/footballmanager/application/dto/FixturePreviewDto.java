package com.footballmanager.application.dto;

import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.NextMatchSlot;

public record FixturePreviewDto(
        int fixtureNumber,
        int roundNumber,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        MatchStatus status,
        Integer nextFixtureNumber,
        NextMatchSlot nextMatchSlot
) {
}
