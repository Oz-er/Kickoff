package com.footballmanager.persistence.repository;

import com.footballmanager.domain.model.Player;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository {
    Player create(Player player);

    Optional<Player> findById(long id);

    List<Player> findAll();

    List<Player> findByTeamId(long teamId);

    List<Player> searchByName(String query);

    List<Player> searchByTeamIdAndName(long teamId, String query);

    Player update(Player player);

    void deleteById(long id);
}
