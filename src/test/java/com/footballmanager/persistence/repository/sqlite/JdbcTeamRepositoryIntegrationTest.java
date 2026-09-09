package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.domain.model.Player;
import com.footballmanager.domain.model.Team;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTeamRepositoryIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private TeamRepository teams;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("teams.db"));
        new DatabaseInitializer(databaseManager).initialize();
        teams = new JdbcTeamRepository(databaseManager);
    }

    @Test
    void createsUpdatesFindsAndDeletesAcrossConnections() {
        Team created = teams.create(Team.create("Dhaka Eagles", "DE", "Amina Khan"));

        Team reloaded = new JdbcTeamRepository(databaseManager).findById(created.id()).orElseThrow();
        assertEquals("Dhaka Eagles", reloaded.name());
        assertEquals("DE", reloaded.shortCode());
        assertTrue(reloaded.createdAt() != null);

        Team updated = teams.update(new Team(created.id(), "Dhaka Eagles FC", "DEF", null, created.createdAt()));
        assertEquals("Dhaka Eagles FC", updated.name());
        assertEquals("DEF", updated.shortCode());
        assertEquals(1, teams.searchByName("EAGLES").stream().filter(team -> team.id().equals(created.id())).count());

        teams.deleteById(created.id());
        assertFalse(new JdbcTeamRepository(databaseManager).findById(created.id()).isPresent());
        assertThrows(EntityNotFoundException.class, () -> teams.deleteById(created.id()));
    }

    @Test
    void enforcesCaseInsensitiveTeamNameAndShortCodeUniqueness() {
        teams.create(Team.create("Dhaka Eagles", "DE", null));

        assertThrows(DataIntegrityException.class, () -> teams.create(Team.create("dhaka eagles", "DX", null)));
        assertThrows(DataIntegrityException.class, () -> teams.create(Team.create("Different Club", "de", null)));
    }

    @Test
    void treatsSearchWildcardsAndSqlTextAsLiteralInput() {
        Team percentTeam = teams.create(Team.create("Hundred% Club", "HPC", null));

        assertEquals(percentTeam.id(), teams.searchByName("%").getFirst().id());
        assertTrue(teams.searchByName("%' OR 1=1 --").isEmpty());
    }

    @Test
    void preventsDeletingATeamThatStillHasPlayers() {
        Team team = teams.create(Team.create("Protected Team", "PRT", null));
        JdbcPlayerRepository players = new JdbcPlayerRepository(databaseManager);
        players.create(Player.create(team.id(), "Protected Player", 7, null));

        assertThrows(DataIntegrityException.class, () -> teams.deleteById(team.id()));
        assertTrue(teams.findById(team.id()).isPresent());
    }
}
