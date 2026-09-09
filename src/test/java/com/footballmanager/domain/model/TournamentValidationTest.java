package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TournamentValidationTest {
    @Test
    void createsANormalizedDraftTournament() {
        Tournament tournament = Tournament.create("  Dhaka Cup  ", TournamentFormat.ROUND_ROBIN, LocalDate.of(2026, 10, 5));

        assertNull(tournament.id());
        assertEquals("Dhaka Cup", tournament.name());
        assertEquals(TournamentStatus.DRAFT, tournament.status());
    }

    @Test
    void rejectsMissingTournamentFields() {
        LocalDate date = LocalDate.of(2026, 10, 5);

        assertThrows(ValidationException.class, () -> Tournament.create(" ", TournamentFormat.KNOCKOUT, date));
        assertThrows(ValidationException.class, () -> Tournament.create("Cup", null, date));
        assertThrows(ValidationException.class, () -> Tournament.create("Cup", TournamentFormat.KNOCKOUT, null));
    }

    @Test
    void rejectsInvalidRegistrationIdentifiersAndSeeds() {
        assertThrows(ValidationException.class, () -> new TournamentRegistration(0, 1, 1));
        assertThrows(ValidationException.class, () -> new TournamentRegistration(1, 0, 1));
        assertThrows(ValidationException.class, () -> new TournamentRegistration(1, 1, 0));
    }
}
