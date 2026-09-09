# Manual UI Checklist

Run the application with `mvn javafx:run` and complete this checklist against a disposable development database.

## Shell and navigation

- [ ] The application opens at 1100 by 720 pixels without an exception.
- [ ] The window remains usable at its 900 by 600 minimum size.
- [ ] Dashboard, Teams, Players, Tournaments, Fixtures, and Reports are reachable from the left navigation.
- [ ] Dashboard, Tournaments, Fixtures, and Reports show clear empty states rather than unfinished controls.
- [ ] The selected navigation item remains visually distinct.

## Teams

- [ ] The seeded teams appear with the correct record count.
- [ ] Add a team with a name, two-to-five-character short code, and optional coach.
- [ ] Try blank names and invalid short codes and confirm a friendly validation message appears.
- [ ] Try a duplicate name or short code with different letter casing and confirm a friendly database message appears.
- [ ] Search with lower- and upper-case text and confirm the same matching teams appear.
- [ ] Edit the team name, code, and coach and confirm the table refreshes.
- [ ] Double-click a team and confirm the edit dialog opens.
- [ ] Cancel a delete confirmation and confirm the team remains.
- [ ] Confirm deletion of a team with no relationships and confirm it disappears.
- [ ] Try deleting a team that has a player and confirm the relationship is protected with no stack trace.

## Players

- [ ] The seeded players appear with their team, shirt number, and position.
- [ ] Select a team filter and confirm only that team's players appear.
- [ ] Combine a team filter with mixed-case name search and confirm both filters apply.
- [ ] Add a player by choosing a team and entering a name, shirt number, and optional position.
- [ ] Try a non-number, zero, and 100 for the shirt number and confirm friendly validation messages appear.
- [ ] Try a shirt number already used by the selected team and confirm a friendly database message appears.
- [ ] Reuse that shirt number for a different team and confirm it is accepted.
- [ ] Edit a player, including moving the player to another team, and confirm the table refreshes.
- [ ] Double-click a player and confirm the edit dialog opens.
- [ ] Cancel a delete confirmation and confirm the player remains.
- [ ] Confirm deletion and confirm the player disappears.

## Persistence and error presentation

- [ ] Close and relaunch the application and confirm saved Team and Player changes remain.
- [ ] Confirm expected validation and database failures are shown as concise dialog messages.
- [ ] Confirm no dialog displays a stack trace, SQL statement, JDBC URL, or internal exception type.
