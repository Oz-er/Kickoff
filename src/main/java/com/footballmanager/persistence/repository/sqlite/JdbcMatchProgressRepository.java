package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.domain.model.FixtureProgress;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.MatchProgressRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class JdbcMatchProgressRepository implements MatchProgressRepository {
    private final DatabaseManager databaseManager;

    public JdbcMatchProgressRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    @Override
    public FixtureProgress findForTournament(long tournamentId) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        String sql = "SELECT COUNT(*) AS total_fixtures, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 0 ELSE 1 END), 0) AS incomplete_fixtures "
                + "FROM matches WHERE tournament_id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return new FixtureProgress(
                        resultSet.getInt("total_fixtures"),
                        resultSet.getInt("incomplete_fixtures")
                );
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Loading fixture progress", exception);
        }
    }
}
