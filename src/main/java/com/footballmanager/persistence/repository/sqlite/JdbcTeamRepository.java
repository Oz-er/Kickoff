package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Team;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.TeamRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcTeamRepository implements TeamRepository {
    private static final String COLUMNS = "id, name, short_code, coach_name, created_at";
    private final DatabaseManager databaseManager;

    public JdbcTeamRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    @Override
    public Team create(Team team) {
        requireNewTeam(team);
        String sql = "INSERT INTO teams(name, short_code, coach_name) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, team.name());
            statement.setString(2, team.shortCode());
            statement.setString(3, team.coachName());
            statement.executeUpdate();
            long id = generatedId(statement, "Team");
            return findById(id).orElseThrow(() -> new EntityNotFoundException("Created team could not be loaded"));
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Creating the team", exception);
        }
    }

    @Override
    public Optional<Team> findById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Team id");
        String sql = "SELECT " + COLUMNS + " FROM teams WHERE id = ?";
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
            throw JdbcRepositorySupport.translate("Loading the team", exception);
        }
    }

    @Override
    public List<Team> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM teams ORDER BY name COLLATE NOCASE, id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Loading teams", exception);
        }
    }

    @Override
    public List<Team> searchByName(String query) {
        String sql = "SELECT " + COLUMNS + " FROM teams WHERE name LIKE ? ESCAPE '\\' COLLATE NOCASE ORDER BY name COLLATE NOCASE, id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, JdbcRepositorySupport.searchPattern(query));
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Searching teams", exception);
        }
    }

    @Override
    public Team update(Team team) {
        requireStoredTeam(team);
        String sql = "UPDATE teams SET name = ?, short_code = ?, coach_name = ? WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, team.name());
            statement.setString(2, team.shortCode());
            statement.setString(3, team.coachName());
            statement.setLong(4, team.id());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Team " + team.id() + " was not found");
            }
            return findById(team.id()).orElseThrow(() -> new EntityNotFoundException("Updated team could not be loaded"));
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Updating the team", exception);
        }
    }

    @Override
    public void deleteById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Team id");
        String sql = "DELETE FROM teams WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Team " + id + " was not found");
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Deleting the team", exception);
        }
    }

    private Team map(ResultSet resultSet) throws SQLException {
        return new Team(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("short_code"),
                resultSet.getString("coach_name"),
                JdbcRepositorySupport.readCreatedAt(resultSet)
        );
    }

    private List<Team> mapAll(ResultSet resultSet) throws SQLException {
        List<Team> teams = new ArrayList<>();
        while (resultSet.next()) {
            teams.add(map(resultSet));
        }
        return List.copyOf(teams);
    }

    private long generatedId(PreparedStatement statement, String entityName) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException(entityName + " id was not generated");
    }

    private void requireNewTeam(Team team) {
        if (team == null) {
            throw new ValidationException("Team is required");
        }
        if (team.id() != null) {
            throw new ValidationException("A new team must not already have an id");
        }
    }

    private void requireStoredTeam(Team team) {
        if (team == null || team.id() == null) {
            throw new ValidationException("An existing team id is required for update");
        }
    }
}
