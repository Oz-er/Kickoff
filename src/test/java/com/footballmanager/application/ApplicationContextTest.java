package com.footballmanager.application;

import com.footballmanager.config.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationContextTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void initializationCreatesConfiguredDatabase() {
        Path databasePath = temporaryDirectory.resolve("context.db");
        ApplicationContext context = new ApplicationContext(new ApplicationConfig(databasePath));

        context.initialize();

        assertTrue(Files.exists(databasePath));
    }
}
