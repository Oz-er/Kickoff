package com.footballmanager.domain.state;

import com.footballmanager.domain.model.TournamentStatus;

public interface TournamentState {
    TournamentStatus status();

    void ensureCanEdit();

    void ensureCanDelete();

    void ensureCanChangeRegistration();

    void ensureCanGenerateFixtures();

    TournamentStatus closeRegistration(TournamentLifecycleContext context);

    TournamentStatus markFixturesGenerated(TournamentLifecycleContext context);

    TournamentStatus complete(TournamentLifecycleContext context);
}
