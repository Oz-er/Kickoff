package com.footballmanager.config;

import com.footballmanager.application.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationConfigLoaderTest {
    @Test
    void usesDefaultDatabasePathWhenNoOverrideExists() {
        ApplicationConfig config = new ApplicationConfigLoader(new Properties(), Map.of()).load();

        assertEquals(Path.of("data", "football_manager.db").toAbsolutePath().normalize(), config.databasePath());
    }

    @Test
    void readsDatabasePathFromEnvironment() {
        Path expected = Path.of("build", "environment.db").toAbsolutePath().normalize();
        ApplicationConfig config = new ApplicationConfigLoader(
                new Properties(),
                Map.of(ApplicationConfigLoader.DATABASE_ENVIRONMENT_VARIABLE, expected.toString())
        ).load();

        assertEquals(expected, config.databasePath());
    }

    @Test
    void systemPropertyTakesPriorityOverEnvironment() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfigLoader.DATABASE_PROPERTY, "build/property.db");
        ApplicationConfig config = new ApplicationConfigLoader(
                properties,
                Map.of(ApplicationConfigLoader.DATABASE_ENVIRONMENT_VARIABLE, "build/environment.db")
        ).load();

        assertEquals(Path.of("build/property.db").toAbsolutePath().normalize(), config.databasePath());
    }

    @Test
    void rejectsInvalidConfiguredPath() {
        Properties properties = new Properties();
        properties.setProperty(ApplicationConfigLoader.DATABASE_PROPERTY, "bad\u0000path");

        assertThrows(ConfigurationException.class, () -> new ApplicationConfigLoader(properties, Map.of()).load());
    }
}
