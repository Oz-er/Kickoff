package com.footballmanager.application.command;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RecordMatchResultCommandTest {

    @Test
    void rejectsNegativeMatchId() {
        assertThrows(ValidationException.class, () -> new RecordMatchResultCommand(
                -1, 1, 0, null, null
        ));
    }

    @Test
    void rejectsZeroMatchId() {
        assertThrows(ValidationException.class, () -> new RecordMatchResultCommand(
                0, 1, 0, null, null
        ));
    }

    @Test
    void rejectsNegativeHomeScore() {
        assertThrows(ValidationException.class, () -> new RecordMatchResultCommand(
                1, -1, 0, null, null
        ));
    }

    @Test
    void rejectsNegativeAwayScore() {
        assertThrows(ValidationException.class, () -> new RecordMatchResultCommand(
                1, 0, -2, null, null
        ));
    }

    @Test
    void rejectsNullMatchRepository() {
        assertThrows(NullPointerException.class, () -> new RecordMatchResultCommand(
                1, 1, 0, null, null
        ));
    }
}
