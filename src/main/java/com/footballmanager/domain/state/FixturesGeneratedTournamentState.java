package com.footballmanager.domain.state;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.TournamentStatus;

public final class FixturesGeneratedTournamentState extends AbstractTournamentState {
    public FixturesGeneratedTournamentState() {
        super(TournamentStatus.FIXTURES_GENERATED);
    }

    @Override
    public TournamentStatus complete(TournamentLifecycleContext context) {
        if (context.totalFixtureCount() == 0) {
            throw new BusinessRuleException("A tournament without fixtures cannot be completed");
        }
        if (context.incompleteFixtureCount() > 0) {
            throw new BusinessRuleException("Every fixture must be completed before the tournament can finish");
        }
        return TournamentStatus.COMPLETED;
    }
}
