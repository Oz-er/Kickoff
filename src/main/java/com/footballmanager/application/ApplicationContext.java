package com.footballmanager.application;

import com.footballmanager.config.ApplicationConfig;
import com.footballmanager.config.ApplicationConfigLoader;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;

import java.util.Objects;

public final class ApplicationContext {
    private final ApplicationConfig config;
    private final DatabaseManager databaseManager;
    private final DatabaseInitializer databaseInitializer;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public ApplicationContext(ApplicationConfig config) {
        this.config = Objects.requireNonNull(config);
        this.databaseManager = new DatabaseManager(config.databasePath());
        this.databaseInitializer = new DatabaseInitializer(databaseManager);
        this.teamRepository = new JdbcTeamRepository(databaseManager);
        this.playerRepository = new JdbcPlayerRepository(databaseManager);
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

    public TeamRepository teamRepository() {
        return teamRepository;
    }

    public PlayerRepository playerRepository() {
        return playerRepository;
    }
}
