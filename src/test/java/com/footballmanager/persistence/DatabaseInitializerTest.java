package com.footballmanager.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseInitializerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void initializationIsRepeatableAndSeedsTeams() throws Exception {
        DatabaseManager manager = new DatabaseManager(temporaryDirectory.resolve("test.db"));
        DatabaseInitializer initializer = new DatabaseInitializer(manager);

        initializer.initialize();
        initializer.initialize();

        try (Connection connection = manager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM teams")) {
            assertEquals(4, result.getInt(1));
        }
    }
}
