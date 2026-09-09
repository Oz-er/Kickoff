package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.EntityNotFoundException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.TournamentRegistration;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcTournamentRegistrationRepository implements TournamentRegistrationRepository {
    private final DatabaseManager databaseManager;

    public JdbcTournamentRegistrationRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    @Override
    public void addAll(List<TournamentRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            throw new ValidationException("At least one registration is required");
        }
        for (TournamentRegistration registration : registrations) {
            if (registration == null) {
                throw new ValidationException("Registration details are required");
            }
        }
        List<TournamentRegistration> values = List.copyOf(registrations);
        String sql = "INSERT INTO tournament_teams(tournament_id, team_id, seed_number) VALUES (?, ?, ?)";
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (TournamentRegistration registration : values) {
                    statement.setLong(1, registration.tournamentId());
                    statement.setLong(2, registration.teamId());
                    statement.setInt(3, registration.seedNumber());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                JdbcRepositorySupport.rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Registering teams", exception);
        }
    }

    @Override
    public List<TournamentRegistration> findByTournamentId(long tournamentId) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        String sql = "SELECT tournament_id, team_id, seed_number FROM tournament_teams WHERE tournament_id = ? ORDER BY seed_number, team_id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TournamentRegistration> registrations = new ArrayList<>();
                while (resultSet.next()) {
                    registrations.add(new TournamentRegistration(
                            resultSet.getLong("tournament_id"),
                            resultSet.getLong("team_id"),
                            resultSet.getInt("seed_number")
                    ));
                }
                return List.copyOf(registrations);
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Loading tournament registrations", exception);
        }
    }

    @Override
    public boolean exists(long tournamentId, long teamId) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        JdbcRepositorySupport.requirePositiveId(teamId, "Team id");
        String sql = "SELECT 1 FROM tournament_teams WHERE tournament_id = ? AND team_id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setLong(2, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Checking tournament registration", exception);
        }
    }

    @Override
    public void remove(long tournamentId, long teamId) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        JdbcRepositorySupport.requirePositiveId(teamId, "Team id");
        String sql = "DELETE FROM tournament_teams WHERE tournament_id = ? AND team_id = ?";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setLong(2, teamId);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException("The team is not registered in this tournament");
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Removing the tournament registration", exception);
        }
    }
}
