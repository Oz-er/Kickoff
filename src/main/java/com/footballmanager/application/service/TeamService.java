package com.footballmanager.application.service;

import com.footballmanager.application.dto.SaveTeamRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Team;
import com.footballmanager.persistence.repository.TeamRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TeamService {
    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = Objects.requireNonNull(teamRepository);
    }

    public TeamDto create(SaveTeamRequest request) {
        requireRequest(request);
        return toDto(teamRepository.create(Team.create(request.name(), request.shortCode(), request.coachName())));
    }

    public TeamDto update(long teamId, SaveTeamRequest request) {
        requireRequest(request);
        Team current = requireTeam(teamId);
        Team updated = new Team(
                current.id(),
                request.name(),
                request.shortCode(),
                request.coachName(),
                current.createdAt()
        );
        return toDto(teamRepository.update(updated));
    }

    public Optional<TeamDto> findById(long teamId) {
        requirePositiveId(teamId);
        return teamRepository.findById(teamId).map(this::toDto);
    }

    public List<TeamDto> findAll() {
        return teamRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<TeamDto> searchByName(String query) {
        return teamRepository.searchByName(query).stream().map(this::toDto).toList();
    }

    public void delete(long teamId) {
        requireTeam(teamId);
        teamRepository.deleteById(teamId);
    }

    private Team requireTeam(long teamId) {
        requirePositiveId(teamId);
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + " was not found"));
    }

    private void requireRequest(SaveTeamRequest request) {
        if (request == null) {
            throw new ValidationException("Team details are required");
        }
    }

    private void requirePositiveId(long teamId) {
        if (teamId <= 0) {
            throw new ValidationException("Team id must be positive");
        }
    }

    private TeamDto toDto(Team team) {
        return new TeamDto(team.id(), team.name(), team.shortCode(), team.coachName());
    }
}
