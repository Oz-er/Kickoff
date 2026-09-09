package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.exception.DatabaseException;
import com.footballmanager.application.exception.ValidationException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class JdbcRepositorySupport {
    private static final DateTimeFormatter DATABASE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JdbcRepositorySupport() {
    }

    static RuntimeException translate(String action, SQLException exception) {
        if (exception.getErrorCode() == 19) {
            return new DataIntegrityException(action + " was rejected because it violates a data rule", exception);
        }
        return new DatabaseException(action + " failed", exception);
    }

    static long requirePositiveId(long id, String fieldName) {
        if (id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number");
        }
        return id;
    }

    static String searchPattern(String query) {
        String value = query == null ? "" : query.trim();
        value = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + value + "%";
    }

    static LocalDateTime readCreatedAt(ResultSet resultSet) throws SQLException {
        return LocalDateTime.parse(resultSet.getString("created_at"), DATABASE_TIME);
    }
}
