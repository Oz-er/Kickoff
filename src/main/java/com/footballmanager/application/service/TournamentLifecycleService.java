package com.footballmanager.application.service;

import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.FixtureProgress;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.domain.state.TournamentLifecycleContext;
import com.footballmanager.domain.state.TournamentState;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.repository.MatchProgressRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.Objects;

public final class TournamentLifecycleService {
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final MatchProgressRepository matchProgressRepository;
    private final TournamentStateResolver stateResolver;

    public TournamentLifecycleService(
            TournamentRepository tournamentRepository,
            TournamentRegistrationRepository registrationRepository,
            MatchProgressRepository matchProgressRepository,
            TournamentStateResolver stateResolver
    ) {
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
        this.registrationRepository = Objects.requireNonNull(registrationRepository);
        this.matchProgressRepository = Objects.requireNonNull(matchProgressRepository);
        this.stateResolver = Objects.requireNonNull(stateResolver);
    }

    public TournamentDto closeRegistration(long tournamentId) {
        Tournament tournament = requireTournament(tournamentId);
        TournamentState state = stateResolver.resolve(tournament.status());
        TournamentStatus nextStatus = state.closeRegistration(contextFor(tournament));
        return persistStatus(tournament, nextStatus);
    }

    public TournamentDto markFixturesGenerated(long tournamentId) {
        Tournament tournament = requireTournament(tournamentId);
        TournamentState state = stateResolver.resolve(tournament.status());
        TournamentStatus nextStatus = state.markFixturesGenerated(contextFor(tournament));
        return persistStatus(tournament, nextStatus);
    }

    public TournamentDto complete(long tournamentId) {
        Tournament tournament = requireTournament(tournamentId);
        TournamentState state = stateResolver.resolve(tournament.status());
        TournamentStatus nextStatus = state.complete(contextFor(tournament));
        return persistStatus(tournament, nextStatus);
    }

    private TournamentLifecycleContext contextFor(Tournament tournament) {
        int registeredTeams = registrationRepository.findByTournamentId(tournament.id()).size();
        FixtureProgress fixtureProgress = matchProgressRepository.findForTournament(tournament.id());
        return new TournamentLifecycleContext(
                tournament.format(),
                registeredTeams,
                fixtureProgress.totalFixtures(),
                fixtureProgress.incompleteFixtures()
        );
    }

    private Tournament requireTournament(long tournamentId) {
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament id must be a positive number");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament " + tournamentId + " was not found"));
    }

    private TournamentDto persistStatus(Tournament tournament, TournamentStatus status) {
        Tournament updated = tournamentRepository.update(new Tournament(
                tournament.id(),
                tournament.name(),
                tournament.format(),
                status,
                tournament.startDate(),
                tournament.createdAt()
        ));
        return new TournamentDto(
                updated.id(),
                updated.name(),
                updated.format(),
                updated.status(),
                updated.startDate(),
                updated.createdAt()
        );
    }
}
