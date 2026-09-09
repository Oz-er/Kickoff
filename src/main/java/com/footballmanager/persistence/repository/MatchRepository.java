package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchSnapshot;
import com.footballmanager.domain.model.NextMatchSlot;
import com.footballmanager.domain.model.TournamentStatus;

import java.util.List;
import java.util.Optional;

public interface MatchRepository {
    Optional<Match> findById(long id);

    List<Match> findByTournamentId(long tournamentId);

    boolean existsForTournament(long tournamentId);

    List<Match> findUpcoming(Long tournamentId, int limit);

    void saveScheduleAndTransition(
            long tournamentId,
            TournamentStatus expectedStatus,
            TournamentStatus nextStatus,
            List<FixtureDraft> fixtures
    );

    void recordResult(
            long matchId,
            int homeScore,
            int awayScore,
            Long nextMatchId,
            NextMatchSlot nextMatchSlot,
            Long progressedTeamId
    );

    void undoResult(MatchSnapshot snapshot);
}
