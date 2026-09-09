package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.SaveTeamRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.service.TeamService;

import java.util.List;
import java.util.Objects;

public final class TeamViewModel {
    private final TeamService teamService;
    private String query = "";
    private List<TeamDto> teams = List.of();

    public TeamViewModel(TeamService teamService) {
        this.teamService = Objects.requireNonNull(teamService);
    }

    public List<TeamDto> search(String value) {
        query = value == null ? "" : value.trim();
        teams = query.isEmpty() ? teamService.findAll() : teamService.searchByName(query);
        return teams;
    }

    public TeamDto save(Long teamId, SaveTeamRequest request) {
        TeamDto saved = teamId == null ? teamService.create(request) : teamService.update(teamId, request);
        search(query);
        return saved;
    }

    public void delete(long teamId) {
        teamService.delete(teamId);
        search(query);
    }

    public List<TeamDto> teams() {
        return teams;
    }
}
