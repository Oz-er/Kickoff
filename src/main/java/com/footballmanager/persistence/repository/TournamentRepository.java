package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.Tournament;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository {
    Tournament create(Tournament tournament);

    Optional<Tournament> findById(long id);

    List<Tournament> findAll();

    List<Tournament> searchByName(String query);

    long count();

    Tournament update(Tournament tournament);

    void deleteById(long id);
}
