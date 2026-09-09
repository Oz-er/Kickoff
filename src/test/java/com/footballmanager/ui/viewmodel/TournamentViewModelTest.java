package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.scheduling.SchedulingService;
import com.footballmanager.application.service.TeamService;
import com.footballmanager.application.service.TournamentLifecycleService;
import com.footballmanager.application.service.TournamentRegistrationService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.scheduling.KnockoutScheduleStrategy;
import com.footballmanager.domain.scheduling.RoundRobinScheduleStrategy;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.MatchProgressRepository;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchProgressRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRegistrationRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TournamentViewModelTest {
    @TempDir
    Path temporaryDirectory;

    private TournamentViewModel viewModel;

    @BeforeEach
    void setup() {
        DatabaseManager databaseManager = new DatabaseManager(temporaryDirectory.resolve("ui_" + System.nanoTime() + ".db"));
        new DatabaseInitializer(databaseManager).initialize();
        
        TeamRepository teams = new JdbcTeamRepository(databaseManager);
        TournamentRepository tournaments = new JdbcTournamentRepository(databaseManager);
        TournamentRegistrationRepository registrations = new JdbcTournamentRegistrationRepository(databaseManager);
        MatchRepository matches = new JdbcMatchRepository(databaseManager);
        MatchProgressRepository progress = new JdbcMatchProgressRepository(databaseManager);
        
        TournamentStateResolver resolver = new TournamentStateResolver();
        TournamentService tournamentService = new TournamentService(tournaments, resolver);
        TeamService teamService = new TeamService(teams);
        TournamentRegistrationService registrationService = new TournamentRegistrationService(tournaments, teams, registrations, resolver);
        TournamentLifecycleService lifecycleService = new TournamentLifecycleService(tournaments, registrations, progress, resolver);
        SchedulingService schedulingService = new SchedulingService(
                tournaments, registrations, teams, matches, resolver,
                List.of(new RoundRobinScheduleStrategy(), new KnockoutScheduleStrategy())
        );
        
        viewModel = new TournamentViewModel(
                tournamentService, registrationService, lifecycleService, schedulingService, teamService
        );
    }

    @Test
    void loadTournamentsReturnsCreatedTournaments() {
        viewModel.create(new CreateTournamentRequest("My Tournament", TournamentFormat.ROUND_ROBIN, LocalDate.now()));
        
        List<TournamentDto> tournaments = viewModel.loadTournaments();
        assertEquals(1, tournaments.size());
        assertEquals("My Tournament", tournaments.get(0).name());
    }
}
