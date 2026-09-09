package com.footballmanager.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TournamentSchemaIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("tournament-constraints.db"));
        new DatabaseInitializer(databaseManager).initialize();
    }

    @Test
    void databaseRejectsInvalidTournamentFormatStatusAndDate() {
        assertThrows(SQLException.class, () -> insertTournament("Format Cup", "LEAGUE", "DRAFT", "2026-12-01"));
        assertThrows(SQLException.class, () -> insertTournament("Status Cup", "KNOCKOUT", "OPEN", "2026-12-01"));
        assertThrows(SQLException.class, () -> insertTournament("Date Cup", "KNOCKOUT", "DRAFT", "not-a-date"));
    }

    private void insertTournament(String name, String format, String status, String date) throws SQLException {
        String sql = "INSERT INTO tournaments(name, format, status, start_date) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, format);
            statement.setString(3, status);
            statement.setString(4, date);
            statement.executeUpdate();
        }
    }
}
