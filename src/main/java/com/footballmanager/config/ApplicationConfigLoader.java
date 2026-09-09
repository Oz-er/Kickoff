package com.footballmanager.config;

import com.footballmanager.application.exception.ConfigurationException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public final class ApplicationConfigLoader {
    public static final String DATABASE_PROPERTY = "football.manager.database";
    public static final String DATABASE_ENVIRONMENT_VARIABLE = "FOOTBALL_MANAGER_DATABASE";
    private static final Path DEFAULT_DATABASE_PATH = Path.of("data", "football_manager.db");

    private final Properties properties;
    private final Map<String, String> environment;

    public ApplicationConfigLoader(Properties properties, Map<String, String> environment) {
        this.properties = Objects.requireNonNull(properties);
        this.environment = Objects.requireNonNull(environment);
    }

    public static ApplicationConfigLoader system() {
        return new ApplicationConfigLoader(System.getProperties(), System.getenv());
    }

    public ApplicationConfig load() {
        String propertyValue = properties.getProperty(DATABASE_PROPERTY);
        String environmentValue = environment.get(DATABASE_ENVIRONMENT_VARIABLE);
        String configuredPath = firstNonBlank(propertyValue, environmentValue);
        try {
            Path path = configuredPath == null ? DEFAULT_DATABASE_PATH : Path.of(configuredPath.trim());
            return new ApplicationConfig(path);
        } catch (InvalidPathException exception) {
            throw new ConfigurationException("The configured database path is invalid", exception);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
