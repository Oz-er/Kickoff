package com.footballmanager.application.dto;

import java.util.List;

public record DashboardSummaryDto(
        long totalTeams,
        long totalPlayers,
        long totalTournaments,
        List<MatchDto> upcomingFixtures
) {}
