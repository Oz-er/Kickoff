package com.footballmanager.application.service;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.RegisteredTeamDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentRegistration;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentRegistrationServiceIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private TeamRepository teams;
    private TournamentRepository tournaments;
    private TournamentRegistrationRepository registrations;
    private TournamentService tournamentService;
    private TournamentRegistrationService registrationService;

    @BeforeEach
    void initializeDatabase() {
        DatabaseManager databaseManager = new DatabaseManager(temporaryDirectory.resolve("registration.db"));
        new DatabaseInitializer(databaseManager).initialize();
        teams = new JdbcTeamRepository(databaseManager);
        tournaments = new JdbcTournamentRepository(databaseManager);
        registrations = new JdbcTournamentRegistrationRepository(databaseManager);
        tournamentService = new TournamentService(tournaments);
        registrationService = new TournamentRegistrationService(tournaments, teams, registrations);
    }

    @Test
    void registersTeamsAndReturnsThemInSeedOrder() {
        Team firstTeam = teams.create(Team.create("First Registration Team", "FRT", null));
        Team secondTeam = teams.create(Team.create("Second Registration Team", "SRT", null));
        TournamentDto tournament = createTournament("Ordered Cup");

        List<RegisteredTeamDto> result = registrationService.registerTeams(tournament.id(), List.of(
                new TournamentRegistrationRequest(firstTeam.id(), 2),
                new TournamentRegistrationRequest(secondTeam.id(), 1)
        ));

        assertEquals(List.of(1, 2), result.stream().map(RegisteredTeamDto::seedNumber).toList());
        assertEquals(secondTeam.id(), result.getFirst().teamId());
        assertEquals("SRT", result.getFirst().shortCode());
        assertEquals(result, registrationService.findRegisteredTeams(tournament.id()));

        registrationService.unregisterTeam(tournament.id(), secondTeam.id());
        assertFalse(registrations.exists(tournament.id(), secondTeam.id()));
    }

    @Test
    void preventsDuplicateTeamsAndSeedsWithoutPartialWrites() {
        Team firstTeam = teams.create(Team.create("First Duplicate Team", "FDT", null));
        Team secondTeam = teams.create(Team.create("Second Duplicate Team", "SDT", null));
        TournamentDto tournament = createTournament("Duplicate Cup");

        assertThrows(BusinessRuleException.class, () -> registrationService.registerTeams(tournament.id(), List.of(
                new TournamentRegistrationRequest(firstTeam.id(), 1),
                new TournamentRegistrationRequest(firstTeam.id(), 2)
        )));
        assertTrue(registrations.findByTournamentId(tournament.id()).isEmpty());

        registrationService.registerTeams(tournament.id(), List.of(new TournamentRegistrationRequest(firstTeam.id(), 1)));
        assertThrows(BusinessRuleException.class, () -> registrationService.registerTeams(tournament.id(), List.of(
                new TournamentRegistrationRequest(secondTeam.id(), 1)
        )));
        assertEquals(1, registrations.findByTournamentId(tournament.id()).size());
    }

    @Test
    void databaseRollsBackTheWholeRegistrationBatchOnFailure() {
        Team team = teams.create(Team.create("Rollback Team", "RBT", null));
        TournamentDto tournament = createTournament("Rollback Cup");

        assertThrows(DataIntegrityException.class, () -> registrations.addAll(List.of(
                new TournamentRegistration(tournament.id(), team.id(), 1),
                new TournamentRegistration(tournament.id(), 999_999, 2)
        )));

        assertTrue(registrations.findByTournamentId(tournament.id()).isEmpty());
    }

    @Test
    void preservesRelationshipsAndSafelyCascadesDraftTournamentDeletion() {
        Team team = teams.create(Team.create("Relationship Team", "RLT", null));
        TournamentDto tournament = createTournament("Relationship Cup");
        registrationService.registerTeams(tournament.id(), List.of(new TournamentRegistrationRequest(team.id(), 1)));

        assertThrows(DataIntegrityException.class, () -> teams.deleteById(team.id()));
        tournamentService.delete(tournament.id());

        assertTrue(registrations.findByTournamentId(tournament.id()).isEmpty());
        teams.deleteById(team.id());
        assertFalse(teams.findById(team.id()).isPresent());
    }

    @Test
    void rejectsMissingTournamentAndTeamRelationships() {
        TournamentDto tournament = createTournament("Missing Relation Cup");

        assertThrows(EntityNotFoundException.class, () -> registrationService.registerTeams(tournament.id(), List.of(
                new TournamentRegistrationRequest(999_999, 1)
        )));
        assertThrows(EntityNotFoundException.class, () -> registrationService.findRegisteredTeams(999_999));
        assertTrue(registrations.findByTournamentId(tournament.id()).isEmpty());
    }

    @Test
    void preventsRegistrationChangesOutsideDraft() {
        Team team = teams.create(Team.create("Closed Registration Team", "CRT", null));
        TournamentDto tournament = createTournament("Closed Registration Cup");
        Tournament stored = tournaments.findById(tournament.id()).orElseThrow();
        tournaments.update(new Tournament(
                stored.id(),
                stored.name(),
                stored.format(),
                TournamentStatus.REGISTRATION_CLOSED,
                stored.startDate(),
                stored.createdAt()
        ));

        assertThrows(BusinessRuleException.class, () -> registrationService.registerTeams(tournament.id(), List.of(
                new TournamentRegistrationRequest(team.id(), 1)
        )));
        assertThrows(BusinessRuleException.class, () -> registrationService.unregisterTeam(tournament.id(), team.id()));
        assertTrue(registrations.findByTournamentId(tournament.id()).isEmpty());
    }

    private TournamentDto createTournament(String name) {
        return tournamentService.create(new CreateTournamentRequest(
                name,
                TournamentFormat.ROUND_ROBIN,
                LocalDate.of(2026, 12, 1)
        ));
    }
}
