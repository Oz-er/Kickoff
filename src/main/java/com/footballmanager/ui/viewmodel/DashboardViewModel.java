package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.DashboardSummaryDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.service.ReportService;
import com.footballmanager.application.service.TournamentService;

import java.util.List;
import java.util.Objects;

public final class DashboardViewModel {
    private static final int DEFAULT_UPCOMING_LIMIT = 5;

    private final TournamentService tournamentService;
    private final ReportService reportService;

    public DashboardViewModel(TournamentService tournamentService, ReportService reportService) {
        this.tournamentService = Objects.requireNonNull(tournamentService);
        this.reportService = Objects.requireNonNull(reportService);
    }

    public List<TournamentDto> loadTournaments() {
        return tournamentService.findAll();
    }

    public DashboardSummaryDto loadSummary(Long tournamentId) {
        return reportService.getDashboardSummary(tournamentId, DEFAULT_UPCOMING_LIMIT);
    }
}
