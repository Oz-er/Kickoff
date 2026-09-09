package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.DashboardSummaryDto;
import com.footballmanager.application.service.ReportService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DashboardViewModelTest {
    @TempDir
    Path temporaryDirectory;

    private DashboardViewModel viewModel;
    private TournamentService tournamentService;

    @BeforeEach
    void setup() {
        DatabaseManager db = new DatabaseManager(temporaryDirectory.resolve("dashboard_" + System.nanoTime() + ".db"));
        new DatabaseInitializer(db).initialize();

        JdbcTournamentRepository tournaments = new JdbcTournamentRepository(db);
        JdbcTeamRepository teams = new JdbcTeamRepository(db);
        JdbcPlayerRepository players = new JdbcPlayerRepository(db);
        JdbcMatchRepository matches = new JdbcMatchRepository(db);

        tournamentService = new TournamentService(tournaments, new TournamentStateResolver());
        ReportService reportService = new ReportService(matches, teams, players, tournaments);

        viewModel = new DashboardViewModel(tournamentService, reportService);
    }

    @Test
    void loadTournamentsReturnsAllCreatedTournaments() {
        tournamentService.create(new CreateTournamentRequest("Alpha Cup", TournamentFormat.ROUND_ROBIN, LocalDate.now()));
        tournamentService.create(new CreateTournamentRequest("Beta League", TournamentFormat.KNOCKOUT, LocalDate.now()));

        assertEquals(2, viewModel.loadTournaments().size());
    }

    @Test
    void loadSummaryWithNullTournamentIdReturnsSummaryWithUpcomingList() {
        DashboardSummaryDto summary = viewModel.loadSummary(null);

        assertNotNull(summary);
        assertNotNull(summary.upcomingFixtures());
    }

    @Test
    void loadSummaryCountsTournamentAfterCreation() {
        tournamentService.create(new CreateTournamentRequest("Cup", TournamentFormat.ROUND_ROBIN, LocalDate.now()));

        DashboardSummaryDto summary = viewModel.loadSummary(null);

        assertEquals(1, summary.totalTournaments());
    }
}
