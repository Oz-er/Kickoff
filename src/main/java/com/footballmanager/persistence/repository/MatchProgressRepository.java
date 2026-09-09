package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.FixtureProgress;

public interface MatchProgressRepository {
    FixtureProgress findForTournament(long tournamentId);
}
