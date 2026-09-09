package com.footballmanager.application.service;

import com.footballmanager.application.dto.DashboardSummaryDto;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.StandingsRowDto;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.Team;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReportService {
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;

    public ReportService(
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            TournamentRepository tournamentRepository
    ) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.teamRepository = Objects.requireNonNull(teamRepository);
        this.playerRepository = Objects.requireNonNull(playerRepository);
        this.tournamentRepository = Objects.requireNonNull(tournamentRepository);
    }

    public List<StandingsRowDto> generateStandings(long tournamentId) {
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament ID must be positive");
        }
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));
                
        if (tournament.format() == com.footballmanager.domain.model.TournamentFormat.KNOCKOUT) {
            throw new BusinessRuleException("Standings are not available for knockout tournaments");
        }
        
        List<Match> matches = matchRepository.findByTournamentId(tournamentId);
        
        Map<Long, Team> teamMap = new HashMap<>();
        for (Match m : matches) {
            if (m.homeTeamId() != null && !teamMap.containsKey(m.homeTeamId())) {
                teamMap.put(m.homeTeamId(), teamRepository.findById(m.homeTeamId()).orElseThrow());
            }
            if (m.awayTeamId() != null && !teamMap.containsKey(m.awayTeamId())) {
                teamMap.put(m.awayTeamId(), teamRepository.findById(m.awayTeamId()).orElseThrow());
            }
        }
        
        return StandingsCalculator.calculate(matches, teamMap);
    }

    public DashboardSummaryDto getDashboardSummary(Long tournamentId, int upcomingLimit) {
        if (tournamentId != null && tournamentId <= 0) {
            throw new ValidationException("Tournament ID must be positive");
        }
        if (upcomingLimit <= 0) {
            throw new ValidationException("Limit must be positive");
        }
        long totalTeams = teamRepository.count();
        long totalPlayers = playerRepository.count();
        long totalTournaments = tournamentRepository.count();
        
        List<MatchDto> upcoming = matchRepository.findUpcoming(tournamentId, upcomingLimit).stream()
                .map(this::toMatchDto)
                .toList();
                
        return new DashboardSummaryDto(totalTeams, totalPlayers, totalTournaments, upcoming);
    }

    public String generateTournamentSummary(long tournamentId) {
        if (tournamentId <= 0) {
            throw new ValidationException("Tournament ID must be positive");
        }
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));
        long matchCount = matchRepository.findByTournamentId(tournamentId).size();
        
        return String.format(
            "Tournament: %s\nFormat: %s\nStatus: %s\nStart Date: %s\nTotal Matches: %d",
            tournament.name(),
            tournament.format(),
            tournament.status(),
            tournament.startDate(),
            matchCount
        );
    }

    private MatchDto toMatchDto(Match match) {
        String homeTeam = match.homeTeamId() != null ? teamRepository.findById(match.homeTeamId()).map(Team::name).orElse("TBD") : "TBD";
        String awayTeam = match.awayTeamId() != null ? teamRepository.findById(match.awayTeamId()).map(Team::name).orElse("TBD") : "TBD";
        return new MatchDto(match.id(), match.tournamentId(), match.roundNumber(),
                match.homeTeamId(), homeTeam, match.awayTeamId(), awayTeam,
                match.homeScore(), match.awayScore(), match.status(), match.scheduledAt(),
                match.nextMatchId(), match.nextMatchSlot());
    }
}
