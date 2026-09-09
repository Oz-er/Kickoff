package com.footballmanager.domain.state;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.TournamentStatus;

public final class RegistrationClosedTournamentState extends AbstractTournamentState {
    public RegistrationClosedTournamentState() {
        super(TournamentStatus.REGISTRATION_CLOSED);
    }

    @Override
    public void ensureCanGenerateFixtures() {
    }

    @Override
    public TournamentStatus markFixturesGenerated(TournamentLifecycleContext context) {
        if (context.totalFixtureCount() == 0) {
            throw new BusinessRuleException("Fixtures must be saved before the tournament can advance");
        }
        return TournamentStatus.FIXTURES_GENERATED;
    }
}
