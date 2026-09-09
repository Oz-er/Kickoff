package com.footballmanager.application.service;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.MatchProgressRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchProgressRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRegistrationRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TournamentLifecycleServiceIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private TeamRepository teams;
    private TournamentRepository tournaments;
    private TournamentRegistrationRepository registrations;
    private TournamentService tournamentService;
    private TournamentRegistrationService registrationService;
    private TournamentLifecycleService lifecycleService;
    private int teamSequence;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("lifecycle.db"));
        new DatabaseInitializer(databaseManager).initialize();
        teams = new JdbcTeamRepository(databaseManager);
        tournaments = new JdbcTournamentRepository(databaseManager);
        registrations = new JdbcTournamentRegistrationRepository(databaseManager);
        MatchProgressRepository progress = new JdbcMatchProgressRepository(databaseManager);
        TournamentStateResolver resolver = new TournamentStateResolver();
        tournamentService = new TournamentService(tournaments, resolver);
        registrationService = new TournamentRegistrationService(tournaments, teams, registrations, resolver);
        lifecycleService = new TournamentLifecycleService(tournaments, registrations, progress, resolver);
        teamSequence = 0;
    }

    @Test
    void closesRoundRobinRegistrationAndPersistsTheTransition() {
        TournamentDto tournament = createAndRegister("Round Robin Cup", TournamentFormat.ROUND_ROBIN, 3);

        TournamentDto transitioned = lifecycleService.closeRegistration(tournament.id());

        assertEquals(TournamentStatus.REGISTRATION_CLOSED, transitioned.status());
        assertEquals(
                TournamentStatus.REGISTRATION_CLOSED,
                new JdbcTournamentRepository(databaseManager).findById(tournament.id()).orElseThrow().status()
        );
        assertThrows(BusinessRuleException.class, () -> tournamentService.delete(tournament.id()));
    }

    @Test
    void rejectsInvalidTeamCountsWithoutPersistingAStatusChange() {
        TournamentDto roundRobin = createAndRegister("Small League", TournamentFormat.ROUND_ROBIN, 2);
        TournamentDto knockout = createAndRegister("Small Knockout", TournamentFormat.KNOCKOUT, 3);

        assertThrows(BusinessRuleException.class, () -> lifecycleService.closeRegistration(roundRobin.id()));
        assertThrows(BusinessRuleException.class, () -> lifecycleService.closeRegistration(knockout.id()));
        assertEquals(TournamentStatus.DRAFT, tournaments.findById(roundRobin.id()).orElseThrow().status());
        assertEquals(TournamentStatus.DRAFT, tournaments.findById(knockout.id()).orElseThrow().status());
    }

    @Test
    void closesSupportedKnockoutRegistration() {
        TournamentDto tournament = createAndRegister("Four Team Knockout", TournamentFormat.KNOCKOUT, 4);

        assertEquals(TournamentStatus.REGISTRATION_CLOSED, lifecycleService.closeRegistration(tournament.id()).status());
    }

    @Test
    void marksFixturesGeneratedOnlyAfterClosureAndSavedFixtures() throws Exception {
        List<Team> registeredTeams = createTeams("Fixture Team", 3);
        TournamentDto tournament = createTournament("Fixture Cup", TournamentFormat.ROUND_ROBIN);
        register(tournament.id(), registeredTeams);
        insertScheduledFixture(tournament.id(), registeredTeams.get(0).id(), registeredTeams.get(1).id());

        assertThrows(BusinessRuleException.class, () -> lifecycleService.markFixturesGenerated(tournament.id()));
        assertEquals(TournamentStatus.DRAFT, tournaments.findById(tournament.id()).orElseThrow().status());

        lifecycleService.closeRegistration(tournament.id());
        assertEquals(TournamentStatus.FIXTURES_GENERATED, lifecycleService.markFixturesGenerated(tournament.id()).status());
        assertEquals(TournamentStatus.FIXTURES_GENERATED, tournaments.findById(tournament.id()).orElseThrow().status());
    }

    @Test
    void completesOnlyAfterEveryFixtureIsCompleted() throws Exception {
        List<Team> registeredTeams = createTeams("Complete Team", 3);
        TournamentDto tournament = createTournament("Completion Cup", TournamentFormat.ROUND_ROBIN);
        register(tournament.id(), registeredTeams);
        long matchId = insertScheduledFixture(tournament.id(), registeredTeams.get(0).id(), registeredTeams.get(1).id());
        lifecycleService.closeRegistration(tournament.id());
        lifecycleService.markFixturesGenerated(tournament.id());

        assertThrows(BusinessRuleException.class, () -> lifecycleService.complete(tournament.id()));
        assertEquals(TournamentStatus.FIXTURES_GENERATED, tournaments.findById(tournament.id()).orElseThrow().status());

        completeFixture(matchId);
        assertEquals(TournamentStatus.COMPLETED, lifecycleService.complete(tournament.id()).status());
        assertEquals(
                TournamentStatus.COMPLETED,
                new JdbcTournamentRepository(databaseManager).findById(tournament.id()).orElseThrow().status()
        );
        assertThrows(BusinessRuleException.class, () -> lifecycleService.complete(tournament.id()));
    }

    private TournamentDto createAndRegister(String name, TournamentFormat format, int teamCount) {
        List<Team> registeredTeams = createTeams(name.replace(" ", "") + "Team", teamCount);
        TournamentDto tournament = createTournament(name, format);
        register(tournament.id(), registeredTeams);
        return tournament;
    }

    private TournamentDto createTournament(String name, TournamentFormat format) {
        return tournamentService.create(new CreateTournamentRequest(name, format, LocalDate.of(2026, 12, 1)));
    }

    private List<Team> createTeams(String prefix, int count) {
        List<Team> created = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            String code = String.format("L%03d", ++teamSequence);
            created.add(teams.create(Team.create(prefix + " " + index, code, null)));
        }
        return created;
    }

    private void register(long tournamentId, List<Team> registeredTeams) {
        List<TournamentRegistrationRequest> requests = new ArrayList<>();
        for (int index = 0; index < registeredTeams.size(); index++) {
            requests.add(new TournamentRegistrationRequest(registeredTeams.get(index).id(), index + 1));
        }
        registrationService.registerTeams(tournamentId, requests);
    }

    private long insertScheduledFixture(long tournamentId, long homeTeamId, long awayTeamId) throws SQLException {
        String sql = "INSERT INTO matches(tournament_id, round_number, home_team_id, away_team_id, status) "
                + "VALUES (?, ?, ?, ?, 'SCHEDULED') RETURNING id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setInt(2, 1);
            statement.setLong(3, homeTeamId);
            statement.setLong(4, awayTeamId);
            return statement.executeQuery().getLong(1);
        }
    }

    private void completeFixture(long matchId) throws SQLException {
        String sql = "UPDATE matches SET home_score = ?, away_score = ?, status = 'COMPLETED' WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, 2);
            statement.setInt(2, 1);
            statement.setLong(3, matchId);
            statement.executeUpdate();
        }
    }
}
