package com.footballmanager.domain.state;

import com.footballmanager.domain.model.TournamentStatus;

public final class CompletedTournamentState extends AbstractTournamentState {
    public CompletedTournamentState() {
        super(TournamentStatus.COMPLETED);
    }
}
