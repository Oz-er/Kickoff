package com.footballmanager.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private final DatabaseManager databaseManager;

    public DatabaseInitializer(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void initialize() {
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            executeScript(connection, "/db/schema.sql");
            executeScript(connection, "/db/seed.sql");
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize the database", exception);
        }
    }

    private void executeScript(Connection connection, String resource) throws SQLException {
        String script = readResource(resource);
        for (String sql : script.split(";")) {
            if (!sql.isBlank()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql.trim());
                }
            }
        }
    }

    private String readResource(String resource) {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing database resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read database resource: " + resource, exception);
        }
    }
}
