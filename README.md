# Football Tournament Manager

A course-sized Java desktop application for organizing round-robin and single-elimination football tournaments. It demonstrates three justified Gang of Four behavioral patterns, layered architecture, SQLite persistence, automated tests, and six working JavaFX screens.

## Team

- Omer Mahboob — BSSE1655
- Rubaiyat Islam — BSSE1626

## Implemented features

- Team and player CRUD with case-insensitive search and team filtering
- Draft tournament creation, editing, and confirmed deletion
- Ordered team registration with unique seed numbers
- Round-robin scheduling for three or more teams
- Four-team and eight-team knockout brackets with winner progression
- Schedule preview followed by atomic fixture generation
- Match result entry, correction, session-scoped undo, and validation
- Tournament completion after every fixture is completed
- League standings ordered by points, goal difference, goals scored, and team name
- Dashboard totals and upcoming-fixture filtering
- Knockout progress, tournament summaries, clipboard copy, and text-file export
- SQLite persistence across application restarts

## Technology requirements

- JDK 21
- Maven 3.9 or newer
- JavaFX dependencies are downloaded by Maven
- SQLite is embedded through the JDBC driver; no database server is required

Confirm the environment:

```bash
java -version
mvn -version
```

Both commands must report Java 21. If Maven reports another Java version, set `JAVA_HOME` to the JDK 21 installation before continuing.

## Build, test, and run

From the project root:

```bash
mvn clean test
mvn javafx:run
```

The application opens at 1100 × 720 and has a minimum size of 900 × 600. Use the left navigation to reach Dashboard, Teams, Players, Tournaments, Fixtures & Results, and Standings & Reports.

## Database setup and recreation

The default database is created at `data/football_manager.db` on first launch. The schema and demonstration seed data come from:

- `src/main/resources/db/schema.sql`
- `src/main/resources/db/seed.sql`

The initializer is idempotent, so launching the application repeatedly does not duplicate seed data. The generated database is ignored by Git.

To recreate the database:

1. Close the application.
2. Delete only `data/football_manager.db`.
3. Run `mvn javafx:run` again.

This removes locally saved application data. The project intentionally uses recreation instead of schema migrations.

To use another location:

```bash
FOOTBALL_MANAGER_DATABASE=/absolute/path/football.db mvn javafx:run
```

The equivalent JVM property is:

```text
-Dfootball.manager.database=/absolute/path/football.db
```

The JVM property has priority over the environment variable.

## Main workflows

### Round robin

1. Create at least three teams.
2. Create a Round Robin tournament.
3. Register teams with unique seeds and close registration.
4. Preview and confirm fixture generation.
5. Record all results in Fixtures & Results.
6. View the league table in Standings & Reports.
7. Return to Tournaments and complete the tournament.

### Knockout

1. Create or use exactly four or eight teams.
2. Create a Knockout tournament and register each team with a unique seed.
3. Close registration, preview the bracket, and generate fixtures.
4. Record first-round results; winners populate the linked future match slots.
5. Continue until the final is complete.
6. Return to Tournaments and complete the tournament.

Knockout draws are rejected. A feeder result cannot be corrected after its downstream match has already been completed because that would invalidate the completed bracket.

## Architecture

```text
JavaFX views
    ↓
View models and application services
    ↓
Domain models, State, Strategy, and Command
    ↓
Repository interfaces
    ↓
SQLite JDBC implementations
```

- `ui` contains JavaFX navigation, views, dialogs, and view models.
- `application` coordinates use cases, DTOs, commands, and transaction-facing operations.
- `domain` contains entities, validation, lifecycle states, and scheduling strategies.
- `persistence` contains database initialization, repository interfaces, and JDBC implementations.
- `ApplicationContext` is the composition root that creates and connects concrete dependencies.

JavaFX code does not access SQL. Scheduling, lifecycle, result, and standings rules remain outside controllers.

## Design patterns

Only three Gang of Four patterns are claimed:

| Pattern | Problem solved | Main classes |
|---|---|---|
| State | Tournament permissions and transitions depend on lifecycle status | `TournamentState`, four concrete state classes, `TournamentStateResolver`, `TournamentLifecycleService` |
| Strategy | Round-robin and knockout schedules use different algorithms | `ScheduleStrategy`, `RoundRobinScheduleStrategy`, `KnockoutScheduleStrategy`, `SchedulingService` |
| Command | A result and its knockout progression must be executable and reversible as one action | `UndoableCommand`, `RecordMatchResultCommand`, `MatchSnapshot`, `CommandHistory`, `ResultService` |

`ApplicationEventPublisher` is a small UI refresh mechanism and is not claimed as a fourth design pattern.

See the [pattern audit and class diagrams](docs/architecture/design-patterns.md) and the [database ER diagram](docs/architecture/database-er.md).

## Database rules

The five tables are `teams`, `players`, `tournaments`, `tournament_teams`, and `matches`. Important safeguards include:

- Case-insensitive unique team names, short codes, and tournament names
- Shirt numbers from 1 to 99 and unique within a team
- Foreign keys enabled on every connection
- Relationship-safe team deletion
- Unique team and seed registration per tournament
- Non-negative scores and consistent Pending, Scheduled, and Completed match data
- Atomic registration, fixture generation, result progression, and undo operations

## Validation and error handling

Application services validate identifiers and business operations before persistence. SQLite constraints provide a second boundary for relationships and stored data. Expected failures become readable validation, business-rule, relationship, or not-found messages. UI dialogs do not expose stack traces, SQL, JDBC URLs, or internal exception class names.

Examples include blank names, duplicate codes, invalid shirt numbers, duplicate seeds, unsupported team counts, stale schedule previews, knockout draws, premature tournament completion, and unsafe result correction.

## Testing

Run all automated tests:

```bash
mvn clean test
```

The final suite contains 119 tests. It covers domain validation, repository CRUD and search, foreign keys, uniqueness, database restart persistence, State permissions, Strategy algorithms, Command execution and undo, transaction rollback, standings, reports, view models, and complete round-robin and knockout workflows.

Useful focused commands:

```bash
mvn -Dtest=EndToEndWorkflowIntegrationTest test
mvn -Dtest=SchedulingServiceIntegrationTest test
mvn -Dtest=ResultServiceIntegrationTest test
```

See the [test-case catalogue](docs/testing/test-cases.md) and [manual UI checklist](docs/testing/manual-ui-checklist.md).

## Documentation

- [Design-pattern audit and Mermaid class diagrams](docs/architecture/design-patterns.md)
- [Mermaid ER diagram](docs/architecture/database-er.md)
- [Test-case catalogue](docs/testing/test-cases.md)
- [Manual UI checklist](docs/testing/manual-ui-checklist.md)
- [Two-person demonstration script](docs/demo-script.md)
- [Final release handoff](docs/handoff/final-release.md)

## Known limitations

- Undo history exists only during the current application session and supports undo, not redo.
- Knockout tournaments support exactly four or eight teams; byes and other bracket sizes are outside scope.
- Fixtures have no venue management and the UI does not assign kickoff dates or times.
- Schema changes require database recreation; migrations are outside scope.
- The application is designed for one local user and does not handle concurrent editors.
- JavaFX layout and modal-dialog interaction require the documented manual checks.

## Troubleshooting

### Maven uses the wrong Java version

Set `JAVA_HOME` to JDK 21 and confirm with `mvn -version`.

### The application fails after a schema change

Close it, remove only `data/football_manager.db`, and launch again. Existing databases are not migrated.

### A team cannot be deleted

Remove its players and tournament registrations first. Foreign keys intentionally protect referenced teams.

### Registration cannot close

Round robin requires at least three teams. Knockout requires exactly four or eight teams, and all seeds must be unique.

### A knockout score is rejected

Scores must be non-negative and knockout matches cannot end in a draw. A feeder result also cannot change after the linked downstream match is completed.

### Tournament completion is rejected

Every generated fixture must have a completed result. Finish remaining matches, then try Complete Tournament again.
