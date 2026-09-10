package com.footballmanager.application.command;

public interface UndoableCommand {
    void execute();

    void undo();

    boolean isExecuted();
}
