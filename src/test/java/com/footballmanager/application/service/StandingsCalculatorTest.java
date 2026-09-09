package com.footballmanager.application.service;

import com.footballmanager.application.dto.StandingsRowDto;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsCalculatorTest {

    private Team createTeam(long id, String name) {
        return new Team(id, name, "T" + id, null, null);
    }

    private Match createMatch(long id, long homeTeamId, long awayTeamId, Integer homeScore, Integer awayScore, MatchStatus status) {
        return new Match(id, 1L, 1, homeTeamId, awayTeamId, homeScore, awayScore, status, null, null, null);
    }

    @Test
    void orderingByPoints() {
        Team t1 = createTeam(1L, "Team A");
        Team t2 = createTeam(2L, "Team B");
        
        List<Match> matches = List.of(
                createMatch(1L, 1L, 2L, 2, 1, MatchStatus.COMPLETED)
        );
        Map<Long, Team> teams = Map.of(1L, t1, 2L, t2);

        List<StandingsRowDto> standings = StandingsCalculator.calculate(matches, teams);
        
        assertEquals(2, standings.size());
        assertEquals("Team A", standings.get(0).teamName());
        assertEquals(3, standings.get(0).points());
        assertEquals("Team B", standings.get(1).teamName());
        assertEquals(0, standings.get(1).points());
    }

    @Test
    void orderingByGoalDifference() {
        Team t1 = createTeam(1L, "Team A");
        Team t2 = createTeam(2L, "Team B");
        Team t3 = createTeam(3L, "Team C");
        
        List<Match> matches = List.of(
                createMatch(1L, 1L, 3L, 2, 0, MatchStatus.COMPLETED), // A gets 3 pts, GD +2
                createMatch(2L, 2L, 3L, 1, 0, MatchStatus.COMPLETED)  // B gets 3 pts, GD +1
        );
        Map<Long, Team> teams = Map.of(1L, t1, 2L, t2, 3L, t3);

        List<StandingsRowDto> standings = StandingsCalculator.calculate(matches, teams);
        
        assertEquals("Team A", standings.get(0).teamName()); // GD +2
        assertEquals("Team B", standings.get(1).teamName()); // GD +1
    }

    @Test
    void orderingByGoalsFor() {
        Team t1 = createTeam(1L, "Team A");
        Team t2 = createTeam(2L, "Team B");
        Team t3 = createTeam(3L, "Team C");
        
        List<Match> matches = List.of(
                createMatch(1L, 1L, 3L, 3, 1, MatchStatus.COMPLETED), // A gets 3 pts, GD +2, GF 3
                createMatch(2L, 2L, 3L, 2, 0, MatchStatus.COMPLETED)  // B gets 3 pts, GD +2, GF 2
        );
        Map<Long, Team> teams = Map.of(1L, t1, 2L, t2, 3L, t3);

        List<StandingsRowDto> standings = StandingsCalculator.calculate(matches, teams);
        
        assertEquals("Team A", standings.get(0).teamName()); // GF 3
        assertEquals("Team B", standings.get(1).teamName()); // GF 2
    }

    @Test
    void orderingByCaseInsensitiveTeamName() {
        Team t1 = createTeam(1L, "team z");
        Team t2 = createTeam(2L, "Team A");
        
        List<Match> matches = List.of(
                createMatch(1L, 1L, 2L, 1, 1, MatchStatus.COMPLETED) // Draw, same GD, GF
        );
        Map<Long, Team> teams = Map.of(1L, t1, 2L, t2);

        List<StandingsRowDto> standings = StandingsCalculator.calculate(matches, teams);
        
        assertEquals("Team A", standings.get(0).teamName());
        assertEquals("team z", standings.get(1).teamName());
    }

    @Test
    void emptyStandings() {
        List<StandingsRowDto> standings = StandingsCalculator.calculate(List.of(), Map.of());
        assertTrue(standings.isEmpty());
    }

    @Test
    void incompleteMatchesAreIgnored() {
        Team t1 = createTeam(1L, "Team A");
        Team t2 = createTeam(2L, "Team B");
        
        List<Match> matches = List.of(
                createMatch(1L, 1L, 2L, null, null, MatchStatus.SCHEDULED)
        );
        Map<Long, Team> teams = Map.of(1L, t1, 2L, t2);

        List<StandingsRowDto> standings = StandingsCalculator.calculate(matches, teams);
        
        assertEquals(2, standings.size());
        assertEquals(0, standings.get(0).played());
        assertEquals(0, standings.get(1).played());
    }
}
