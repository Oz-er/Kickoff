package com.footballmanager.domain.state;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.TournamentStatus;

abstract class AbstractTournamentState implements TournamentState {
    private final TournamentStatus status;

    AbstractTournamentState(TournamentStatus status) {
        this.status = status;
    }

    @Override
    public TournamentStatus status() {
        return status;
    }

    @Override
    public void ensureCanEdit() {
        reject("edited");
    }

    @Override
    public void ensureCanDelete() {
        reject("deleted");
    }

    @Override
    public void ensureCanChangeRegistration() {
        reject("have its registrations changed");
    }

    @Override
    public void ensureCanGenerateFixtures() {
        reject("generate fixtures");
    }

    @Override
    public TournamentStatus closeRegistration(TournamentLifecycleContext context) {
        throw invalidTransition("close registration");
    }

    @Override
    public TournamentStatus markFixturesGenerated(TournamentLifecycleContext context) {
        throw invalidTransition("mark fixtures as generated");
    }

    @Override
    public TournamentStatus complete(TournamentLifecycleContext context) {
        throw invalidTransition("be completed");
    }

    private void reject(String action) {
        throw new BusinessRuleException("A " + displayStatus() + " tournament cannot be " + action);
    }

    private BusinessRuleException invalidTransition(String action) {
        return new BusinessRuleException("A " + displayStatus() + " tournament cannot " + action);
    }

    private String displayStatus() {
        return status.name().toLowerCase().replace('_', ' ');
    }
}
