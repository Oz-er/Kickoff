package com.footballmanager.domain.scheduling;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentRegistration;

import java.util.ArrayList;
import java.util.List;

public final class RoundRobinScheduleStrategy implements ScheduleStrategy {
    @Override
    public TournamentFormat format() {
        return TournamentFormat.ROUND_ROBIN;
    }

    @Override
    public List<FixtureDraft> generate(long tournamentId, List<TournamentRegistration> registrations) {
        List<TournamentRegistration> ordered = ScheduleInput.validateAndSort(tournamentId, registrations);
        if (ordered.size() < 3) {
            throw new BusinessRuleException("Round-robin scheduling requires at least 3 teams");
        }

        List<Long> rotation = new ArrayList<>(ordered.stream().map(TournamentRegistration::teamId).toList());
        if (rotation.size() % 2 != 0) {
            rotation.add(null);
        }

        List<FixtureDraft> fixtures = new ArrayList<>();
        int fixtureNumber = 1;
        int teamSlots = rotation.size();
        for (int round = 1; round < teamSlots; round++) {
            for (int pair = 0; pair < teamSlots / 2; pair++) {
                Long first = rotation.get(pair);
                Long second = rotation.get(teamSlots - 1 - pair);
                if (first != null && second != null) {
                    Long home = round % 2 == 1 ? first : second;
                    Long away = round % 2 == 1 ? second : first;
                    fixtures.add(new FixtureDraft(
                            fixtureNumber++,
                            tournamentId,
                            round,
                            home,
                            away,
                            MatchStatus.SCHEDULED,
                            null,
                            null
                    ));
                }
            }
            Long last = rotation.removeLast();
            rotation.add(1, last);
        }
        return List.copyOf(fixtures);
    }
}
