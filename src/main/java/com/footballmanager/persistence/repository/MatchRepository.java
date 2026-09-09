package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.TournamentStatus;

import java.util.List;
import java.util.Optional;

public interface MatchRepository {
    Optional<Match> findById(long id);

    List<Match> findByTournamentId(long tournamentId);

    boolean existsForTournament(long tournamentId);

    void saveScheduleAndTransition(
            long tournamentId,
            TournamentStatus expectedStatus,
            TournamentStatus nextStatus,
            List<FixtureDraft> fixtures
    );
}
