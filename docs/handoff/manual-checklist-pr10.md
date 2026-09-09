# Prompt 10 UI Manual Checklist

## Filtering
- [ ] Select a Tournament. Verify only matches from that tournament are listed.
- [ ] Select a Round from the Round combobox. Verify the list filters to matches in that round.
- [ ] Select a Status from the Status combobox (e.g., SCHEDULED, PENDING). Verify the list correctly filters by state.

## Result Entry & Progression
- [ ] Double-click a SCHEDULED match (where both home and away teams are known).
- [ ] Enter a valid integer for both Home Score and Away Score. Click Save.
- [ ] Verify the table updates. The match status should change to COMPLETED and the score should be visible.
- [ ] If this was a KNOCKOUT match, verify that the subsequent match (which had "Winner plays as X in Match Y") is updated and the winning team is propelled into that match (if the opponent is also determined, the next match state should become SCHEDULED).
- [ ] Double-click a PENDING match (where one or both teams are unknown). Verify that an error dialog appears immediately indicating that a pending match cannot be scored.
- [ ] Attempt to save a score using text instead of numbers. Verify a user-friendly error appears and the dialog does not lose the input data.

## Correction and Undo
- [ ] Double-click a COMPLETED match. Correct the score by entering new values. Save.
- [ ] Verify the table reflects the corrected score.
- [ ] Click the "Undo Last Result" button.
- [ ] Verify the most recent result modification is reverted to its previous state (or back to SCHEDULED).
- [ ] Verify the "Undo Last Result" button is only enabled when there is command history available.
