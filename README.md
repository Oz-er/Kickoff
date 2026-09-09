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

The starter creates `data/football_manager.db` on first launch. The generated database file is intentionally ignored by Git; the reproducible schema and seed scripts are versioned under `src/main/resources/db`.

## Planned capabilities

- Team, player, and tournament management
- Round-robin and single-elimination fixture generation
- Tournament lifecycle validation
- Match-result recording, correction, and undo
- League standings, knockout progression, search, and summary reports
- Persistent SQLite storage with reproducible schema and seed data

## Design focus

The application uses Strategy for fixture-generation algorithms, State for tournament lifecycle behavior, and Command for reversible match-result operations. Detailed architecture, diagrams, testing evidence, and pattern justifications will be added as the corresponding features are implemented.

## Current status

The initial scaffold provides the Maven configuration, JavaFX application shell, SQLite schema initialization, seed data, and a database initialization test.
