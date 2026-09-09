package com.footballmanager.application.service;

import com.footballmanager.application.dto.StandingsRowDto;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.Team;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StandingsCalculator {
    private StandingsCalculator() {
    }

    public static List<StandingsRowDto> calculate(List<Match> matches, Map<Long, Team> teams) {
        Map<Long, StandingsAccumulator> accumulators = new HashMap<>();
        for (Long teamId : teams.keySet()) {
            accumulators.put(teamId, new StandingsAccumulator(teams.get(teamId)));
        }

        for (Match match : matches) {
            if (match.status() == MatchStatus.COMPLETED && match.homeTeamId() != null && match.awayTeamId() != null) {
                accumulators.get(match.homeTeamId()).addMatch(match.homeScore(), match.awayScore());
                accumulators.get(match.awayTeamId()).addMatch(match.awayScore(), match.homeScore());
            }
        }

        return accumulators.values().stream()
                .map(StandingsAccumulator::toDto)
                .sorted(Comparator.comparing(StandingsRowDto::points).reversed()
                        .thenComparing(Comparator.comparing(StandingsRowDto::goalDifference).reversed())
                        .thenComparing(Comparator.comparing(StandingsRowDto::goalsFor).reversed())
                        .thenComparing(dto -> dto.teamName().toLowerCase()))
                .toList();
    }

    private static class StandingsAccumulator {
        private final Team team;
        private int played = 0;
        private int won = 0;
        private int drawn = 0;
        private int lost = 0;
        private int goalsFor = 0;
        private int goalsAgainst = 0;

        public StandingsAccumulator(Team team) {
            this.team = team;
        }

        public void addMatch(int goalsScored, int goalsConceded) {
            played++;
            goalsFor += goalsScored;
            goalsAgainst += goalsConceded;
            if (goalsScored > goalsConceded) {
                won++;
            } else if (goalsScored == goalsConceded) {
                drawn++;
            } else {
                lost++;
            }
        }

        public StandingsRowDto toDto() {
            int goalDifference = goalsFor - goalsAgainst;
            int points = (won * 3) + (drawn * 1);
            return new StandingsRowDto(
                    team.id(), team.name(), played, won, drawn, lost,
                    goalsFor, goalsAgainst, goalDifference, points
            );
        }
    }
}
