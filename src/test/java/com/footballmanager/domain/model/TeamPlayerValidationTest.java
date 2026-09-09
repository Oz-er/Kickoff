package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamPlayerValidationTest {
    @Test
    void normalizesTeamAndOptionalValues() {
        Team team = Team.create("  Dhaka Eagles  ", " de ", "  Amina Khan  ");

        assertEquals("Dhaka Eagles", team.name());
        assertEquals("DE", team.shortCode());
        assertEquals("Amina Khan", team.coachName());
        assertNull(Team.create("Team", "TM", " ").coachName());
    }

    @Test
    void rejectsInvalidTeamFields() {
        assertThrows(ValidationException.class, () -> Team.create(" ", "ABC", null));
        assertThrows(ValidationException.class, () -> Team.create("Team", "A", null));
        assertThrows(ValidationException.class, () -> Team.create("Team", "ABCDEF", null));
    }

    @Test
    void normalizesPlayerAndRejectsInvalidShirtNumbers() {
        Player player = Player.create(1, "  Jamil Ahmed ", 12, "  Defender ");

        assertEquals("Jamil Ahmed", player.fullName());
        assertEquals("Defender", player.position());
        assertThrows(ValidationException.class, () -> Player.create(1, "Player", 0, null));
        assertThrows(ValidationException.class, () -> Player.create(1, "Player", 100, null));
    }
}
