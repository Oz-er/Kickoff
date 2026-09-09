package com.footballmanager.application;

import com.footballmanager.application.service.TournamentLifecycleService;
import com.footballmanager.application.service.TournamentRegistrationService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.config.ApplicationConfig;
import com.footballmanager.config.ApplicationConfigLoader;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.MatchProgressRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchProgressRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRegistrationRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRepository;

import java.util.Objects;

public final class ApplicationContext {
    private final ApplicationConfig config;
    private final DatabaseManager databaseManager;
    private final DatabaseInitializer databaseInitializer;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final MatchProgressRepository matchProgressRepository;
    private final TournamentService tournamentService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentLifecycleService tournamentLifecycleService;

    public ApplicationContext(ApplicationConfig config) {
        this.config = Objects.requireNonNull(config);
        this.databaseManager = new DatabaseManager(config.databasePath());
        this.databaseInitializer = new DatabaseInitializer(databaseManager);
        this.teamRepository = new JdbcTeamRepository(databaseManager);
        this.playerRepository = new JdbcPlayerRepository(databaseManager);
        this.tournamentRepository = new JdbcTournamentRepository(databaseManager);
        this.tournamentRegistrationRepository = new JdbcTournamentRegistrationRepository(databaseManager);
        this.matchProgressRepository = new JdbcMatchProgressRepository(databaseManager);
        TournamentStateResolver stateResolver = new TournamentStateResolver();
        this.tournamentService = new TournamentService(tournamentRepository, stateResolver);
        this.tournamentRegistrationService = new TournamentRegistrationService(
                tournamentRepository,
                teamRepository,
                tournamentRegistrationRepository,
                stateResolver
        );
        this.tournamentLifecycleService = new TournamentLifecycleService(
                tournamentRepository,
                tournamentRegistrationRepository,
                matchProgressRepository,
                stateResolver
        );
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

    public TournamentRepository tournamentRepository() {
        return tournamentRepository;
    }

    public TournamentRegistrationRepository tournamentRegistrationRepository() {
        return tournamentRegistrationRepository;
    }

    public MatchProgressRepository matchProgressRepository() {
        return matchProgressRepository;
    }

    public TournamentService tournamentService() {
        return tournamentService;
    }

    public TournamentRegistrationService tournamentRegistrationService() {
        return tournamentRegistrationService;
    }

    public TournamentLifecycleService tournamentLifecycleService() {
        return tournamentLifecycleService;
    }
}
