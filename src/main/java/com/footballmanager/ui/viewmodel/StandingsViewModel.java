package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.StandingsRowDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.service.ReportService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.model.TournamentFormat;

import java.util.List;
import java.util.Objects;

public final class StandingsViewModel {
    private final TournamentService tournamentService;
    private final ReportService reportService;

    public StandingsViewModel(TournamentService tournamentService, ReportService reportService) {
        this.tournamentService = Objects.requireNonNull(tournamentService);
        this.reportService = Objects.requireNonNull(reportService);
    }

    public List<TournamentDto> loadTournaments() {
        return tournamentService.findAll();
    }

    public boolean isKnockout(TournamentDto tournament) {
        return tournament != null && tournament.format() == TournamentFormat.KNOCKOUT;
    }

    public List<StandingsRowDto> loadStandings(long tournamentId) {
        return reportService.generateStandings(tournamentId);
    }

    public List<MatchDto> loadKnockoutMatches(long tournamentId) {
        return reportService.getTournamentMatches(tournamentId);
    }

    public String generateSummaryReport(long tournamentId) {
        return reportService.generateTournamentSummary(tournamentId);
    }
}
