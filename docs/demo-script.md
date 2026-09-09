# Two-Person Demonstration Script

Target duration: eight minutes. Each member speaks for four minutes.

## Omer Mahboob — 0:00 to 4:00

### 0:00–0:45: purpose and architecture

- Introduce both members and the course-sized football tournament problem.
- Show the six-screen navigation.
- Explain the UI → application → domain → repository → SQLite dependency direction.

### 0:45–1:30: database and CRUD

- Show seeded Teams and Players, case-insensitive search, and player team filtering.
- Show `schema.sql` and identify the five tables, primary keys, foreign keys, composite registration key, and important unique/check constraints.
- Explain that every connection enables foreign keys and that generated databases are ignored.

### 1:30–2:30: State pattern

- Create a Draft tournament and attempt to close registration too early.
- Register the required teams and close registration successfully.
- Explain `TournamentState`, the four concrete states, `TournamentStateResolver`, and why repeated status conditionals were rejected.

### 2:30–4:00: Strategy pattern and transactions

- Preview and generate a round-robin schedule.
- Show that `SchedulingService` selects `RoundRobinScheduleStrategy` or `KnockoutScheduleStrategy` by format.
- Explain that strategies calculate fixtures without SQL and `JdbcMatchRepository` saves fixtures plus the status transition atomically.
- Briefly show the Strategy and State portions of the Mermaid class diagram.

## Rubaiyat Islam — 4:00 to 8:00

### 4:00–5:15: Command pattern

- Open Fixtures & Results and record a score.
- Demonstrate Undo Last Result and a corrected score.
- Explain `RecordMatchResultCommand`, `MatchSnapshot`, `CommandHistory`, and why failed commands never enter history.
- Explain the knockout correction guard when a downstream match is already completed.

### 5:15–6:15: knockout progression

- Open a prepared knockout tournament or create a four-team example.
- Record both semifinal results and show the finalists appear in the linked final.
- Explain `next_match_id` and `next_match_slot` and why result/progression writes share one transaction.

### 6:15–7:10: analytics and reports

- Show Dashboard totals and tournament-filtered upcoming fixtures.
- Show the round-robin standings columns and deterministic tie ordering.
- Show knockout progress and copy or save the plain-text summary.

### 7:10–8:00: tests and limitations

- Show the final `mvn clean test` result and identify unit, temporary-SQLite integration, rollback, and end-to-end tests.
- State the main limits: four/eight-team knockout, session-only undo, no migrations, and no concurrent users.
- Finish by completing a tournament after all fixtures have results.

## Questions Omer should be ready to answer

1. Why is State justified instead of one status `switch`?
2. How does adding another tournament format affect Strategy classes?
3. Which operation saves fixtures and tournament status in one transaction?
4. What prevents deletion of a team referenced by players, registrations, or matches?
5. Why is `tournament_teams` both a join table and an entity with `seed_number`?

## Questions Rubaiyat should be ready to answer

1. Why does Command store a `MatchSnapshot` instead of only the old score?
2. Why must failed commands stay out of `CommandHistory`?
3. What happens when both winners become known for a pending knockout match?
4. Which database constraints protect match status and score consistency?
5. How are standings ties ordered, and why is that logic outside the repository?

## Questions both members should be ready to answer

- Name the three claimed patterns and one rejected alternative for each.
- Explain where transaction boundaries are and give one rollback test example.
- Explain how a fresh database is created and how persistence after restart is tested.
- Identify one defect found during integration and the regression test that prevents it returning.
