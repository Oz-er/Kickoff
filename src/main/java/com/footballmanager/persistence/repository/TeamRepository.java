package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.Team;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {
    Team create(Team team);

    Optional<Team> findById(long id);

    List<Team> findAll();

    List<Team> searchByName(String query);

    Team update(Team team);

    void deleteById(long id);
}
