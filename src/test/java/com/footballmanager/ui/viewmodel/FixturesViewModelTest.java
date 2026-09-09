package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.event.ApplicationEventPublisher;
import com.footballmanager.application.service.ReportService;
import com.footballmanager.application.service.ResultService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FixturesViewModelTest {
    @TempDir
    Path temporaryDirectory;

    private FixturesViewModel viewModel;

    @BeforeEach
    void setup() {
        DatabaseManager databaseManager = new DatabaseManager(temporaryDirectory.resolve("fixtures_ui_" + System.nanoTime() + ".db"));
        new DatabaseInitializer(databaseManager).initialize();
        
        TournamentRepository tournaments = new JdbcTournamentRepository(databaseManager);
        TeamRepository teams = new JdbcTeamRepository(databaseManager);
        MatchRepository matches = new JdbcMatchRepository(databaseManager);
        
        TournamentStateResolver resolver = new TournamentStateResolver();
        TournamentService tournamentService = new TournamentService(tournaments, resolver);
        ReportService reportService = new ReportService(
            matches, teams, new com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository(databaseManager),
            tournaments
        );
        ResultService resultService = new ResultService(matches, tournaments, teams);
        ApplicationEventPublisher eventPublisher = new ApplicationEventPublisher();
        
        viewModel = new FixturesViewModel(tournamentService, reportService, resultService, eventPublisher);
    }

    @Test
    void canUndoIsFalseInitially() {
        assertFalse(viewModel.canUndo());
    }
}
