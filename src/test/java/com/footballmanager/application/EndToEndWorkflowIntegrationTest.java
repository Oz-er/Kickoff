package com.footballmanager.application;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.SchedulePreviewDto;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.config.ApplicationConfig;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndWorkflowIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundRobinWorkflowCompletesAndPersistsAfterRestart() {
        Path databasePath = temporaryDirectory.resolve("round_robin.db");
        ApplicationContext context = startApplication(databasePath);
        TournamentDto tournament = context.tournamentService().create(new CreateTournamentRequest(
                "Release League",
                TournamentFormat.ROUND_ROBIN,
                LocalDate.of(2027, 4, 10)
        ));

        assertThrows(
                BusinessRuleException.class,
                () -> context.tournamentLifecycleService().closeRegistration(tournament.id())
        );
        assertEquals(
                TournamentStatus.DRAFT,
                context.tournamentService().findById(tournament.id()).orElseThrow().status()
        );

        registerSeededTeams(context, tournament.id(), 3);
        assertEquals(
                TournamentStatus.REGISTRATION_CLOSED,
                context.tournamentLifecycleService().closeRegistration(tournament.id()).status()
        );

        SchedulePreviewDto preview = context.schedulingService().previewSchedule(tournament.id());
        assertEquals(3, preview.fixtures().size());
        List<MatchDto> fixtures = context.schedulingService().generateSchedule(preview);
        assertEquals(3, fixtures.size());

        for (int index = 0; index < fixtures.size(); index++) {
            context.resultService().recordResult(fixtures.get(index).id(), index + 1, index);
        }

        assertEquals(3, context.reportService().generateStandings(tournament.id()).size());
        assertTrue(context.reportService().generateStandings(tournament.id()).stream()
                .allMatch(row -> row.played() == 2));
        assertEquals(
                TournamentStatus.COMPLETED,
                context.tournamentLifecycleService().complete(tournament.id()).status()
        );

        ApplicationContext restarted = startApplication(databasePath);
        assertEquals(
                TournamentStatus.COMPLETED,
                restarted.tournamentService().findById(tournament.id()).orElseThrow().status()
        );
        assertEquals(3, restarted.reportService().getTournamentMatches(tournament.id()).size());
        assertTrue(restarted.reportService().getTournamentMatches(tournament.id()).stream()
                .allMatch(match -> match.status() == MatchStatus.COMPLETED));
        assertTrue(restarted.reportService().generateTournamentSummary(tournament.id())
                .contains("Status: COMPLETED"));
    }

    @Test
    void knockoutWorkflowProgressesCompletesAndPersistsAfterRestart() {
        Path databasePath = temporaryDirectory.resolve("knockout.db");
        ApplicationContext context = startApplication(databasePath);
        TournamentDto tournament = context.tournamentService().create(new CreateTournamentRequest(
                "Release Cup",
                TournamentFormat.KNOCKOUT,
                LocalDate.of(2027, 4, 12)
        ));

        registerSeededTeams(context, tournament.id(), 4);
        context.tournamentLifecycleService().closeRegistration(tournament.id());
        SchedulePreviewDto preview = context.schedulingService().previewSchedule(tournament.id());
        assertEquals(3, preview.fixtures().size());
        context.schedulingService().generateSchedule(preview);

        List<MatchDto> firstRound = context.schedulingService().findMatches(tournament.id()).stream()
                .filter(match -> match.roundNumber() == 1)
                .toList();
        assertEquals(2, firstRound.size());
        context.resultService().recordResult(firstRound.get(0).id(), 2, 0);
        context.resultService().recordResult(firstRound.get(1).id(), 0, 1);

        MatchDto finalMatch = context.schedulingService().findMatches(tournament.id()).stream()
                .filter(match -> match.roundNumber() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals(MatchStatus.SCHEDULED, finalMatch.status());
        assertNotNull(finalMatch.homeTeamId());
        assertNotNull(finalMatch.awayTeamId());
        assertEquals(
                Set.of(firstRound.get(0).homeTeamId(), firstRound.get(1).awayTeamId()),
                Set.of(finalMatch.homeTeamId(), finalMatch.awayTeamId())
        );

        context.resultService().recordResult(finalMatch.id(), 3, 1);
        assertEquals(
                TournamentStatus.COMPLETED,
                context.tournamentLifecycleService().complete(tournament.id()).status()
        );

        ApplicationContext restarted = startApplication(databasePath);
        List<MatchDto> persistedMatches = restarted.reportService().getTournamentMatches(tournament.id());
        assertEquals(3, persistedMatches.size());
        assertTrue(persistedMatches.stream().allMatch(match -> match.status() == MatchStatus.COMPLETED));
        assertEquals(
                TournamentStatus.COMPLETED,
                restarted.tournamentService().findById(tournament.id()).orElseThrow().status()
        );
    }

    private ApplicationContext startApplication(Path databasePath) {
        ApplicationContext context = new ApplicationContext(new ApplicationConfig(databasePath));
        context.initialize();
        return context;
    }

    private void registerSeededTeams(ApplicationContext context, long tournamentId, int teamCount) {
        List<TeamDto> teams = context.teamService().findAll();
        List<TournamentRegistrationRequest> registrations = new ArrayList<>();
        for (int index = 0; index < teamCount; index++) {
            registrations.add(new TournamentRegistrationRequest(teams.get(index).id(), index + 1));
        }
        context.tournamentRegistrationService().registerTeams(tournamentId, registrations);
    }
}
