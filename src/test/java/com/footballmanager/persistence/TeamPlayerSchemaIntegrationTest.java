package com.footballmanager.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamPlayerSchemaIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("constraints.db"));
        new DatabaseInitializer(databaseManager).initialize();
    }

    @Test
    void databaseRejectsShirtNumbersOutsideOneToNinetyNine() throws Exception {
        long teamId = insertTeam("Boundary Team", "BDY");

        assertThrows(SQLException.class, () -> insertPlayer(teamId, "Zero Shirt", 0));
        assertThrows(SQLException.class, () -> insertPlayer(teamId, "Hundred Shirt", 100));
    }

    @Test
    void databaseRejectsMissingTeamAndDuplicateTeamShirtNumber() throws Exception {
        long teamId = insertTeam("Constraint Team", "CST");
        insertPlayer(teamId, "First Player", 15);

        assertThrows(SQLException.class, () -> insertPlayer(teamId, "Duplicate Shirt", 15));
        assertThrows(SQLException.class, () -> insertPlayer(999_999, "Missing Team", 16));
    }

    private long insertTeam(String name, String shortCode) throws SQLException {
        String sql = "INSERT INTO teams(name, short_code) VALUES (?, ?) RETURNING id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, shortCode);
            return statement.executeQuery().getLong(1);
        }
    }

    private void insertPlayer(long teamId, String name, int shirtNumber) throws SQLException {
        String sql = "INSERT INTO players(team_id, full_name, shirt_number) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teamId);
            statement.setString(2, name);
            statement.setInt(3, shirtNumber);
            statement.executeUpdate();
        }
    }
}
