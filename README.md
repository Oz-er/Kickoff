# Football Tournament Manager

A desktop application for organizing small round-robin and knockout football tournaments. The project is developed for the Design Patterns Lab using JavaFX, Maven, and SQLite.

## Team

- Omer Mahboob - BSSE1655
- Rubaiyat Islam - BSSE1626

## Requirements

- JDK 21
- Maven 3.9+

## Commands

```bash
mvn clean test
mvn javafx:run
```

The application creates `data/football_manager.db` on first launch. The generated database file is intentionally ignored by Git; the reproducible schema and seed scripts are versioned under `src/main/resources/db`.

## Database location

The default database path is `data/football_manager.db`, resolved from the directory where the application starts. Override it with either option below. The Java system property takes priority when both are present.

```bash
FOOTBALL_MANAGER_DATABASE=/absolute/path/football.db mvn javafx:run
```

When launching from an IDE, the equivalent Java VM option is:

```text
-Dfootball.manager.database=/absolute/path/football.db
```

Tests use temporary database directories and do not modify the development database.

## Project structure

```text
src/main/java/com/footballmanager
├── application     application wiring and use cases
├── config          environment and path configuration
├── domain          business models and rules
├── persistence     SQLite initialization and repositories
└── ui              JavaFX navigation, views, and controllers
```

## Planned capabilities

- Team, player, and tournament management
- Round-robin and single-elimination fixture generation
- Tournament lifecycle validation
- Match-result recording, correction, and undo
- League standings, knockout progression, search, and summary reports
- Persistent SQLite storage with reproducible schema and seed data

## Design focus

The planned design introduces Strategy for genuinely different fixture-generation algorithms, State for tournament lifecycle behavior, and Command for reversible match-result operations. Each pattern will be added with its corresponding feature so that the implementation is justified by a concrete need. Detailed architecture, diagrams, testing evidence, and pattern justifications will be added as those features are implemented.

## Current status

The foundation provides an enforced JDK/Maven build environment, application composition root, configurable database path, shared startup exceptions, six-screen navigation shell, SQLite initialization, seed data, and focused foundation tests. Feature screens currently show clear empty states until their workflows are implemented.
