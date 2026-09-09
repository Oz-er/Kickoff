package com.footballmanager.domain.scheduling;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.TournamentRegistration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundRobinScheduleStrategyTest {
    private final RoundRobinScheduleStrategy strategy = new RoundRobinScheduleStrategy();

    @Test
    void createsEveryPairOnceForEvenAndOddTeamCounts() {
        assertUniquePairings(4, 6);
        assertUniquePairings(5, 10);
    }

    @Test
    void givesEveryTeamOneMatchAgainstEveryOpponent() {
        List<FixtureDraft> fixtures = strategy.generate(1, registrations(1, 3));

        assertEquals(3, fixtures.size());
        for (long teamId = 1; teamId <= 3; teamId++) {
            long currentTeamId = teamId;
            long appearances = fixtures.stream()
                    .filter(fixture -> fixture.homeTeamId() == currentTeamId || fixture.awayTeamId() == currentTeamId)
                    .count();
            assertEquals(2, appearances);
        }
    }

    @Test
    void usesSeedOrderDeterministically() {
        List<TournamentRegistration> shuffled = new ArrayList<>(registrations(1, 4));
        TournamentRegistration first = shuffled.removeFirst();
        shuffled.add(first);

        assertEquals(strategy.generate(1, registrations(1, 4)), strategy.generate(1, shuffled));
    }

    @Test
    void rejectsTooFewOrDuplicateRegistrations() {
        assertThrows(BusinessRuleException.class, () -> strategy.generate(1, registrations(1, 2)));
        assertThrows(BusinessRuleException.class, () -> strategy.generate(1, List.of(
                new TournamentRegistration(1, 10, 1),
                new TournamentRegistration(1, 10, 2),
                new TournamentRegistration(1, 11, 3)
        )));
        assertThrows(BusinessRuleException.class, () -> strategy.generate(1, List.of(
                new TournamentRegistration(1, 10, 1),
                new TournamentRegistration(1, 11, 1),
                new TournamentRegistration(1, 12, 2)
        )));
    }

    private void assertUniquePairings(int teamCount, int expectedFixtures) {
        List<FixtureDraft> fixtures = strategy.generate(1, registrations(1, teamCount));
        Set<String> pairings = new HashSet<>();
        for (FixtureDraft fixture : fixtures) {
            long first = Math.min(fixture.homeTeamId(), fixture.awayTeamId());
            long second = Math.max(fixture.homeTeamId(), fixture.awayTeamId());
            assertTrue(pairings.add(first + "-" + second));
        }
        assertEquals(expectedFixtures, fixtures.size());
        assertEquals(expectedFixtures, pairings.size());
    }

    private List<TournamentRegistration> registrations(long tournamentId, int count) {
        List<TournamentRegistration> registrations = new ArrayList<>();
        for (int seed = 1; seed <= count; seed++) {
            registrations.add(new TournamentRegistration(tournamentId, seed, seed));
        }
        return registrations;
    }
}
