package com.footballmanager.application;

import com.footballmanager.config.ApplicationConfig;
import com.footballmanager.config.ApplicationConfigLoader;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;

import java.util.Objects;

public final class ApplicationContext {
    private final ApplicationConfig config;
    private final DatabaseManager databaseManager;
    private final DatabaseInitializer databaseInitializer;

    public ApplicationContext(ApplicationConfig config) {
        this.config = Objects.requireNonNull(config);
        this.databaseManager = new DatabaseManager(config.databasePath());
        this.databaseInitializer = new DatabaseInitializer(databaseManager);
    }

    public static ApplicationContext createDefault() {
        return new ApplicationContext(ApplicationConfigLoader.system().load());
    }

    public void initialize() {
        databaseInitializer.initialize();
    }

    public ApplicationConfig config() {
        return config;
    }

    public DatabaseManager databaseManager() {
        return databaseManager;
    }
}
