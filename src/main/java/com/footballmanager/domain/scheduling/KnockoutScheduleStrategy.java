package com.footballmanager.domain.scheduling;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.NextMatchSlot;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentRegistration;

import java.util.ArrayList;
import java.util.List;

public final class KnockoutScheduleStrategy implements ScheduleStrategy {
    @Override
    public TournamentFormat format() {
        return TournamentFormat.KNOCKOUT;
    }

    @Override
    public List<FixtureDraft> generate(long tournamentId, List<TournamentRegistration> registrations) {
        List<TournamentRegistration> ordered = ScheduleInput.validateAndSort(tournamentId, registrations);
        if (ordered.size() != 4 && ordered.size() != 8) {
            throw new BusinessRuleException("Knockout scheduling requires exactly 4 or 8 uniquely seeded teams");
        }
        if (ordered.size() == 4) {
            return fourTeamSchedule(tournamentId, ordered);
        }
        return eightTeamSchedule(tournamentId, ordered);
    }

    private List<FixtureDraft> fourTeamSchedule(long tournamentId, List<TournamentRegistration> teams) {
        List<FixtureDraft> fixtures = new ArrayList<>();
        fixtures.add(scheduled(1, tournamentId, 1, teamId(teams, 1), teamId(teams, 4), 3, NextMatchSlot.HOME));
        fixtures.add(scheduled(2, tournamentId, 1, teamId(teams, 2), teamId(teams, 3), 3, NextMatchSlot.AWAY));
        fixtures.add(pending(3, tournamentId, 2, null, null));
        return List.copyOf(fixtures);
    }

    private List<FixtureDraft> eightTeamSchedule(long tournamentId, List<TournamentRegistration> teams) {
        List<FixtureDraft> fixtures = new ArrayList<>();
        fixtures.add(scheduled(1, tournamentId, 1, teamId(teams, 1), teamId(teams, 8), 5, NextMatchSlot.HOME));
        fixtures.add(scheduled(2, tournamentId, 1, teamId(teams, 4), teamId(teams, 5), 5, NextMatchSlot.AWAY));
        fixtures.add(scheduled(3, tournamentId, 1, teamId(teams, 2), teamId(teams, 7), 6, NextMatchSlot.HOME));
        fixtures.add(scheduled(4, tournamentId, 1, teamId(teams, 3), teamId(teams, 6), 6, NextMatchSlot.AWAY));
        fixtures.add(pending(5, tournamentId, 2, 7, NextMatchSlot.HOME));
        fixtures.add(pending(6, tournamentId, 2, 7, NextMatchSlot.AWAY));
        fixtures.add(pending(7, tournamentId, 3, null, null));
        return List.copyOf(fixtures);
    }

    private FixtureDraft scheduled(
            int fixtureNumber,
            long tournamentId,
            int round,
            long homeTeamId,
            long awayTeamId,
            int nextFixture,
            NextMatchSlot slot
    ) {
        return new FixtureDraft(
                fixtureNumber,
                tournamentId,
                round,
                homeTeamId,
                awayTeamId,
                MatchStatus.SCHEDULED,
                nextFixture,
                slot
        );
    }

    private FixtureDraft pending(
            int fixtureNumber,
            long tournamentId,
            int round,
            Integer nextFixture,
            NextMatchSlot slot
    ) {
        return new FixtureDraft(
                fixtureNumber,
                tournamentId,
                round,
                null,
                null,
                MatchStatus.PENDING,
                nextFixture,
                slot
        );
    }

    private long teamId(List<TournamentRegistration> teams, int seedPosition) {
        return teams.get(seedPosition - 1).teamId();
    }
}
