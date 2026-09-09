# Final Manual UI Checklist

Run `mvn javafx:run` against a disposable database. Use both the normal 1100 × 720 window and the 900 × 600 minimum size.

## Shell and navigation

- [ ] Application opens without an exception and Dashboard is selected.
- [ ] Dashboard, Teams, Players, Tournaments, Fixtures & Results, and Standings & Reports are reachable.
- [ ] The selected navigation item remains visually distinct.
- [ ] Each screen remains usable at both documented sizes.

## Teams and players

- [ ] Seeded teams and players appear.
- [ ] Add, edit, search, and delete an unreferenced team.
- [ ] Cancel a delete and confirm no change occurs.
- [ ] Verify duplicate team names and codes are rejected regardless of case.
- [ ] Add and edit a player; combine team filtering with mixed-case name search.
- [ ] Verify shirt numbers 0 and 100 are rejected, while 1 and 99 are accepted.
- [ ] Verify duplicate shirt numbers are rejected within one team but allowed across different teams.
- [ ] Verify deleting a referenced team shows a friendly relationship message.

## Round-robin workflow

- [ ] Create a Round Robin tournament.
- [ ] Create another Draft tournament, cancel one deletion, then confirm deletion.
- [ ] Verify the screen says at least three teams are required.
- [ ] Attempt to close with fewer than three teams and confirm status remains Draft.
- [ ] Register three or more teams with unique seeds and close registration.
- [ ] Preview fixtures and verify every pair appears once.
- [ ] Confirm generation and verify status becomes Fixtures Generated.
- [ ] Record results, including a draw, and verify standings columns and ordering.
- [ ] Attempt completion before all fixtures finish and confirm it is rejected.
- [ ] Finish all fixtures, complete the tournament, and verify the Completed display.

## Knockout workflow

- [ ] Create a Knockout tournament and verify the screen says exactly four or eight teams are required.
- [ ] Verify an unsupported team count is rejected without changing status.
- [ ] Register four teams with unique seeds, preview, and generate the three-match bracket.
- [ ] Verify the final begins Pending and shows winner-to-slot progression.
- [ ] Verify a Pending match cannot receive a score and a knockout draw is rejected.
- [ ] Record both semifinals and confirm the final becomes Scheduled with both winners.
- [ ] Correct an unconsumed semifinal result and verify its final slot changes.
- [ ] Record the final, then verify changing a feeder result is rejected without changing the bracket.
- [ ] Complete the tournament after every match is Completed.

## Undo, dashboard, and reports

- [ ] Record or correct a result and use Undo Last Result.
- [ ] Verify the previous score, status, and knockout slot are restored.
- [ ] Verify the Undo button is disabled when session history is empty.
- [ ] Dashboard totals match stored teams, players, and tournaments.
- [ ] Upcoming fixtures exclude Completed and Pending matches and respect the tournament filter.
- [ ] Round Robin shows league standings; Knockout shows bracket progress.
- [ ] Summary text contains tournament name, format, status, date, and match count.
- [ ] Copy summary text and save it to a `.txt` file.

## Persistence and error presentation

- [ ] Close and relaunch the application and verify saved data, results, and statuses remain.
- [ ] Verify validation dialogs retain recoverable form input.
- [ ] Verify no dialog displays a stack trace, SQL statement, JDBC URL, or internal exception class.
- [ ] Verify the generated database remains under `data/` and is not staged by Git.
