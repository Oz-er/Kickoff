package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.TournamentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcTournamentRepository implements TournamentRepository {
    private static final String COLUMNS = "id, name, format, status, start_date, created_at";
    private final DatabaseManager databaseManager;

    public JdbcTournamentRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    @Override
    public Tournament create(Tournament tournament) {
        requireNewTournament(tournament);
        String sql = "INSERT INTO tournaments(name, format, status, start_date) VALUES (?, ?, ?, ?)";
        long id;
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(statement, tournament);
            statement.executeUpdate();
            id = generatedId(statement);
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Creating the tournament", exception);
        }
        return findById(id).orElseThrow(() -> new EntityNotFoundException("Created tournament could not be loaded"));
    }

    @Override
    public Optional<Tournament> findById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Tournament id");
        String sql = "SELECT " + COLUMNS + " FROM tournaments WHERE id = ?";
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
            throw JdbcRepositorySupport.translate("Loading the tournament", exception);
        }
    }

    @Override
    public List<Tournament> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM tournaments ORDER BY start_date, name COLLATE NOCASE, id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Loading tournaments", exception);
        }
    }

    @Override
    public List<Tournament> searchByName(String query) {
        String sql = "SELECT " + COLUMNS + " FROM tournaments WHERE name LIKE ? ESCAPE '\\' COLLATE NOCASE ORDER BY start_date, name COLLATE NOCASE, id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, JdbcRepositorySupport.searchPattern(query));
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Searching tournaments", exception);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM tournaments";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0;
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Counting tournaments", exception);
        }
    }

    @Override
    public Tournament update(Tournament tournament) {
        requireStoredTournament(tournament);
        String sql = "UPDATE tournaments SET name = ?, format = ?, status = ?, start_date = ? WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setFields(statement, tournament);
            statement.setLong(5, tournament.id());
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Tournament " + tournament.id() + " was not found");
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Updating the tournament", exception);
        }
        return findById(tournament.id()).orElseThrow(() -> new EntityNotFoundException("Updated tournament could not be loaded"));
    }

    @Override
    public void deleteById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Tournament id");
        String sql = "DELETE FROM tournaments WHERE id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("Tournament " + id + " was not found");
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Deleting the tournament", exception);
        }
    }

    private Tournament map(ResultSet resultSet) throws SQLException {
        return new Tournament(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                TournamentFormat.valueOf(resultSet.getString("format")),
                TournamentStatus.valueOf(resultSet.getString("status")),
                LocalDate.parse(resultSet.getString("start_date")),
                JdbcRepositorySupport.readCreatedAt(resultSet)
        );
    }

    private List<Tournament> mapAll(ResultSet resultSet) throws SQLException {
        List<Tournament> tournaments = new ArrayList<>();
        while (resultSet.next()) {
            tournaments.add(map(resultSet));
        }
        return List.copyOf(tournaments);
    }

    private void setFields(PreparedStatement statement, Tournament tournament) throws SQLException {
        statement.setString(1, tournament.name());
        statement.setString(2, tournament.format().name());
        statement.setString(3, tournament.status().name());
        statement.setString(4, tournament.startDate().toString());
    }

    private long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("Tournament id was not generated");
    }

    private void requireNewTournament(Tournament tournament) {
        if (tournament == null) {
            throw new ValidationException("Tournament is required");
        }
        if (tournament.id() != null) {
            throw new ValidationException("A new tournament must not already have an id");
        }
    }

    private void requireStoredTournament(Tournament tournament) {
        if (tournament == null || tournament.id() == null) {
            throw new ValidationException("An existing tournament id is required for update");
        }
    }
}
