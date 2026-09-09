package com.footballmanager.application.command;

import com.footballmanager.application.exception.BusinessRuleException;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandHistory {
    private final Deque<RecordMatchResultCommand> history = new ArrayDeque<>();

    public void push(RecordMatchResultCommand command) {
        if (command == null || !command.isExecuted()) {
            throw new BusinessRuleException("Only successfully executed commands can be stored");
        }
        history.push(command);
    }

    public RecordMatchResultCommand undoLast() {
        if (history.isEmpty()) {
            throw new BusinessRuleException("There are no results to undo in this session");
        }
        RecordMatchResultCommand command = history.peek();
        command.undo();
        history.pop();
        return command;
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public int size() {
        return history.size();
    }
}
