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

## Tournament persistence and registration

Tournament application services support creating, reading, searching, editing, and deleting tournaments. New tournaments start in Draft. The State pattern rejects editing, deletion, registration, and unregistration after Draft.

Teams can be registered in an explicit seed order. A team and seed number can each appear only once per tournament. Multi-team registration uses one database transaction, so a failed row rolls back the entire batch. Deleting a Draft tournament also removes its registrations, while deleting a registered team is restricted to protect relationships.

## Testing

Run the complete automated suite with:

```bash
mvn clean test
```

Repository integration tests create isolated temporary databases. Current coverage includes domain input validation, CRUD, case-insensitive search, reconnect persistence, seed repeatability, uniqueness, shirt-number boundaries, foreign keys, ordered registration, relationship protection, safe deletion, transactional rollback, every State's permissions, format-specific team counts, fixture progress, and persisted lifecycle transitions.

Scheduling tests additionally cover odd and even round robins, unique pairings, deterministic seed ordering, four-team and eight-team knockout brackets, pending future rounds, winner-to-slot links, preview safety, duplicate prevention, and rollback when a match or status update fails.

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
- Tournament workflow screens for fixture generation
- Tournament lifecycle validation
- Match-result recording, correction, and undo
- League standings, knockout progression, search, and summary reports
- Persistent SQLite storage with reproducible schema and seed data

## Design focus

State controls tournament lifecycle permissions and transitions through four concrete states. Strategy selects between independently tested round-robin and knockout scheduling algorithms. Their [class diagrams and design justifications](docs/architecture/design-patterns.md) use the implemented class names and explain the alternatives considered.

Command remains planned for reversible match-result operations and will be introduced only with that feature.

## Current status

The application currently provides its build and navigation foundation plus tested Team, Player, Tournament, registration, and Match persistence. State enforces lifecycle behavior, while Strategy previews and atomically saves round-robin or knockout schedules with explicit progression links. Registration and schedule generation are transactional. Four demonstration teams and four players are seeded idempotently. Feature screens remain clear empty states until their workflows are implemented.
