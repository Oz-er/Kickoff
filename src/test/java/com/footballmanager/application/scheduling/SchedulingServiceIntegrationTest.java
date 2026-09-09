package com.footballmanager.application.scheduling;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.SchedulePreviewDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.exception.DatabaseException;
import com.footballmanager.application.service.TournamentLifecycleService;
import com.footballmanager.application.service.TournamentRegistrationService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.NextMatchSlot;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentRegistration;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.domain.scheduling.KnockoutScheduleStrategy;
import com.footballmanager.domain.scheduling.RoundRobinScheduleStrategy;
import com.footballmanager.domain.scheduling.ScheduleStrategy;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingServiceIntegrationTest {
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
    private int teamSequence;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("scheduling.db"));
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
                tournaments,
                registrations,
                teams,
                matches,
                resolver,
                List.of(new RoundRobinScheduleStrategy(), new KnockoutScheduleStrategy())
        );
        teamSequence = 0;
    }

    @Test
    void previewsWithoutSavingOrChangingTournamentStatus() {
        TournamentDto tournament = createClosedTournament("Preview Cup", TournamentFormat.ROUND_ROBIN, 4);

        SchedulePreviewDto preview = schedulingService.previewSchedule(tournament.id());

        assertEquals(6, preview.fixtures().size());
        assertTrue(matches.findByTournamentId(tournament.id()).isEmpty());
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, tournaments.findById(tournament.id()).orElseThrow().status());
    }

    @Test
    void savesRoundRobinPairingsAndStateAtomically() {
        TournamentDto tournament = createClosedTournament("Round Robin Schedule", TournamentFormat.ROUND_ROBIN, 4);
        SchedulePreviewDto preview = schedulingService.previewSchedule(tournament.id());

        List<MatchDto> saved = schedulingService.generateSchedule(preview);

        assertEquals(6, saved.size());
        Set<String> pairings = new HashSet<>();
        for (MatchDto match : saved) {
            long first = Math.min(match.homeTeamId(), match.awayTeamId());
            long second = Math.max(match.homeTeamId(), match.awayTeamId());
            assertTrue(pairings.add(first + "-" + second));
        }
        assertEquals(6, pairings.size());
        assertEquals(TournamentStatus.FIXTURES_GENERATED, tournaments.findById(tournament.id()).orElseThrow().status());
        assertEquals(6, new JdbcMatchRepository(databaseManager).findByTournamentId(tournament.id()).size());
    }

    @Test
    void savesEightTeamKnockoutWinnerLinks() {
        TournamentDto tournament = createClosedTournament("Knockout Schedule", TournamentFormat.KNOCKOUT, 8);

        List<MatchDto> saved = schedulingService.generateSchedule(schedulingService.previewSchedule(tournament.id()));
        List<Match> persisted = matches.findByTournamentId(tournament.id());

        assertEquals(7, saved.size());
        assertEquals(4, persisted.stream().filter(match -> match.status() == MatchStatus.SCHEDULED).count());
        assertEquals(3, persisted.stream().filter(match -> match.status() == MatchStatus.PENDING).count());
        Match firstSemiFinal = persisted.get(4);
        Match secondSemiFinal = persisted.get(5);
        Match finalMatch = persisted.get(6);
        assertLink(persisted.get(0), firstSemiFinal.id(), NextMatchSlot.HOME);
        assertLink(persisted.get(1), firstSemiFinal.id(), NextMatchSlot.AWAY);
        assertLink(persisted.get(2), secondSemiFinal.id(), NextMatchSlot.HOME);
        assertLink(persisted.get(3), secondSemiFinal.id(), NextMatchSlot.AWAY);
        assertLink(firstSemiFinal, finalMatch.id(), NextMatchSlot.HOME);
        assertLink(secondSemiFinal, finalMatch.id(), NextMatchSlot.AWAY);
    }

    @Test
    void preventsDuplicateGenerationAndRejectsStalePreviews() {
        TournamentDto generatedTournament = createClosedTournament("Generated Cup", TournamentFormat.KNOCKOUT, 4);
        SchedulePreviewDto generatedPreview = schedulingService.previewSchedule(generatedTournament.id());
        schedulingService.generateSchedule(generatedPreview);

        assertThrows(BusinessRuleException.class, () -> schedulingService.generateSchedule(generatedPreview));
        assertThrows(BusinessRuleException.class, () -> schedulingService.previewSchedule(generatedTournament.id()));

        TournamentDto staleTournament = createClosedTournament("Stale Cup", TournamentFormat.KNOCKOUT, 4);
        SchedulePreviewDto preview = schedulingService.previewSchedule(staleTournament.id());
        SchedulePreviewDto altered = new SchedulePreviewDto(
                preview.tournamentId(),
                "Altered name",
                preview.format(),
                preview.fixtures()
        );
        assertThrows(BusinessRuleException.class, () -> schedulingService.generateSchedule(altered));
        assertTrue(matches.findByTournamentId(staleTournament.id()).isEmpty());
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, tournaments.findById(staleTournament.id()).orElseThrow().status());
    }

    @Test
    void rollsBackAllMatchesWhenAForeignKeyFails() {
        TournamentDto tournament = createClosedTournament("Foreign Key Rollback", TournamentFormat.KNOCKOUT, 4);
        List<TournamentRegistration> registered = registrations.findByTournamentId(tournament.id());
        List<FixtureDraft> fixtures = new ArrayList<>(new KnockoutScheduleStrategy().generate(tournament.id(), registered));
        FixtureDraft original = fixtures.get(1);
        fixtures.set(1, new FixtureDraft(
                original.fixtureNumber(),
                original.tournamentId(),
                original.roundNumber(),
                original.homeTeamId(),
                999_999L,
                original.status(),
                original.nextFixtureNumber(),
                original.nextMatchSlot()
        ));

        assertThrows(DataIntegrityException.class, () -> matches.saveScheduleAndTransition(
                tournament.id(),
                TournamentStatus.REGISTRATION_CLOSED,
                TournamentStatus.FIXTURES_GENERATED,
                fixtures
        ));
        assertTrue(matches.findByTournamentId(tournament.id()).isEmpty());
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, tournaments.findById(tournament.id()).orElseThrow().status());
    }

    @Test
    void rollsBackMatchesWhenTheExpectedStatusChanged() {
        TournamentDto tournament = createClosedTournament("Status Rollback", TournamentFormat.KNOCKOUT, 4);
        List<FixtureDraft> fixtures = new KnockoutScheduleStrategy().generate(
                tournament.id(),
                registrations.findByTournamentId(tournament.id())
        );

        assertThrows(DatabaseException.class, () -> matches.saveScheduleAndTransition(
                tournament.id(),
                TournamentStatus.DRAFT,
                TournamentStatus.FIXTURES_GENERATED,
                fixtures
        ));
        assertTrue(matches.findByTournamentId(tournament.id()).isEmpty());
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, tournaments.findById(tournament.id()).orElseThrow().status());
    }

    private TournamentDto createClosedTournament(
            String name,
            TournamentFormat format,
            int teamCount
    ) {
        TournamentDto tournament = tournamentService.create(new CreateTournamentRequest(
                name,
                format,
                LocalDate.of(2027, 1, 10)
        ));
        List<TournamentRegistrationRequest> requests = new ArrayList<>();
        for (int seed = 1; seed <= teamCount; seed++) {
            String code = String.format("S%03d", ++teamSequence);
            Team team = teams.create(Team.create(name + " Team " + seed, code, null));
            requests.add(new TournamentRegistrationRequest(team.id(), seed));
        }
        registrationService.registerTeams(tournament.id(), requests);
        return lifecycleService.closeRegistration(tournament.id());
    }

    private void assertLink(Match match, long nextMatchId, NextMatchSlot slot) {
        assertEquals(nextMatchId, match.nextMatchId());
        assertEquals(slot, match.nextMatchSlot());
    }
}
