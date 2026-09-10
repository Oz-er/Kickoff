package com.footballmanager.application.command;

import com.footballmanager.application.exception.BusinessRuleException;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandHistory<T extends UndoableCommand> {
    private final Deque<T> history = new ArrayDeque<>();

    public void push(T command) {
        if (command == null || !command.isExecuted()) {
            throw new BusinessRuleException("Only successfully executed commands can be stored");
        }
        history.push(command);
    }

    public T undoLast() {
        if (history.isEmpty()) {
            throw new BusinessRuleException("There are no results to undo in this session");
        }
        T command = history.peek();
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
