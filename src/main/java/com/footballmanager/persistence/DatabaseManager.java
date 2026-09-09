package com.footballmanager.persistence;

import com.footballmanager.application.exception.DatabaseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class DatabaseManager {
    private final Path databasePath;

    public DatabaseManager(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath).toAbsolutePath().normalize();
    }

    public Connection openConnection() throws SQLException {
        createParentDirectory();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public Path databasePath() {
        return databasePath;
    }

    private void createParentDirectory() {
        Path parent = databasePath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new DatabaseException("Could not create the database directory", exception);
        }
    }
}
