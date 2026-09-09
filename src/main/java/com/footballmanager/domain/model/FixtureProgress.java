package com.footballmanager.domain.model;

import com.footballmanager.application.exception.ValidationException;

public record FixtureProgress(int totalFixtures, int incompleteFixtures) {
    public FixtureProgress {
        if (totalFixtures < 0 || incompleteFixtures < 0) {
            throw new ValidationException("Fixture counts cannot be negative");
        }
        if (incompleteFixtures > totalFixtures) {
            throw new ValidationException("Incomplete fixtures cannot exceed total fixtures");
        }
    }
}
