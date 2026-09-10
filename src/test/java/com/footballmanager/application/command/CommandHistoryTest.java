package com.footballmanager.application.command;

import com.footballmanager.application.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHistoryTest {

    @Test
    void newHistoryIsEmpty() {
        CommandHistory<TestCommand> history = new CommandHistory<>();

        assertFalse(history.hasHistory());
        assertEquals(0, history.size());
    }

    @Test
    void undoOnEmptyHistoryThrows() {
        CommandHistory<TestCommand> history = new CommandHistory<>();

        assertThrows(BusinessRuleException.class, history::undoLast);
    }

    @Test
    void rejectsNullCommand() {
        CommandHistory<TestCommand> history = new CommandHistory<>();

        assertThrows(BusinessRuleException.class, () -> history.push(null));
    }

    @Test
    void storesAndUndoesThroughTheCommandAbstraction() {
        CommandHistory<TestCommand> history = new CommandHistory<>();
        TestCommand command = new TestCommand();

        command.execute();
        history.push(command);
        TestCommand undone = history.undoLast();

        assertSame(command, undone);
        assertFalse(command.isExecuted());
        assertFalse(history.hasHistory());
    }

    private static final class TestCommand implements UndoableCommand {
        private boolean executed;

        @Override
        public void execute() {
            executed = true;
        }

        @Override
        public void undo() {
            executed = false;
        }

        @Override
        public boolean isExecuted() {
            return executed;
        }
    }
}
