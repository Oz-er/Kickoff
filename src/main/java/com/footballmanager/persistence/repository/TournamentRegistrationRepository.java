package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.TournamentRegistration;

import java.util.List;

public interface TournamentRegistrationRepository {
    void addAll(List<TournamentRegistration> registrations);

    List<TournamentRegistration> findByTournamentId(long tournamentId);

    boolean exists(long tournamentId, long teamId);

    void remove(long tournamentId, long teamId);
}
