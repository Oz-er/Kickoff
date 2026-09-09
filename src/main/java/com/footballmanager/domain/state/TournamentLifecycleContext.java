package com.footballmanager.domain.state;

import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.TournamentFormat;

public record TournamentLifecycleContext(
        TournamentFormat format,
        int registeredTeamCount,
        int totalFixtureCount,
        int incompleteFixtureCount
) {
    public TournamentLifecycleContext {
        if (format == null) {
            throw new ValidationException("Tournament format is required");
        }
        if (registeredTeamCount < 0 || totalFixtureCount < 0 || incompleteFixtureCount < 0) {
            throw new ValidationException("Lifecycle counts cannot be negative");
        }
        if (incompleteFixtureCount > totalFixtureCount) {
            throw new ValidationException("Incomplete fixtures cannot exceed total fixtures");
        }
    }
}
