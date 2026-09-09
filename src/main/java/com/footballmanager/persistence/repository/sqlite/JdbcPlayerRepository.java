package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Player;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.PlayerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcPlayerRepository implements PlayerRepository {
    private static final String COLUMNS = "id, team_id, full_name, shirt_number, position, created_at";
    private final DatabaseManager databaseManager;

    public JdbcPlayerRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    @Override
    public Player create(Player player) {
        requireNewPlayer(player);
        String sql = "INSERT INTO players(team_id, full_name, shirt_number, position) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setEditableFields(statement, player);
            statement.executeUpdate();
            long id = generatedId(statement);
            return findById(id).orElseThrow(() -> new EntityNotFoundException("Created player could not be loaded"));
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Creating the player", exception);
        }
    }

    @Override
    public Optional<Player> findById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Player id");
        String sql = "SELECT " + COLUMNS + " FROM players WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Loading the player", exception);
        }
    }

    @Override
    public List<Player> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM players ORDER BY full_name COLLATE NOCASE, id";
        return query(sql, null, null, "Loading players");
    }

    @Override
    public List<Player> findByTeamId(long teamId) {
        JdbcRepositorySupport.requirePositiveId(teamId, "Team id");
        String sql = "SELECT " + COLUMNS + " FROM players WHERE team_id = ? ORDER BY shirt_number, full_name COLLATE NOCASE, id";
        return query(sql, teamId, null, "Loading players for the team");
    }

    @Override
    public List<Player> searchByName(String query) {
        String sql = "SELECT " + COLUMNS + " FROM players WHERE full_name LIKE ? ESCAPE '\\' COLLATE NOCASE ORDER BY full_name COLLATE NOCASE, id";
        return query(sql, null, JdbcRepositorySupport.searchPattern(query), "Searching players");
    }

    @Override
    public List<Player> searchByTeamIdAndName(long teamId, String query) {
        JdbcRepositorySupport.requirePositiveId(teamId, "Team id");
        String sql = "SELECT " + COLUMNS + " FROM players WHERE team_id = ? AND full_name LIKE ? ESCAPE '\\' COLLATE NOCASE ORDER BY shirt_number, full_name COLLATE NOCASE, id";
        return query(sql, teamId, JdbcRepositorySupport.searchPattern(query), "Searching players for the team");
    }

    @Override
    public Player update(Player player) {
        requireStoredPlayer(player);
        String sql = "UPDATE players SET team_id = ?, full_name = ?, shirt_number = ?, position = ? WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setEditableFields(statement, player);
            statement.setLong(5, player.id());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Player " + player.id() + " was not found");
            }
            return findById(player.id()).orElseThrow(() -> new EntityNotFoundException("Updated player could not be loaded"));
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Updating the player", exception);
        }
    }

    @Override
    public void deleteById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Player id");
        String sql = "DELETE FROM players WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Player " + id + " was not found");
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Deleting the player", exception);
        }
    }

    private List<Player> query(String sql, Long teamId, String pattern, String action) {
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            if (teamId != null) {
                statement.setLong(parameter++, teamId);
            }
            if (pattern != null) {
                statement.setString(parameter, pattern);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Player> players = new ArrayList<>();
                while (resultSet.next()) {
                    players.add(map(resultSet));
                }
                return List.copyOf(players);
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate(action, exception);
        }
    }

    private Player map(ResultSet resultSet) throws SQLException {
        return new Player(
                resultSet.getLong("id"),
                resultSet.getLong("team_id"),
                resultSet.getString("full_name"),
                resultSet.getInt("shirt_number"),
                resultSet.getString("position"),
                JdbcRepositorySupport.readCreatedAt(resultSet)
        );
    }

    private void setEditableFields(PreparedStatement statement, Player player) throws SQLException {
        statement.setLong(1, player.teamId());
        statement.setString(2, player.fullName());
        statement.setInt(3, player.shirtNumber());
        statement.setString(4, player.position());
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("Player id was not generated");
    }

    private void requireNewPlayer(Player player) {
        if (player == null) {
            throw new ValidationException("Player is required");
        }
        if (player.id() != null) {
            throw new ValidationException("A new player must not already have an id");
        }
    }

    private void requireStoredPlayer(Player player) {
        if (player == null || player.id() == null) {
            throw new ValidationException("An existing player id is required for update");
        }
    }
}
