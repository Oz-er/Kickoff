package com.footballmanager.application;

import com.footballmanager.application.scheduling.SchedulingService;
import com.footballmanager.application.service.TournamentLifecycleService;
import com.footballmanager.application.service.TournamentRegistrationService;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.application.service.TeamService;
import com.footballmanager.application.service.PlayerService;
import com.footballmanager.application.service.ResultService;
import com.footballmanager.config.ApplicationConfig;
import com.footballmanager.config.ApplicationConfigLoader;
import com.footballmanager.domain.state.TournamentStateResolver;
import com.footballmanager.domain.scheduling.KnockoutScheduleStrategy;
import com.footballmanager.domain.scheduling.RoundRobinScheduleStrategy;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.PlayerRepository;
import com.footballmanager.persistence.repository.MatchProgressRepository;
import com.footballmanager.persistence.repository.MatchRepository;
import com.footballmanager.persistence.repository.TeamRepository;
import com.footballmanager.persistence.repository.TournamentRegistrationRepository;
import com.footballmanager.persistence.repository.TournamentRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchProgressRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcMatchRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRegistrationRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTournamentRepository;

import java.util.Objects;
import java.util.List;

public final class ApplicationContext {
    private final ApplicationConfig config;
    private final DatabaseManager databaseManager;
    private final DatabaseInitializer databaseInitializer;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final MatchProgressRepository matchProgressRepository;
    private final MatchRepository matchRepository;
    private final TournamentService tournamentService;
    private final TeamService teamService;
    private final PlayerService playerService;
    private final TournamentRegistrationService tournamentRegistrationService;
    private final TournamentLifecycleService tournamentLifecycleService;
    private final SchedulingService schedulingService;
    private final ResultService resultService;

    public ApplicationContext(ApplicationConfig config) {
        this.config = Objects.requireNonNull(config);
        this.databaseManager = new DatabaseManager(config.databasePath());
        this.databaseInitializer = new DatabaseInitializer(databaseManager);
        this.teamRepository = new JdbcTeamRepository(databaseManager);
        this.playerRepository = new JdbcPlayerRepository(databaseManager);
        this.teamService = new TeamService(teamRepository);
        this.playerService = new PlayerService(playerRepository, teamRepository);
        this.tournamentRepository = new JdbcTournamentRepository(databaseManager);
        this.tournamentRegistrationRepository = new JdbcTournamentRegistrationRepository(databaseManager);
        this.matchProgressRepository = new JdbcMatchProgressRepository(databaseManager);
        this.matchRepository = new JdbcMatchRepository(databaseManager);
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
        this.schedulingService = new SchedulingService(
                tournamentRepository,
                tournamentRegistrationRepository,
                teamRepository,
                matchRepository,
                stateResolver,
                List.of(new RoundRobinScheduleStrategy(), new KnockoutScheduleStrategy())
        );
        this.resultService = new ResultService(
                matchRepository,
                tournamentRepository,
                teamRepository
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

    public MatchRepository matchRepository() {
        return matchRepository;
    }

    public TournamentService tournamentService() {
        return tournamentService;
    }

    public TeamService teamService() {
        return teamService;
    }

    public PlayerService playerService() {
        return playerService;
    }

    public TournamentRegistrationService tournamentRegistrationService() {
        return tournamentRegistrationService;
    }

    public TournamentLifecycleService tournamentLifecycleService() {
        return tournamentLifecycleService;
    }

    public SchedulingService schedulingService() {
        return schedulingService;
    }

    public ResultService resultService() {
        return resultService;
    }
}
