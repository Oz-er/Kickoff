package com.footballmanager.application.service;

import com.footballmanager.application.dto.DashboardSummaryDto;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.application.scheduling.SchedulingService;
import com.footballmanager.domain.model.Team;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private TeamRepository teams;
    private PlayerRepository players;
    private TournamentRepository tournaments;
    private TournamentRegistrationRepository registrations;
    private MatchRepository matches;
    private TournamentService tournamentService;
    private TournamentRegistrationService registrationService;
    private TournamentLifecycleService lifecycleService;
    private SchedulingService schedulingService;
    private ResultService resultService;
    private ReportService reportService;
    private int teamSequence;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("reports_" + System.nanoTime() + ".db"));
        new DatabaseInitializer(databaseManager).initialize();
        teams = new JdbcTeamRepository(databaseManager);
        players = new JdbcPlayerRepository(databaseManager);
        tournaments = new JdbcTournamentRepository(databaseManager);
        registrations = new JdbcTournamentRegistrationRepository(databaseManager);
        matches = new JdbcMatchRepository(databaseManager);
        MatchProgressRepository progress = new JdbcMatchProgressRepository(databaseManager);
        TournamentStateResolver resolver = new TournamentStateResolver();
        tournamentService = new TournamentService(tournaments, resolver);
        registrationService = new TournamentRegistrationService(tournaments, teams, registrations, resolver);
        lifecycleService = new TournamentLifecycleService(tournaments, registrations, progress, resolver);
        schedulingService = new SchedulingService(
                tournaments, registrations, teams, matches, resolver,
                List.of(new RoundRobinScheduleStrategy(), new KnockoutScheduleStrategy())
        );
        resultService = new ResultService(matches, tournaments, teams);
        reportService = new ReportService(matches, teams, players, tournaments);
        teamSequence = 0;
    }

    @Test
    void dashboardSummaryProvidesCorrectTotalsAndLimitsUpcomingFixtures() {
        long tournamentId = createGeneratedTournament("Dashboard", TournamentFormat.ROUND_ROBIN, 3);
        
        DashboardSummaryDto summary = reportService.getDashboardSummary(null, 2);
        
        assertEquals(7, summary.totalTeams());
        assertEquals(4, summary.totalPlayers());
        assertEquals(1, summary.totalTournaments());
        assertEquals(2, summary.upcomingFixtures().size());
    }

    @Test
    void upcomingFixturesCanBeFilteredByTournamentAndExcludesCompleted() {
        long tournament1 = createGeneratedTournament("T1", TournamentFormat.ROUND_ROBIN, 3);
        long tournament2 = createGeneratedTournament("T2", TournamentFormat.ROUND_ROBIN, 3);
        
        DashboardSummaryDto allSummary = reportService.getDashboardSummary(null, 10);
        assertEquals(6, allSummary.upcomingFixtures().size());
        
        resultService.recordResult(allSummary.upcomingFixtures().get(0).id(), 1, 0);
        
        DashboardSummaryDto filteredSummary = reportService.getDashboardSummary(tournament1, 10);
        assertEquals(2, filteredSummary.upcomingFixtures().size());
        for (MatchDto m : filteredSummary.upcomingFixtures()) {
            assertEquals(tournament1, m.tournamentId());
            assertEquals(com.footballmanager.domain.model.MatchStatus.SCHEDULED, m.status());
        }
    }

    @Test
    void upcomingFixturesAreOrderedById() {
        long tournamentId = createGeneratedTournament("Order", TournamentFormat.ROUND_ROBIN, 3);
        
        DashboardSummaryDto summary = reportService.getDashboardSummary(tournamentId, 10);
        List<MatchDto> upcoming = summary.upcomingFixtures();
        
        assertTrue(upcoming.size() >= 2);
        assertTrue(upcoming.get(0).id() < upcoming.get(1).id());
    }

    @Test
    void validationsRejectInvalidArguments() {
        assertThrows(ValidationException.class, () -> reportService.generateStandings(-1L));
        assertThrows(ValidationException.class, () -> reportService.getDashboardSummary(-1L, 5));
        assertThrows(ValidationException.class, () -> reportService.getDashboardSummary(1L, 0));
        assertThrows(ValidationException.class, () -> reportService.getDashboardSummary(1L, -1));
        assertThrows(ValidationException.class, () -> reportService.generateTournamentSummary(0L));
    }

    @Test
    void missingTournamentThrowsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> reportService.generateStandings(999L));
        assertThrows(EntityNotFoundException.class, () -> reportService.getDashboardSummary(999L, 5));
        assertThrows(EntityNotFoundException.class, () -> reportService.generateTournamentSummary(999L));
        assertThrows(EntityNotFoundException.class, () -> reportService.getTournamentMatches(999L));
    }

    @Test
    void knockoutTournamentThrowsBusinessRuleExceptionForStandings() {
        long tournamentId = createGeneratedTournament("Knockout", TournamentFormat.KNOCKOUT, 4);
        assertThrows(BusinessRuleException.class, () -> reportService.generateStandings(tournamentId));
    }

    @Test
    void plainTextSummaryFormatsCorrectly() {
        long tournamentId = createGeneratedTournament("Summary", TournamentFormat.ROUND_ROBIN, 3);
        String report = reportService.generateTournamentSummary(tournamentId);
        
        String expected = "Tournament: Summary\nFormat: ROUND_ROBIN\nStatus: FIXTURES_GENERATED\nStart Date: 2027-02-15\nTotal Matches: 3";
        assertEquals(expected, report);
    }

    private long createGeneratedTournament(String name, TournamentFormat format, int teamCount) {
        TournamentDto tournament = tournamentService.create(new CreateTournamentRequest(
                name, format, LocalDate.of(2027, 2, 15)
        ));
        List<TournamentRegistrationRequest> requests = new ArrayList<>();
        for (int seed = 1; seed <= teamCount; seed++) {
            String code = String.format("R%03d", ++teamSequence);
            Team team = teams.create(Team.create(name + " Team " + seed, code, null));
            requests.add(new TournamentRegistrationRequest(team.id(), seed));
        }
        registrationService.registerTeams(tournament.id(), requests);
        lifecycleService.closeRegistration(tournament.id());
        schedulingService.generateSchedule(schedulingService.previewSchedule(tournament.id()));
        return tournament.id();
    }
}
