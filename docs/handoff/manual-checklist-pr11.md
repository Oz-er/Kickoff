# Prompt 11 UI Manual Checklist

## Dashboard Screen
- [ ] Navigate to Dashboard. Stat cards show Teams, Players, and Tournaments counts.
- [ ] With no fixtures in the database, the Upcoming Fixtures table shows the empty-state placeholder.
- [ ] Select a specific tournament from the filter ComboBox. The Upcoming Fixtures table narrows to that tournament only.
- [ ] Select "All Tournaments". The Upcoming Fixtures table restores to the global upcoming list.
- [ ] After generating fixtures for a tournament, re-open Dashboard and verify the Upcoming Fixtures list updates correctly.

## Standings & Reports Screen - Round Robin
- [ ] Navigate to Standings & Reports. Both standings table and knockout table are hidden until a tournament is selected.
- [ ] Select a ROUND_ROBIN tournament. The League Standings table is visible and the Knockout Progress table is hidden.
- [ ] Verify the standings table columns: Team, P, W, D, L, GF, GA, GD, Pts.
- [ ] Record some results in Fixtures & Results and return here. Verify standings update on reselect.

## Standings & Reports Screen - Knockout
- [ ] Select a KNOCKOUT tournament. The Knockout Progress table is visible and the League Standings table is hidden.
- [ ] Verify the knockout table shows: Round, Match, Status, Score for all fixtures.
- [ ] Verify completed matches show actual scores and pending/scheduled ones show "—".

## Summary Report
- [ ] Select any tournament. The Summary Report text area populates with tournament name, format, status, start date, and total matches.
- [ ] Click "Copy to Clipboard". Paste into a text editor and verify the content matches what is displayed in the text area.
- [ ] Click "Save to File...". A file chooser dialog opens. Save and verify the file content is correct.
- [ ] Select no tournament (clear selection). The Summary Report text area should be empty.

## Navigation Smoke Check (all six screens)
- [ ] Dashboard — loads with stats and upcoming fixtures.
- [ ] Teams — lists teams, allows add/edit/delete.
- [ ] Players — lists players, allows add/edit/delete.
- [ ] Tournaments — allows draft/registration/scheduling workflow.
- [ ] Fixtures & Results — allows filtering and result entry.
- [ ] Standings & Reports — shows correct view for format, allows report copy/save.
