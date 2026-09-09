package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.domain.model.FixtureDraft;
import com.footballmanager.domain.model.Match;
import com.footballmanager.domain.model.MatchSnapshot;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.domain.model.NextMatchSlot;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.MatchRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class JdbcMatchRepository implements MatchRepository {
    private static final String COLUMNS = "id, tournament_id, round_number, home_team_id, away_team_id, "
            + "home_score, away_score, status, scheduled_at, next_match_id, next_match_slot";
    private static final DateTimeFormatter DATABASE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseManager databaseManager;

    public JdbcMatchRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    @Override
    public Optional<Match> findById(long id) {
        JdbcRepositorySupport.requirePositiveId(id, "Match id");
        String sql = "SELECT " + COLUMNS + " FROM matches WHERE id = ?";
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
            throw JdbcRepositorySupport.translate("Loading the match", exception);
        }
    }

    @Override
    public List<Match> findByTournamentId(long tournamentId) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        String sql = "SELECT " + COLUMNS + " FROM matches WHERE tournament_id = ? ORDER BY round_number, id";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Match> matches = new ArrayList<>();
                while (resultSet.next()) {
                    matches.add(map(resultSet));
                }
                return List.copyOf(matches);
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Loading tournament matches", exception);
        }
    }

    @Override
    public boolean existsForTournament(long tournamentId) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        String sql = "SELECT 1 FROM matches WHERE tournament_id = ? LIMIT 1";
        try (Connection connection = databaseManager.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Checking tournament matches", exception);
        }
    }

    @Override
    public void saveScheduleAndTransition(
            long tournamentId,
            TournamentStatus expectedStatus,
            TournamentStatus nextStatus,
            List<FixtureDraft> fixtures
    ) {
        List<FixtureDraft> values = validateSchedule(tournamentId, expectedStatus, nextStatus, fixtures);
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureTournamentHasNoMatches(connection, tournamentId);
                Map<Integer, Long> ids = insertFixtures(connection, values);
                linkFixtures(connection, values, ids);
                updateTournamentStatus(connection, tournamentId, expectedStatus, nextStatus);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                JdbcRepositorySupport.rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Saving the tournament schedule", exception);
        }
    }

    private List<FixtureDraft> validateSchedule(
            long tournamentId,
            TournamentStatus expectedStatus,
            TournamentStatus nextStatus,
            List<FixtureDraft> fixtures
    ) {
        JdbcRepositorySupport.requirePositiveId(tournamentId, "Tournament id");
        if (expectedStatus == null || nextStatus == null) {
            throw new ValidationException("Expected and next tournament statuses are required");
        }
        if (fixtures == null || fixtures.isEmpty()) {
            throw new ValidationException("At least one fixture is required");
        }
        Set<Integer> fixtureNumbers = new HashSet<>();
        for (FixtureDraft fixture : fixtures) {
            if (fixture == null || fixture.tournamentId() != tournamentId) {
                throw new ValidationException("Every fixture must belong to the tournament");
            }
            if (!fixtureNumbers.add(fixture.fixtureNumber())) {
                throw new ValidationException("Fixture numbers must be unique");
            }
        }
        for (FixtureDraft fixture : fixtures) {
            if (fixture.nextFixtureNumber() != null && !fixtureNumbers.contains(fixture.nextFixtureNumber())) {
                throw new ValidationException("Every next fixture must exist in the schedule");
            }
            if (fixture.nextFixtureNumber() != null) {
                FixtureDraft nextFixture = fixtures.stream()
                        .filter(candidate -> candidate.fixtureNumber() == fixture.nextFixtureNumber())
                        .findFirst()
                        .orElseThrow();
                if (nextFixture.roundNumber() <= fixture.roundNumber()) {
                    throw new ValidationException("A winner must advance to a later round");
                }
            }
        }
        return List.copyOf(fixtures);
    }

    private void ensureTournamentHasNoMatches(Connection connection, long tournamentId) throws SQLException {
        String sql = "SELECT 1 FROM matches WHERE tournament_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new DataIntegrityException("Fixtures have already been generated for this tournament");
                }
            }
        }
    }

    private Map<Integer, Long> insertFixtures(Connection connection, List<FixtureDraft> fixtures) throws SQLException {
        String sql = "INSERT INTO matches(tournament_id, round_number, home_team_id, away_team_id, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        Map<Integer, Long> ids = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (FixtureDraft fixture : fixtures) {
                statement.setLong(1, fixture.tournamentId());
                statement.setInt(2, fixture.roundNumber());
                setNullableLong(statement, 3, fixture.homeTeamId());
                setNullableLong(statement, 4, fixture.awayTeamId());
                statement.setString(5, fixture.status().name());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Match id was not generated");
                    }
                    ids.put(fixture.fixtureNumber(), keys.getLong(1));
                }
            }
        }
        return ids;
    }

    private void linkFixtures(
            Connection connection,
            List<FixtureDraft> fixtures,
            Map<Integer, Long> ids
    ) throws SQLException {
        String sql = "UPDATE matches SET next_match_id = ?, next_match_slot = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (FixtureDraft fixture : fixtures) {
                if (fixture.nextFixtureNumber() != null) {
                    statement.setLong(1, ids.get(fixture.nextFixtureNumber()));
                    statement.setString(2, fixture.nextMatchSlot().name());
                    statement.setLong(3, ids.get(fixture.fixtureNumber()));
                    statement.executeUpdate();
                }
            }
        }
    }

    private void updateTournamentStatus(
            Connection connection,
            long tournamentId,
            TournamentStatus expectedStatus,
            TournamentStatus nextStatus
    ) throws SQLException {
        String sql = "UPDATE tournaments SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nextStatus.name());
            statement.setLong(2, tournamentId);
            statement.setString(3, expectedStatus.name());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Tournament status changed before the schedule was saved");
            }
        }
    }

    private Match map(ResultSet resultSet) throws SQLException {
        return new Match(
                resultSet.getLong("id"),
                resultSet.getLong("tournament_id"),
                resultSet.getInt("round_number"),
                nullableLong(resultSet, "home_team_id"),
                nullableLong(resultSet, "away_team_id"),
                nullableInteger(resultSet, "home_score"),
                nullableInteger(resultSet, "away_score"),
                MatchStatus.valueOf(resultSet.getString("status")),
                nullableTime(resultSet, "scheduled_at"),
                nullableLong(resultSet, "next_match_id"),
                nullableSlot(resultSet, "next_match_slot")
        );
    }

    private void setNullableLong(PreparedStatement statement, int parameter, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(parameter, java.sql.Types.INTEGER);
        } else {
            statement.setLong(parameter, value);
        }
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private LocalDateTime nullableTime(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? null : LocalDateTime.parse(value, DATABASE_TIME);
    }

    private NextMatchSlot nullableSlot(ResultSet resultSet, String column) throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? null : NextMatchSlot.valueOf(value);
    }

    @Override
    public void recordResult(
            long matchId,
            int homeScore,
            int awayScore,
            Long nextMatchId,
            NextMatchSlot nextMatchSlot,
            Long progressedTeamId
    ) {
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                updateMatchResult(connection, matchId, homeScore, awayScore);
                if (nextMatchId != null) {
                    progressWinner(connection, nextMatchId, nextMatchSlot, progressedTeamId);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                JdbcRepositorySupport.rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Recording the match result", exception);
        }
    }

    @Override
    public void undoResult(MatchSnapshot snapshot) {
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (snapshot.nextMatchId() != null && snapshot.progressedTeamId() != null) {
                    restoreNextMatch(connection, snapshot);
                }
                restoreMatch(connection, snapshot);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                JdbcRepositorySupport.rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw JdbcRepositorySupport.translate("Undoing the match result", exception);
        }
    }

    private void updateMatchResult(Connection connection, long matchId, int homeScore, int awayScore) throws SQLException {
        String sql = "UPDATE matches SET home_score = ?, away_score = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, homeScore);
            statement.setInt(2, awayScore);
            statement.setString(3, MatchStatus.COMPLETED.name());
            statement.setLong(4, matchId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Match " + matchId + " was not updated");
            }
        }
    }

    private void progressWinner(Connection connection, long nextMatchId, NextMatchSlot slot, long winnerId) throws SQLException {
        String column = slot == NextMatchSlot.HOME ? "home_team_id" : "away_team_id";
        String otherColumn = slot == NextMatchSlot.HOME ? "away_team_id" : "home_team_id";
        String sql = "UPDATE matches SET " + column + " = ?, "
                + "status = CASE WHEN " + otherColumn + " IS NOT NULL THEN 'SCHEDULED' ELSE status END "
                + "WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, winnerId);
            statement.setLong(2, nextMatchId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Next match " + nextMatchId + " was not updated");
            }
        }
    }

    private void restoreMatch(Connection connection, MatchSnapshot snapshot) throws SQLException {
        String sql = "UPDATE matches SET home_score = ?, away_score = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (snapshot.homeScore() == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, snapshot.homeScore());
            }
            if (snapshot.awayScore() == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, snapshot.awayScore());
            }
            statement.setString(3, snapshot.status().name());
            statement.setLong(4, snapshot.matchId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Match " + snapshot.matchId() + " was not restored");
            }
        }
    }

    private void restoreNextMatch(Connection connection, MatchSnapshot snapshot) throws SQLException {
        String sql = "UPDATE matches SET home_team_id = ?, away_team_id = ?, status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (snapshot.nextMatchPreviousHomeTeamId() == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, snapshot.nextMatchPreviousHomeTeamId());
            }
            if (snapshot.nextMatchPreviousAwayTeamId() == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setLong(2, snapshot.nextMatchPreviousAwayTeamId());
            }
            statement.setString(3, snapshot.nextMatchPreviousStatus().name());
            statement.setLong(4, snapshot.nextMatchId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Next match " + snapshot.nextMatchId() + " was not restored");
            }
        }
    }
}
