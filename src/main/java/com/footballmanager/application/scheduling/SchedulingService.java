package com.footballmanager.application.scheduling;

import com.footballmanager.application.dto.FixturePreviewDto;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.SchedulePreviewDto;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.ConfigurationException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentRegistration;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.domain.scheduling.ScheduleStrategy;
import com.footballmanager.domain.state.TournamentLifecycleContext;
import com.footballmanager.domain.state.TournamentState;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SchedulingService {
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final TournamentStateResolver stateResolver;
    private final Map<TournamentFormat, ScheduleStrategy> strategies;

    public SchedulingService(
            TournamentRepository tournamentRepository,
            TournamentRegistrationRepository registrationRepository,
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            TournamentStateResolver stateResolver,
            List<ScheduleStrategy> strategies
    ) {
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
        this.registrationRepository = Objects.requireNonNull(registrationRepository);
        this.teamRepository = Objects.requireNonNull(teamRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.stateResolver = Objects.requireNonNull(stateResolver);
        this.strategies = registerStrategies(strategies);
    }

    public SchedulePreviewDto previewSchedule(long tournamentId) {
        return toPreview(prepareSchedule(tournamentId));
    }

    public List<MatchDto> generateSchedule(SchedulePreviewDto approvedPreview) {
        if (approvedPreview == null) {
            throw new ValidationException("An approved schedule preview is required");
        }
        PreparedSchedule prepared = prepareSchedule(approvedPreview.tournamentId());
        SchedulePreviewDto currentPreview = toPreview(prepared);
        if (!currentPreview.equals(approvedPreview)) {
            throw new BusinessRuleException("The schedule preview is no longer current; preview it again before saving");
        }

        TournamentState state = stateResolver.resolve(prepared.tournament().status());
        TournamentLifecycleContext context = new TournamentLifecycleContext(
                prepared.tournament().format(),
                prepared.registrations().size(),
                prepared.fixtures().size(),
                prepared.fixtures().size()
        );
        TournamentStatus nextStatus = state.markFixturesGenerated(context);
        matchRepository.saveScheduleAndTransition(
                prepared.tournament().id(),
                prepared.tournament().status(),
                nextStatus,
                prepared.fixtures()
        );
        return matchRepository.findByTournamentId(prepared.tournament().id()).stream()
                .map(this::toMatchDto)
                .toList();
    }

    public List<MatchDto> findMatches(long tournamentId) {
        requireTournament(tournamentId);
        return matchRepository.findByTournamentId(tournamentId).stream().map(this::toMatchDto).toList();
    }

    private PreparedSchedule prepareSchedule(long tournamentId) {
        Tournament tournament = requireTournament(tournamentId);
        stateResolver.resolve(tournament.status()).ensureCanGenerateFixtures();
        if (matchRepository.existsForTournament(tournamentId)) {
            throw new BusinessRuleException("Fixtures have already been generated for this tournament");
        }
        List<TournamentRegistration> registrations = registrationRepository.findByTournamentId(tournamentId);
        ScheduleStrategy strategy = strategies.get(tournament.format());
        if (strategy == null) {
            throw new ConfigurationException("No scheduling strategy is registered for " + tournament.format());
        }
        List<FixtureDraft> fixtures = strategy.generate(tournamentId, registrations);
        return new PreparedSchedule(tournament, registrations, fixtures);
    }

    private SchedulePreviewDto toPreview(PreparedSchedule prepared) {
        List<FixturePreviewDto> fixtures = prepared.fixtures().stream()
                .map(this::toPreviewFixture)
                .toList();
        return new SchedulePreviewDto(
                prepared.tournament().id(),
                prepared.tournament().name(),
                prepared.tournament().format(),
                fixtures
        );
    }

    private FixturePreviewDto toPreviewFixture(FixtureDraft fixture) {
        return new FixturePreviewDto(
                fixture.fixtureNumber(),
                fixture.roundNumber(),
                fixture.homeTeamId(),
                teamName(fixture.homeTeamId()),
                fixture.awayTeamId(),
                teamName(fixture.awayTeamId()),
                fixture.status(),
                fixture.nextFixtureNumber(),
                fixture.nextMatchSlot()
        );
    }

    private MatchDto toMatchDto(Match match) {
        return new MatchDto(
                match.id(),
                match.tournamentId(),
                match.roundNumber(),
                match.homeTeamId(),
                teamName(match.homeTeamId()),
                match.awayTeamId(),
                teamName(match.awayTeamId()),
                match.homeScore(),
                match.awayScore(),
                match.status(),
                match.scheduledAt(),
                match.nextMatchId(),
                match.nextMatchSlot()
        );
    }

    private Tournament requireTournament(long tournamentId) {
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament id must be positive");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament " + tournamentId + " was not found"));
    }

    private String teamName(Long teamId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " was not found"));
        return team.name();
    }

    private Map<TournamentFormat, ScheduleStrategy> registerStrategies(List<ScheduleStrategy> strategyList) {
        if (strategyList == null || strategyList.isEmpty()) {
            throw new ConfigurationException("At least one scheduling strategy is required");
        }
        Map<TournamentFormat, ScheduleStrategy> registered = new EnumMap<>(TournamentFormat.class);
        for (ScheduleStrategy strategy : strategyList) {
            if (strategy == null) {
                throw new ConfigurationException("Scheduling strategies cannot contain null values");
            }
            if (strategy.format() == null) {
                throw new ConfigurationException("Every scheduling strategy must declare a format");
            }
            if (registered.putIfAbsent(strategy.format(), strategy) != null) {
                throw new ConfigurationException("Only one scheduling strategy can be registered for " + strategy.format());
            }
        }
        return Map.copyOf(registered);
    }

    private record PreparedSchedule(
            Tournament tournament,
            List<TournamentRegistration> registrations,
            List<FixtureDraft> fixtures
    ) {
        private PreparedSchedule {
            registrations = List.copyOf(registrations);
            fixtures = List.copyOf(fixtures);
        }
    }
}
