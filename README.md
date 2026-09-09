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

To recreate the development database with the latest schema and demonstration data, close the application, delete `data/football_manager.db`, and launch the application again. Only delete this generated file when its saved development data is no longer needed.

## Team and player persistence

The current persistence layer provides repository interfaces with SQLite JDBC implementations for:

- Creating, finding, listing, updating, and deleting teams and players
- Case-insensitive team and player name searches
- Filtering players by team, with an optional name search
- Preserving records when the application reconnects to the same database

Team names and short codes are unique without regard to letter case. Shirt numbers must be from 1 to 99 and are unique within a team. Foreign keys prevent players from referring to missing teams and prevent a team from being deleted while players still reference it. SQL values are passed through prepared statements.

## Testing

Run the complete automated suite with:

```bash
mvn clean test
```

Repository integration tests create isolated temporary databases. Current coverage includes domain input validation, CRUD, case-insensitive search, reconnect persistence, seed repeatability, uniqueness, shirt-number boundaries, foreign keys, and delete restrictions.

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

The application currently provides its build and navigation foundation plus tested Team and Player domain models and SQLite repositories. Four demonstration teams and four players are seeded idempotently. Feature screens remain clear empty states until their workflows are implemented.
