package com.footballmanager.domain.scheduling;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.NextMatchSlot;
import com.footballmanager.domain.model.TournamentRegistration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnockoutScheduleStrategyTest {
    private final KnockoutScheduleStrategy strategy = new KnockoutScheduleStrategy();

    @Test
    void createsSeededFourTeamBracketAndFinalLinks() {
        List<FixtureDraft> fixtures = strategy.generate(1, registrations(4));

        assertEquals(3, fixtures.size());
        assertPair(fixtures.get(0), 1, 4);
        assertPair(fixtures.get(1), 2, 3);
        assertEquals(3, fixtures.get(0).nextFixtureNumber());
        assertEquals(NextMatchSlot.HOME, fixtures.get(0).nextMatchSlot());
        assertEquals(3, fixtures.get(1).nextFixtureNumber());
        assertEquals(NextMatchSlot.AWAY, fixtures.get(1).nextMatchSlot());
        assertEquals(MatchStatus.PENDING, fixtures.get(2).status());
        assertNull(fixtures.get(2).nextFixtureNumber());
    }

    @Test
    void createsStandardEightTeamBracketWithPendingFutureRounds() {
        List<FixtureDraft> fixtures = strategy.generate(1, registrations(8));

        assertEquals(7, fixtures.size());
        assertPair(fixtures.get(0), 1, 8);
        assertPair(fixtures.get(1), 4, 5);
        assertPair(fixtures.get(2), 2, 7);
        assertPair(fixtures.get(3), 3, 6);
        assertEquals(4, fixtures.stream().filter(fixture -> fixture.status() == MatchStatus.SCHEDULED).count());
        assertEquals(3, fixtures.stream().filter(fixture -> fixture.status() == MatchStatus.PENDING).count());
        assertEquals(5, fixtures.get(0).nextFixtureNumber());
        assertEquals(5, fixtures.get(1).nextFixtureNumber());
        assertEquals(6, fixtures.get(2).nextFixtureNumber());
        assertEquals(6, fixtures.get(3).nextFixtureNumber());
        assertEquals(7, fixtures.get(4).nextFixtureNumber());
        assertEquals(7, fixtures.get(5).nextFixtureNumber());
    }

    @Test
    void sortsShuffledRegistrationsBySeed() {
        List<TournamentRegistration> shuffled = new ArrayList<>(registrations(4));
        TournamentRegistration first = shuffled.removeFirst();
        shuffled.add(first);

        assertEquals(strategy.generate(1, registrations(4)), strategy.generate(1, shuffled));
    }

    @Test
    void rejectsUnsupportedTeamCountsAndDuplicateSeeds() {
        assertThrows(BusinessRuleException.class, () -> strategy.generate(1, registrations(3)));
        assertThrows(BusinessRuleException.class, () -> strategy.generate(1, registrations(6)));
        assertThrows(BusinessRuleException.class, () -> strategy.generate(1, List.of(
                new TournamentRegistration(1, 1, 1),
                new TournamentRegistration(1, 2, 1),
                new TournamentRegistration(1, 3, 3),
                new TournamentRegistration(1, 4, 4)
        )));
    }

    private void assertPair(FixtureDraft fixture, long homeTeamId, long awayTeamId) {
        assertEquals(homeTeamId, fixture.homeTeamId());
        assertEquals(awayTeamId, fixture.awayTeamId());
    }

    private List<TournamentRegistration> registrations(int count) {
        List<TournamentRegistration> registrations = new ArrayList<>();
        for (int seed = 1; seed <= count; seed++) {
            registrations.add(new TournamentRegistration(1, seed, seed));
        }
        return registrations;
    }
}
