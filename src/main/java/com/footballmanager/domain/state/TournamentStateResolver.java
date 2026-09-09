package com.footballmanager.domain.state;

import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.TournamentStatus;

import java.util.EnumMap;
import java.util.Map;

public final class TournamentStateResolver {
    private final Map<TournamentStatus, TournamentState> states;

    public TournamentStateResolver() {
        states = new EnumMap<>(TournamentStatus.class);
        register(new DraftTournamentState());
        register(new RegistrationClosedTournamentState());
        register(new FixturesGeneratedTournamentState());
        register(new CompletedTournamentState());
    }

    public TournamentState resolve(TournamentStatus status) {
        if (status == null) {
            throw new ValidationException("Tournament status is required");
        }
        TournamentState state = states.get(status);
        if (state == null) {
            throw new ValidationException("No behavior is registered for status " + status);
        }
        return state;
    }

    private void register(TournamentState state) {
        states.put(state.status(), state);
    }
}
