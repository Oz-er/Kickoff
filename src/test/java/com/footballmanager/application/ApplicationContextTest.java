package com.footballmanager.application;

import com.footballmanager.config.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationContextTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void initializationCreatesConfiguredDatabase() {
        Path databasePath = temporaryDirectory.resolve("context.db");
        ApplicationContext context = new ApplicationContext(new ApplicationConfig(databasePath));

        context.initialize();

        assertTrue(Files.exists(databasePath));
        assertNotNull(context.teamRepository());
        assertNotNull(context.playerRepository());
        assertNotNull(context.tournamentRepository());
        assertNotNull(context.tournamentRegistrationRepository());
        assertNotNull(context.matchProgressRepository());
        assertNotNull(context.matchRepository());
        assertNotNull(context.tournamentService());
        assertNotNull(context.teamService());
        assertNotNull(context.playerService());
        assertNotNull(context.tournamentRegistrationService());
        assertNotNull(context.tournamentLifecycleService());
        assertNotNull(context.schedulingService());
        assertNotNull(context.resultService());
    }
}
