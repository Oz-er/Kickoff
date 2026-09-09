package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.domain.model.Player;
import com.footballmanager.domain.model.Team;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcPlayerRepositoryIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private TeamRepository teams;
    private PlayerRepository players;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("players.db"));
        new DatabaseInitializer(databaseManager).initialize();
        teams = new JdbcTeamRepository(databaseManager);
        players = new JdbcPlayerRepository(databaseManager);
    }

    @Test
    void createsUpdatesFiltersSearchesAndDeletesAcrossConnections() {
        Team firstTeam = teams.create(Team.create("First Team", "FST", null));
        Team secondTeam = teams.create(Team.create("Second Team", "SND", null));
        Player created = players.create(Player.create(firstTeam.id(), "Jamil Ahmed", 12, "Defender"));
        players.create(Player.create(secondTeam.id(), "Other Player", 12, null));

        Player reloaded = new JdbcPlayerRepository(databaseManager).findById(created.id()).orElseThrow();
        assertEquals("Jamil Ahmed", reloaded.fullName());
        assertTrue(reloaded.createdAt() != null);
        assertEquals(1, players.findByTeamId(firstTeam.id()).size());
        assertEquals(created.id(), players.searchByName("jAmIl").getFirst().id());
        assertEquals(created.id(), players.searchByTeamIdAndName(firstTeam.id(), "AHMED").getFirst().id());
        assertTrue(players.searchByTeamIdAndName(secondTeam.id(), "Ahmed").isEmpty());

        Player updated = players.update(new Player(created.id(), secondTeam.id(), "Jamil Hasan", 19, "Midfielder", created.createdAt()));
        assertEquals(secondTeam.id().longValue(), updated.teamId());
        assertEquals(19, updated.shirtNumber());

        players.deleteById(created.id());
        assertFalse(new JdbcPlayerRepository(databaseManager).findById(created.id()).isPresent());
        assertThrows(EntityNotFoundException.class, () -> players.deleteById(created.id()));
    }

    @Test
    void enforcesShirtNumberUniquenessWithinEachTeam() {
        Team firstTeam = teams.create(Team.create("First Team", "FST", null));
        Team secondTeam = teams.create(Team.create("Second Team", "SND", null));
        players.create(Player.create(firstTeam.id(), "First Player", 9, null));

        assertThrows(DataIntegrityException.class, () -> players.create(Player.create(firstTeam.id(), "Second Player", 9, null)));
        Player allowed = players.create(Player.create(secondTeam.id(), "Third Player", 9, null));
        assertEquals(9, allowed.shirtNumber());
    }

    @Test
    void rejectsAPlayerWhoseTeamDoesNotExist() {
        assertThrows(DataIntegrityException.class, () -> players.create(Player.create(999_999, "No Team Player", 5, null)));
    }
}
