package com.footballmanager.application.service;

import com.footballmanager.application.dto.PlayerDto;
import com.footballmanager.application.dto.SavePlayerRequest;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Player;
import com.footballmanager.domain.model.Team;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.TeamRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PlayerService {
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public PlayerService(PlayerRepository playerRepository, TeamRepository teamRepository) {
        this.playerRepository = Objects.requireNonNull(playerRepository);
        this.teamRepository = Objects.requireNonNull(teamRepository);
    }

    public PlayerDto create(SavePlayerRequest request) {
        requireRequest(request);
        requireTeam(request.teamId());
        Player player = Player.create(request.teamId(), request.fullName(), request.shirtNumber(), request.position());
        return toDto(playerRepository.create(player));
    }

    public PlayerDto update(long playerId, SavePlayerRequest request) {
        requireRequest(request);
        requireTeam(request.teamId());
        Player current = requirePlayer(playerId);
        Player updated = new Player(
                current.id(),
                request.teamId(),
                request.fullName(),
                request.shirtNumber(),
                request.position(),
                current.createdAt()
        );
        return toDto(playerRepository.update(updated));
    }

    public Optional<PlayerDto> findById(long playerId) {
        requirePositiveId(playerId, "Player id");
        return playerRepository.findById(playerId).map(this::toDto);
    }

    public List<PlayerDto> findAll() {
        return playerRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<PlayerDto> findByTeam(long teamId) {
        requireTeam(teamId);
        return playerRepository.findByTeamId(teamId).stream().map(this::toDto).toList();
    }

    public List<PlayerDto> searchByName(String query) {
        return playerRepository.searchByName(query).stream().map(this::toDto).toList();
    }

    public List<PlayerDto> searchByTeamAndName(long teamId, String query) {
        requireTeam(teamId);
        return playerRepository.searchByTeamIdAndName(teamId, query).stream().map(this::toDto).toList();
    }

    public void delete(long playerId) {
        requirePlayer(playerId);
        playerRepository.deleteById(playerId);
    }

    private Player requirePlayer(long playerId) {
        requirePositiveId(playerId, "Player id");
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player " + playerId + " was not found"));
    }

    private Team requireTeam(long teamId) {
        requirePositiveId(teamId, "Team id");
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " was not found"));
    }

    private void requireRequest(SavePlayerRequest request) {
        if (request == null) {
            throw new ValidationException("Player details are required");
        }
    }

    private void requirePositiveId(long id, String fieldName) {
        if (id <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }

    private PlayerDto toDto(Player player) {
        Team team = requireTeam(player.teamId());
        return new PlayerDto(
                player.id(),
                player.teamId(),
                team.name(),
                player.fullName(),
                player.shirtNumber(),
                player.position()
        );
    }
}
