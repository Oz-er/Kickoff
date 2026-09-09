# Prompt 6 Historical Handoff

This document records the project state at the Omer-to-Rubaiyat handoff after Prompt 6. For the completed system, use `README.md` and `docs/handoff/final-release.md`.

## Completed behavior

- Maven and JavaFX application foundation with six-screen reusable navigation
- Configurable SQLite path and idempotent schema and demonstration data initialization
- Team and Player CRUD, case-insensitive search, player filtering, validation, confirmation dialogs, and relationship-safe deletion
- Tournament CRUD and ordered seeded registration with transactional batch behavior
- State-controlled tournament lifecycle from Draft through Completed
- Strategy-based round-robin and four- or eight-team knockout fixture generation
- Atomic schedule persistence, duplicate-generation protection, and explicit knockout winner-to-slot links

The Team and Player screens are functional. Dashboard, Tournaments, Fixtures, and Reports intentionally remain honest empty states for the next phase.

## Important classes

- `ApplicationContext` composes repositories, services, State resolution, and scheduling strategies.
- `TeamService`, `PlayerService`, `TeamViewModel`, and `PlayerViewModel` support the two implemented management screens.
- `TeamView` and `PlayerView` own JavaFX interaction without SQL or tournament business rules.
- `JdbcTeamRepository`, `JdbcPlayerRepository`, `JdbcTournamentRepository`, `JdbcTournamentRegistrationRepository`, and `JdbcMatchRepository` contain SQLite access.
- `TournamentState`, `DraftTournamentState`, `RegistrationClosedTournamentState`, `FixturesGeneratedTournamentState`, and `CompletedTournamentState` enforce lifecycle permissions.
- `ScheduleStrategy`, `RoundRobinScheduleStrategy`, `KnockoutScheduleStrategy`, and `SchedulingService` generate and persist fixtures.
- `UserMessageMapper` prevents unexpected technical details from reaching UI error dialogs.

## Database recreation

The default database is `data/football_manager.db`. To recreate it with the current schema and demonstration records:

1. Close the application.
2. Delete only `data/football_manager.db` if its development data is no longer needed.
3. Run `mvn javafx:run` from the project root.

Use `FOOTBALL_MANAGER_DATABASE=/absolute/path/test.db mvn javafx:run` to launch against another database. Existing database files are not migrated automatically, so recreation is required after a schema constraint changes.

## Verification

Run `mvn clean test` for the automated suite. It covers configuration, initialization, domain validation, repository CRUD and searches, foreign keys, uniqueness, registration rollback, every State's permissions and persisted transitions, both scheduling algorithms, atomic fixture persistence, view-model filtering and refresh behavior, safe UI error messages, and the knockout progression constraint regression.

Complete the [manual UI checklist](../testing/manual-ui-checklist.md) before the final demonstration.

## Limitations at the time of this handoff

- Tournament and fixture workflows do not yet have JavaFX forms.
- Match result entry, knockout winner advancement, undo, standings, and reports are not implemented.
- The planned Command pattern belongs to reversible match-result entry and has not been added early.
- Database schema changes use recreation rather than versioned migrations.
- Automated tests exercise view-model behavior, while JavaFX layout and modal interaction remain manual checks.
