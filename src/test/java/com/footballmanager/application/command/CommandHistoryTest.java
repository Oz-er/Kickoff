package com.footballmanager.application.command;

import com.footballmanager.application.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHistoryTest {

    @Test
    void newHistoryIsEmpty() {
        CommandHistory history = new CommandHistory();

        assertFalse(history.hasHistory());
        assertEquals(0, history.size());
    }

    @Test
    void undoOnEmptyHistoryThrows() {
        CommandHistory history = new CommandHistory();

        assertThrows(BusinessRuleException.class, history::undoLast);
    }

    @Test
    void rejectsNullCommand() {
        CommandHistory history = new CommandHistory();

        assertThrows(BusinessRuleException.class, () -> history.push(null));
    }
}
