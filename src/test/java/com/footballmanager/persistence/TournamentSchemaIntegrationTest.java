package com.footballmanager.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void pendingKnockoutMatchAllowsOneKnownTeamButRejectsTwoKnownTeams() throws SQLException {
        long tournamentId = insertAndReturnId(
                "INSERT INTO tournaments(name, format, status, start_date) VALUES (?, 'KNOCKOUT', 'REGISTRATION_CLOSED', '2026-12-01')",
                "Progression Cup"
        );
        long firstTeamId = insertAndReturnId(
                "INSERT INTO teams(name, short_code) VALUES (?, 'ONE')",
                "First Team"
        );
        long secondTeamId = insertAndReturnId(
                "INSERT INTO teams(name, short_code) VALUES (?, 'TWO')",
                "Second Team"
        );

        assertDoesNotThrow(() -> insertPendingMatch(tournamentId, firstTeamId, null));
        assertThrows(SQLException.class, () -> insertPendingMatch(tournamentId, firstTeamId, secondTeamId));
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

    private long insertAndReturnId(String sql, String value) throws SQLException {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, value);
            statement.executeUpdate();
            try (java.sql.ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private void insertPendingMatch(long tournamentId, Long homeTeamId, Long awayTeamId) throws SQLException {
        String sql = "INSERT INTO matches(tournament_id, round_number, home_team_id, away_team_id, status) VALUES (?, 2, ?, ?, 'PENDING')";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            if (homeTeamId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setLong(2, homeTeamId);
            }
            if (awayTeamId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setLong(3, awayTeamId);
            }
            statement.executeUpdate();
        }
    }
}
