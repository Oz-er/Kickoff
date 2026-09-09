package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.exception.BusinessRuleException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsViewModelTest {
    @TempDir
    Path temporaryDirectory;

    private StandingsViewModel viewModel;
    private TournamentService tournamentService;

    @BeforeEach
    void setup() {
        DatabaseManager db = new DatabaseManager(temporaryDirectory.resolve("standings_" + System.nanoTime() + ".db"));
        new DatabaseInitializer(db).initialize();

        JdbcTournamentRepository tournaments = new JdbcTournamentRepository(db);
        JdbcTeamRepository teams = new JdbcTeamRepository(db);
        JdbcPlayerRepository players = new JdbcPlayerRepository(db);
        JdbcMatchRepository matches = new JdbcMatchRepository(db);

        tournamentService = new TournamentService(tournaments, new TournamentStateResolver());
        ReportService reportService = new ReportService(matches, teams, players, tournaments);

        viewModel = new StandingsViewModel(tournamentService, reportService);
    }

    @Test
    void isKnockoutReturnsTrueForKnockoutFormat() {
        TournamentDto t = tournamentService.create(new CreateTournamentRequest("K Cup", TournamentFormat.KNOCKOUT, LocalDate.now()));
        assertTrue(viewModel.isKnockout(t));
    }

    @Test
    void isKnockoutReturnsFalseForRoundRobinFormat() {
        TournamentDto t = tournamentService.create(new CreateTournamentRequest("League", TournamentFormat.ROUND_ROBIN, LocalDate.now()));
        assertFalse(viewModel.isKnockout(t));
    }

    @Test
    void isKnockoutReturnsFalseForNullTournament() {
        assertFalse(viewModel.isKnockout(null));
    }

    @Test
    void loadStandingsThrowsBusinessRuleExceptionForKnockoutTournament() {
        TournamentDto t = tournamentService.create(new CreateTournamentRequest("K Cup", TournamentFormat.KNOCKOUT, LocalDate.now()));
        assertThrows(BusinessRuleException.class, () -> viewModel.loadStandings(t.id()));
    }

    @Test
    void loadStandingsReturnsEmptyListForRoundRobinWithNoMatches() {
        TournamentDto t = tournamentService.create(new CreateTournamentRequest("League", TournamentFormat.ROUND_ROBIN, LocalDate.now()));
        assertEquals(0, viewModel.loadStandings(t.id()).size());
    }

    @Test
    void generateSummaryReportContainsTournamentName() {
        TournamentDto t = tournamentService.create(new CreateTournamentRequest("Grand Final", TournamentFormat.ROUND_ROBIN, LocalDate.now()));
        String report = viewModel.generateSummaryReport(t.id());
        assertTrue(report.contains("Grand Final"));
    }
}
