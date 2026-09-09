package com.footballmanager.config;

import java.nio.file.Path;
import java.util.Objects;

public record ApplicationConfig(Path databasePath) {
    public ApplicationConfig {
        databasePath = Objects.requireNonNull(databasePath).toAbsolutePath().normalize();
    }
}
