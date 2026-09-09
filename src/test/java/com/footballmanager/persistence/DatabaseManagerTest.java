package com.footballmanager.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsParentDirectoryAndEnablesForeignKeys() throws Exception {
        Path databasePath = temporaryDirectory.resolve("nested").resolve("manager.db");
        DatabaseManager manager = new DatabaseManager(databasePath);

        try (Connection connection = manager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, result.getInt(1));
        }

        assertTrue(Files.exists(databasePath.getParent()));
    }
}
