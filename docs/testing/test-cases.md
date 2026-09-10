# Test-Case Catalogue

The automated suite uses JUnit 5. Repository and workflow integration tests create isolated SQLite databases under JUnit temporary directories.

| Area | Normal case | Boundary case | Error or rollback case | Main evidence |
|---|---|---|---|---|
| Initialization | Schema and seeds are created | Initializer runs repeatedly | Existing data is not duplicated | `DatabaseInitializerTest` |
| Teams | CRUD and case-insensitive search | Two-to-five-character short code | Duplicate name/code and referenced deletion | `JdbcTeamRepositoryIntegrationTest`, `TeamPlayerSchemaIntegrationTest` |
| Players | CRUD, team filter, search | Shirt numbers 1 and 99 | Missing team, duplicate team shirt number, 0 and 100 | `JdbcPlayerRepositoryIntegrationTest`, `TeamPlayerValidationTest` |
| Tournaments | Draft CRUD and search | Valid ISO start date | Duplicate name, invalid fields, non-Draft edit/delete | `JdbcTournamentRepositoryIntegrationTest`, `TournamentValidationTest` |
| Registration | Ordered unique seeds | Minimum format-specific team counts | Duplicate team/seed and failed batch roll back fully | `TournamentRegistrationServiceIntegrationTest` |
| State | Valid Draft through Completed transitions | Round robin 3 teams; knockout 4 or 8 | Wrong-state operations and incomplete fixtures are rejected | `TournamentStateTest`, `TournamentLifecycleServiceIntegrationTest` |
| Round-robin Strategy | Every pairing appears once | Odd and even team counts | Stale preview and duplicate generation | `RoundRobinScheduleStrategyTest`, `SchedulingServiceIntegrationTest` |
| Knockout Strategy | First round, pending future rounds, winner links | Four-team and eight-team brackets | Unsupported counts, failed insert, or changed status roll back | `KnockoutScheduleStrategyTest`, `SchedulingServiceIntegrationTest` |
| Command results | Record, correct, progress winner, undo | 0–0 round-robin draw | Negative score, knockout draw, pending match, failed execute/undo | `RecordMatchResultCommandTest`, `ResultServiceIntegrationTest` |
| Correction safety | Correct an unconsumed feeder result | Downstream match Scheduled | Downstream Completed rejects correction without mutation | `ResultServiceIntegrationTest` |
| Standings | Win/draw/loss totals and points | Empty/incomplete fixtures and complete ties | Knockout standings and missing tournament are rejected | `StandingsCalculatorTest`, `ReportServiceIntegrationTest` |
| Reports | Counts, filtered upcoming fixtures, summary text | Result limit and no fixtures | Invalid limit or missing tournament | `ReportServiceIntegrationTest`, `DashboardViewModelTest` |
| UI boundaries | View-model loading, filtering, and delegation | Empty lists and null filters | Friendly error mapping hides technical details | `ui.viewmodel` tests, `UserMessageMapperTest` |
| Complete workflows | Create, register, schedule, score, report, complete | Three-team league and four-team knockout | Premature close leaves Draft unchanged | `EndToEndWorkflowIntegrationTest` |
| Restart persistence | Reopen the same SQLite path | Completed tournament and all fixtures | Generated development database is never used by tests | `EndToEndWorkflowIntegrationTest`, repository integration tests |

## Transaction-focused checks

- Registration batch failure leaves no partial registrations.
- Fixture foreign-key failure leaves no matches and does not change tournament status.
- Fixture status-race failure rolls back inserted matches.
- Result-progression failure restores the source and downstream matches and does not enter Command history.
- Failed undo remains available in history so the user can retry after the underlying problem is resolved.

## Commands

```bash
mvn clean test
mvn -Dtest=EndToEndWorkflowIntegrationTest test
mvn -Dtest=SchedulingServiceIntegrationTest test
mvn -Dtest=ResultServiceIntegrationTest test
```

The final verified full-suite result is 119 tests, 0 failures, 0 errors, and 0 skipped.
