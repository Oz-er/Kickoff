package com.footballmanager.application.service;

import com.footballmanager.application.dto.RegisteredTeamDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.model.TournamentRegistration;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TournamentRegistrationService {
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final TournamentStateResolver stateResolver;

    public TournamentRegistrationService(
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            TournamentRegistrationRepository registrationRepository
    ) {
        this(tournamentRepository, teamRepository, registrationRepository, new TournamentStateResolver());
    }

    public TournamentRegistrationService(
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            TournamentRegistrationRepository registrationRepository,
            TournamentStateResolver stateResolver
    ) {
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
        this.teamRepository = Objects.requireNonNull(teamRepository);
        this.registrationRepository = Objects.requireNonNull(registrationRepository);
        this.stateResolver = Objects.requireNonNull(stateResolver);
    }

    public List<RegisteredTeamDto> registerTeams(long tournamentId, List<TournamentRegistrationRequest> requests) {
        Tournament tournament = requireTournament(tournamentId);
        stateResolver.resolve(tournament.status()).ensureCanChangeRegistration();
        if (requests == null || requests.isEmpty()) {
            throw new ValidationException("At least one team registration is required");
        }

        List<TournamentRegistration> existing = registrationRepository.findByTournamentId(tournamentId);
        Set<Long> teamIds = new HashSet<>();
        Set<Integer> seeds = new HashSet<>();
        for (TournamentRegistration registration : existing) {
            teamIds.add(registration.teamId());
            seeds.add(registration.seedNumber());
        }

        List<TournamentRegistration> registrations = new ArrayList<>();
        for (TournamentRegistrationRequest request : requests) {
            if (request == null) {
                throw new ValidationException("Registration details are required");
            }
            TournamentRegistration registration = new TournamentRegistration(
                    tournamentId,
                    request.teamId(),
                    request.seedNumber()
            );
            if (!teamIds.add(registration.teamId())) {
                throw new BusinessRuleException("A team cannot be registered twice in the same tournament");
            }
            if (!seeds.add(registration.seedNumber())) {
                throw new BusinessRuleException("Seed numbers must be unique within a tournament");
            }
            requireTeam(registration.teamId());
            registrations.add(registration);
        }

        registrationRepository.addAll(registrations);
        return findRegisteredTeams(tournamentId);
    }

    public List<RegisteredTeamDto> findRegisteredTeams(long tournamentId) {
        requireTournament(tournamentId);
        return registrationRepository.findByTournamentId(tournamentId).stream()
                .map(this::toDto)
                .toList();
    }

    public void unregisterTeam(long tournamentId, long teamId) {
        Tournament tournament = requireTournament(tournamentId);
        stateResolver.resolve(tournament.status()).ensureCanChangeRegistration();
        requireTeam(teamId);
        registrationRepository.remove(tournamentId, teamId);
    }

    private Tournament requireTournament(long tournamentId) {
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament id must be a positive number");
        }
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament " + tournamentId + " was not found"));
    }

    private Team requireTeam(long teamId) {
        if (teamId <= 0) {
            throw new ValidationException("Team id must be a positive number");
        }
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " was not found"));
    }

    private RegisteredTeamDto toDto(TournamentRegistration registration) {
        Team team = requireTeam(registration.teamId());
        return new RegisteredTeamDto(team.id(), team.name(), team.shortCode(), registration.seedNumber());
    }
}
