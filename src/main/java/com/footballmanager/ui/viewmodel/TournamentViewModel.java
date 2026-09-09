package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.RegisteredTeamDto;
import com.footballmanager.application.dto.SchedulePreviewDto;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.dto.UpdateTournamentRequest;
import com.footballmanager.application.scheduling.SchedulingService;
import com.footballmanager.application.service.TeamService;
import com.footballmanager.application.service.TournamentLifecycleService;
import com.footballmanager.application.service.TournamentRegistrationService;
import com.footballmanager.application.service.TournamentService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TournamentViewModel {
    private final TournamentService tournamentService;
    private final TournamentRegistrationService registrationService;
    private final TournamentLifecycleService lifecycleService;
    private final SchedulingService schedulingService;
    private final TeamService teamService;

    public TournamentViewModel(
            TournamentService tournamentService,
            TournamentRegistrationService registrationService,
            TournamentLifecycleService lifecycleService,
            SchedulingService schedulingService,
            TeamService teamService
    ) {
        this.tournamentService = Objects.requireNonNull(tournamentService);
        this.registrationService = Objects.requireNonNull(registrationService);
        this.lifecycleService = Objects.requireNonNull(lifecycleService);
        this.schedulingService = Objects.requireNonNull(schedulingService);
        this.teamService = Objects.requireNonNull(teamService);
    }

    public List<TournamentDto> loadTournaments() {
        return tournamentService.findAll();
    }

    public TournamentDto create(CreateTournamentRequest request) {
        return tournamentService.create(request);
    }

    public TournamentDto update(long id, UpdateTournamentRequest request) {
        return tournamentService.update(id, request);
    }

    public void delete(long id) {
        tournamentService.delete(id);
    }

    public List<RegisteredTeamDto> loadRegisteredTeams(long tournamentId) {
        return registrationService.findRegisteredTeams(tournamentId);
    }

    public List<TeamDto> loadAvailableTeams(long tournamentId) {
        List<RegisteredTeamDto> registered = registrationService.findRegisteredTeams(tournamentId);
        Set<Long> registeredIds = registered.stream()
                .map(RegisteredTeamDto::teamId)
                .collect(Collectors.toSet());
                
        return teamService.findAll().stream()
                .filter(t -> !registeredIds.contains(t.id()))
                .toList();
    }

    public void registerTeams(long tournamentId, List<TournamentRegistrationRequest> requests) {
        registrationService.registerTeams(tournamentId, requests);
    }

    public void unregisterTeam(long tournamentId, long teamId) {
        registrationService.unregisterTeam(tournamentId, teamId);
    }

    public TournamentDto closeRegistration(long tournamentId) {
        return lifecycleService.closeRegistration(tournamentId);
    }

    public SchedulePreviewDto previewSchedule(long tournamentId) {
        return schedulingService.previewSchedule(tournamentId);
    }

    public void generateSchedule(SchedulePreviewDto preview) {
        schedulingService.generateSchedule(preview);
    }

    public TournamentDto completeTournament(long tournamentId) {
        return lifecycleService.complete(tournamentId);
    }
}
