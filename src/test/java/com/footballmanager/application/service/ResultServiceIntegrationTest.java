package com.footballmanager.application.service;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.application.scheduling.SchedulingService;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.scheduling.KnockoutScheduleStrategy;
import com.footballmanager.domain.scheduling.RoundRobinScheduleStrategy;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.MatchProgressRepository;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchProgressRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultServiceIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private TeamRepository teams;
    private TournamentRepository tournaments;
    private TournamentRegistrationRepository registrations;
    private MatchRepository matches;
    private TournamentService tournamentService;
    private TournamentRegistrationService registrationService;
    private TournamentLifecycleService lifecycleService;
    private SchedulingService schedulingService;
    private ResultService resultService;
    private int teamSequence;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("results.db"));
        new DatabaseInitializer(databaseManager).initialize();
        teams = new JdbcTeamRepository(databaseManager);
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
        teamSequence = 0;
    }

    @Test
    void recordsRoundRobinResultAndPersists() {
        long tournamentId = createGeneratedTournament("RR Result", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        MatchDto result = resultService.recordResult(firstMatch.id(), 2, 1);

        assertEquals(MatchStatus.COMPLETED, result.status());
        assertEquals(2, result.homeScore());
        assertEquals(1, result.awayScore());
        Match persisted = matches.findById(firstMatch.id()).orElseThrow();
        assertEquals(MatchStatus.COMPLETED, persisted.status());
        assertEquals(2, persisted.homeScore());
        assertEquals(1, persisted.awayScore());
    }

    @Test
    void roundRobinDrawIsAllowed() {
        long tournamentId = createGeneratedTournament("RR Draw", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        MatchDto result = resultService.recordResult(firstMatch.id(), 1, 1);

        assertEquals(MatchStatus.COMPLETED, result.status());
        assertEquals(1, result.homeScore());
        assertEquals(1, result.awayScore());
    }

    @Test
    void knockoutDrawIsRejected() {
        long tournamentId = createGeneratedTournament("KO Draw", TournamentFormat.KNOCKOUT, 4);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        assertThrows(BusinessRuleException.class, () -> resultService.recordResult(firstMatch.id(), 2, 2));
        Match persisted = matches.findById(firstMatch.id()).orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, persisted.status());
        assertNull(persisted.homeScore());
    }

    @Test
    void knockoutResultAdvancesWinnerToNextMatch() {
        long tournamentId = createGeneratedTournament("KO Progression", TournamentFormat.KNOCKOUT, 4);
        List<Match> allMatches = matches.findByTournamentId(tournamentId);
        Match semiFinal1 = allMatches.get(0);
        Match finalMatch = allMatches.get(2);

        resultService.recordResult(semiFinal1.id(), 3, 0);

        Match updatedFinal = matches.findById(finalMatch.id()).orElseThrow();
        assertEquals(semiFinal1.homeTeamId(), updatedFinal.homeTeamId());
    }

    @Test
    void knockoutProgressionMakesPendingMatchScheduledWhenBothTeamsPresent() {
        long tournamentId = createGeneratedTournament("KO Both", TournamentFormat.KNOCKOUT, 4);
        List<Match> allMatches = matches.findByTournamentId(tournamentId);
        Match semiFinal1 = allMatches.get(0);
        Match semiFinal2 = allMatches.get(1);

        resultService.recordResult(semiFinal1.id(), 2, 0);
        Match afterFirst = matches.findById(allMatches.get(2).id()).orElseThrow();
        assertEquals(MatchStatus.PENDING, afterFirst.status());

        resultService.recordResult(semiFinal2.id(), 1, 3);
        Match afterBoth = matches.findById(allMatches.get(2).id()).orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, afterBoth.status());
        assertNotNull(afterBoth.homeTeamId());
        assertNotNull(afterBoth.awayTeamId());
    }

    @Test
    void undoRestoresMatchToPreviousState() {
        long tournamentId = createGeneratedTournament("Undo RR", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        resultService.recordResult(firstMatch.id(), 2, 1);
        assertEquals(MatchStatus.COMPLETED, matches.findById(firstMatch.id()).orElseThrow().status());

        resultService.undoLastResult();
        Match restored = matches.findById(firstMatch.id()).orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, restored.status());
        assertNull(restored.homeScore());
        assertNull(restored.awayScore());
    }

    @Test
    void undoKnockoutResultRestoresProgressionInNextMatch() {
        long tournamentId = createGeneratedTournament("Undo KO", TournamentFormat.KNOCKOUT, 4);
        List<Match> allMatches = matches.findByTournamentId(tournamentId);
        Match semiFinal1 = allMatches.get(0);
        Match finalMatch = allMatches.get(2);

        resultService.recordResult(semiFinal1.id(), 3, 1);
        Match progressedFinal = matches.findById(finalMatch.id()).orElseThrow();
        assertEquals(semiFinal1.homeTeamId(), progressedFinal.homeTeamId());

        resultService.undoLastResult();
        Match restoredFinal = matches.findById(finalMatch.id()).orElseThrow();
        assertNull(restoredFinal.homeTeamId());
        assertNull(restoredFinal.awayTeamId());
        assertEquals(MatchStatus.PENDING, restoredFinal.status());
    }

    @Test
    void correctionOverwritesPreviousResult() {
        long tournamentId = createGeneratedTournament("Correct RR", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        resultService.recordResult(firstMatch.id(), 2, 1);
        resultService.correctResult(firstMatch.id(), 0, 3);

        Match corrected = matches.findById(firstMatch.id()).orElseThrow();
        assertEquals(0, corrected.homeScore());
        assertEquals(3, corrected.awayScore());
        assertTrue(resultService.canUndo());
    }

    @Test
    void correctionIsRejectedAfterTheNextKnockoutMatchIsCompleted() {
        long tournamentId = createGeneratedTournament("Protected KO", TournamentFormat.KNOCKOUT, 4);
        List<Match> tournamentMatches = matches.findByTournamentId(tournamentId);
        Match firstSemiFinal = tournamentMatches.get(0);
        Match secondSemiFinal = tournamentMatches.get(1);
        Match finalMatch = tournamentMatches.get(2);

        resultService.recordResult(firstSemiFinal.id(), 3, 0);
        resultService.recordResult(secondSemiFinal.id(), 2, 0);
        resultService.recordResult(finalMatch.id(), 1, 0);

        Match semiFinalBeforeCorrection = matches.findById(firstSemiFinal.id()).orElseThrow();
        Match finalBeforeCorrection = matches.findById(finalMatch.id()).orElseThrow();

        assertThrows(
                BusinessRuleException.class,
                () -> resultService.correctResult(firstSemiFinal.id(), 0, 3)
        );

        assertEquals(semiFinalBeforeCorrection, matches.findById(firstSemiFinal.id()).orElseThrow());
        assertEquals(finalBeforeCorrection, matches.findById(finalMatch.id()).orElseThrow());
        assertEquals(3, resultService.commandHistory().size());
    }

    @Test
    void undoAfterCorrectionRestoresCorrectedNotOriginal() {
        long tournamentId = createGeneratedTournament("Undo Correct", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        resultService.recordResult(firstMatch.id(), 2, 1);
        resultService.correctResult(firstMatch.id(), 0, 3);
        resultService.undoLastResult();

        Match afterUndo = matches.findById(firstMatch.id()).orElseThrow();
        assertEquals(2, afterUndo.homeScore());
        assertEquals(1, afterUndo.awayScore());
    }

    @Test
    void undoOnEmptySessionHistoryThrows() {
        assertThrows(BusinessRuleException.class, () -> resultService.undoLastResult());
    }

    @Test
    void failedCommandDoesNotEnterHistory() {
        long tournamentId = createGeneratedTournament("Failed KO", TournamentFormat.KNOCKOUT, 4);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        try {
            resultService.recordResult(firstMatch.id(), 2, 2);
        } catch (BusinessRuleException ignored) {
        }

        assertFalse(resultService.canUndo());
    }

    @Test
    void pendingMatchCannotHaveResultRecorded() {
        long tournamentId = createGeneratedTournament("Pending KO", TournamentFormat.KNOCKOUT, 4);
        List<Match> allMatches = matches.findByTournamentId(tournamentId);
        Match pendingFinal = allMatches.get(2);

        assertThrows(BusinessRuleException.class, () -> resultService.recordResult(pendingFinal.id(), 1, 0));
    }

    @Test
    void rejectsNegativeScoresBeforeExecution() {
        assertThrows(ValidationException.class, () -> resultService.recordResult(1, -1, 0));
    }

    @Test
    void multipleUndosWorkInReverseOrder() {
        long tournamentId = createGeneratedTournament("Multi Undo", TournamentFormat.ROUND_ROBIN, 3);
        List<Match> allMatches = matches.findByTournamentId(tournamentId);
        Match first = allMatches.get(0);
        Match second = allMatches.get(1);

        resultService.recordResult(first.id(), 1, 0);
        resultService.recordResult(second.id(), 2, 2);

        resultService.undoLastResult();
        Match secondAfterUndo = matches.findById(second.id()).orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, secondAfterUndo.status());
        Match firstStillCompleted = matches.findById(first.id()).orElseThrow();
        assertEquals(MatchStatus.COMPLETED, firstStillCompleted.status());

        resultService.undoLastResult();
        Match firstAfterUndo = matches.findById(first.id()).orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, firstAfterUndo.status());

        assertFalse(resultService.canUndo());
    }

    @Test
    void resultPersistsAcrossNewConnection() {
        long tournamentId = createGeneratedTournament("Persist", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        resultService.recordResult(firstMatch.id(), 4, 2);

        MatchRepository freshRepo = new JdbcMatchRepository(databaseManager);
        Match persisted = freshRepo.findById(firstMatch.id()).orElseThrow();
        assertEquals(MatchStatus.COMPLETED, persisted.status());
        assertEquals(4, persisted.homeScore());
        assertEquals(2, persisted.awayScore());
    }

    @Test
    void zeroZeroDrawIsAllowedInRoundRobin() {
        long tournamentId = createGeneratedTournament("Zero Draw", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        MatchDto result = resultService.recordResult(firstMatch.id(), 0, 0);

        assertEquals(0, result.homeScore());
        assertEquals(0, result.awayScore());
        assertEquals(MatchStatus.COMPLETED, result.status());
    }

    @Test
    void failedUndoStaysInHistory() throws Exception {
        long tournamentId = createGeneratedTournament("Failed Undo", TournamentFormat.ROUND_ROBIN, 3);
        Match firstMatch = matches.findByTournamentId(tournamentId).getFirst();

        resultService.recordResult(firstMatch.id(), 2, 1);
        assertTrue(resultService.canUndo());

        try (java.sql.Connection c = databaseManager.openConnection();
             java.sql.PreparedStatement s = c.prepareStatement("DELETE FROM matches WHERE id = ?")) {
            s.setLong(1, firstMatch.id());
            s.executeUpdate();
        }

        assertThrows(RuntimeException.class, () -> resultService.undoLastResult());

        assertTrue(resultService.canUndo());
        assertEquals(1, resultService.commandHistory().size());
    }

    @Test
    void databaseFailureDuringProgressionRollsBackAndKeepsHistoryClean() throws Exception {
        long tournamentId = createGeneratedTournament("Progression Fail", TournamentFormat.KNOCKOUT, 4);
        List<Match> allMatches = matches.findByTournamentId(tournamentId);
        Match semiFinal = allMatches.get(0);
        Match finalMatch = allMatches.get(2);

        try (java.sql.Connection c = databaseManager.openConnection();
             java.sql.Statement s = c.createStatement()) {
            s.execute("CREATE TRIGGER sabotage BEFORE UPDATE OF home_team_id, away_team_id ON matches WHEN NEW.id = " + finalMatch.id() + " BEGIN SELECT RAISE(ABORT, 'sabotage'); END;");
        }

        assertThrows(RuntimeException.class, () -> resultService.recordResult(semiFinal.id(), 2, 1));

        Match semiFinalAfterFail = matches.findById(semiFinal.id()).orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, semiFinalAfterFail.status());
        assertNull(semiFinalAfterFail.homeScore());

        Match finalMatchAfterFail = matches.findById(finalMatch.id()).orElseThrow();
        assertEquals(MatchStatus.PENDING, finalMatchAfterFail.status());
        assertEquals(finalMatch.homeTeamId(), finalMatchAfterFail.homeTeamId());
        assertEquals(finalMatch.awayTeamId(), finalMatchAfterFail.awayTeamId());

        assertFalse(resultService.canUndo());
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
