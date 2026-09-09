package com.footballmanager.application.dto;

import com.footballmanager.domain.model.TournamentFormat;

import java.util.List;

public record SchedulePreviewDto(
        long tournamentId,
        String tournamentName,
        TournamentFormat format,
        List<FixturePreviewDto> fixtures
) {
    public SchedulePreviewDto {
        fixtures = fixtures == null ? List.of() : List.copyOf(fixtures);
    }
}
