package com.footballmanager.domain.state;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;

public final class DraftTournamentState extends AbstractTournamentState {
    public DraftTournamentState() {
        super(TournamentStatus.DRAFT);
    }

    @Override
    public void ensureCanEdit() {
    }

    @Override
    public void ensureCanDelete() {
    }

    @Override
    public void ensureCanChangeRegistration() {
    }

    @Override
    public TournamentStatus closeRegistration(TournamentLifecycleContext context) {
        if (context.format() == TournamentFormat.ROUND_ROBIN && context.registeredTeamCount() < 3) {
            throw new BusinessRuleException("Round-robin tournaments require at least 3 registered teams");
        }
        if (context.format() == TournamentFormat.KNOCKOUT
                && context.registeredTeamCount() != 4
                && context.registeredTeamCount() != 8) {
            throw new BusinessRuleException("Knockout tournaments require exactly 4 or 8 registered teams");
        }
        return TournamentStatus.REGISTRATION_CLOSED;
    }
}
