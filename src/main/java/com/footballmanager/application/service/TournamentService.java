package com.footballmanager.application.service;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.UpdateTournamentRequest;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TournamentStateResolver stateResolver;

    public TournamentService(TournamentRepository tournamentRepository) {
        this(tournamentRepository, new TournamentStateResolver());
    }

    public TournamentService(TournamentRepository tournamentRepository, TournamentStateResolver stateResolver) {
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
        this.stateResolver = Objects.requireNonNull(stateResolver);
    }

    public TournamentDto create(CreateTournamentRequest request) {
        if (request == null) {
            throw new ValidationException("Tournament details are required");
        }
        Tournament tournament = Tournament.create(request.name(), request.format(), request.startDate());
        return toDto(tournamentRepository.create(tournament));
    }

    public Optional<TournamentDto> findById(long id) {
        requirePositiveId(id);
        return tournamentRepository.findById(id).map(this::toDto);
    }

    public List<TournamentDto> findAll() {
        return tournamentRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<TournamentDto> searchByName(String query) {
        return tournamentRepository.searchByName(query).stream().map(this::toDto).toList();
    }

    public TournamentDto update(long id, UpdateTournamentRequest request) {
        requirePositiveId(id);
        if (request == null) {
            throw new ValidationException("Tournament details are required");
        }
        Tournament current = requireTournament(id);
        stateResolver.resolve(current.status()).ensureCanEdit();
        Tournament updated = new Tournament(
                current.id(),
                request.name(),
                request.format(),
                current.status(),
                request.startDate(),
                current.createdAt()
        );
        return toDto(tournamentRepository.update(updated));
    }

    public void delete(long id) {
        requirePositiveId(id);
        Tournament current = requireTournament(id);
        stateResolver.resolve(current.status()).ensureCanDelete();
        tournamentRepository.deleteById(id);
    }

    Tournament requireTournament(long id) {
        requirePositiveId(id);
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tournament " + id + " was not found"));
    }

    private void requirePositiveId(long id) {
        if (id <= 0) {
            throw new ValidationException("Tournament id must be a positive number");
        }
    }

    private TournamentDto toDto(Tournament tournament) {
        return new TournamentDto(
                tournament.id(),
                tournament.name(),
                tournament.format(),
                tournament.status(),
                tournament.startDate(),
                tournament.createdAt()
        );
    }
}
