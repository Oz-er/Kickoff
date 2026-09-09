package com.footballmanager.ui.dialog;

import com.footballmanager.application.exception.ApplicationException;

public final class UserMessageMapper {
    public String messageFor(Throwable exception) {
        if (exception instanceof ApplicationException
                && exception.getMessage() != null
                && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return "The operation could not be completed. Please try again.";
    }
}
