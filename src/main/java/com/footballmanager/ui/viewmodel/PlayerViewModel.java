package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.PlayerDto;
import com.footballmanager.application.dto.SavePlayerRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.service.PlayerService;
import com.footballmanager.application.service.TeamService;

import java.util.List;
import java.util.Objects;

public final class PlayerViewModel {
    private final PlayerService playerService;
    private final TeamService teamService;
    private Long teamId;
    private String query = "";
    private List<PlayerDto> players = List.of();

    public PlayerViewModel(PlayerService playerService, TeamService teamService) {
        this.playerService = Objects.requireNonNull(playerService);
        this.teamService = Objects.requireNonNull(teamService);
    }

    public List<TeamDto> availableTeams() {
        return teamService.findAll();
    }

    public List<PlayerDto> filter(Long selectedTeamId, String value) {
        teamId = selectedTeamId;
        query = value == null ? "" : value.trim();
        if (teamId == null && query.isEmpty()) {
            players = playerService.findAll();
        } else if (teamId == null) {
            players = playerService.searchByName(query);
        } else if (query.isEmpty()) {
            players = playerService.findByTeam(teamId);
        } else {
            players = playerService.searchByTeamAndName(teamId, query);
        }
        return players;
    }

    public PlayerDto save(Long playerId, SavePlayerRequest request) {
        PlayerDto saved = playerId == null ? playerService.create(request) : playerService.update(playerId, request);
        filter(teamId, query);
        return saved;
    }

    public void delete(long playerId) {
        playerService.delete(playerId);
        filter(teamId, query);
    }

    public List<PlayerDto> players() {
        return players;
    }
}
