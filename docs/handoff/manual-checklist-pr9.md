# Prompt 9 UI Manual Checklist

## Round Robin Format Checklist
- [ ] Create a new Tournament with ROUND_ROBIN format via "Create Draft".
- [ ] Verify you cannot "Close Registration" with 0 or 1 team (shows error).
- [ ] Register 3 teams, assigning unique seeds to each.
- [ ] Click "Close Registration". Tournament state should update to REGISTRATION_CLOSED.
- [ ] Verify Draft panel hides and Scheduling panel appears.
- [ ] Click "Preview Schedule". Ensure 3 fixtures appear in the preview table.
- [ ] Click "Confirm & Generate Fixtures".
- [ ] Verify the Active Tournament panel appears indicating fixtures are generated.

## Knockout Format Checklist
- [ ] Create a new Tournament with KNOCKOUT format via "Create Draft".
- [ ] Register 3 teams, assigning unique seeds.
- [ ] Click "Close Registration" and verify it is rejected with a business rule exception (not a power of 2).
- [ ] Register 1 more team (total 4 teams) with a unique seed.
- [ ] Click "Close Registration". Tournament state should update to REGISTRATION_CLOSED.
- [ ] Click "Preview Schedule". Ensure exactly 2 fixtures (Round 1) appear in the preview table.
- [ ] Click "Confirm & Generate Fixtures".
- [ ] Verify the Active Tournament panel appears indicating fixtures are generated.

## General UI Behaviors
- [ ] Selecting different tournaments in the ComboBox updates the panel correctly (Draft, Scheduling, or Active).
- [ ] Clicking "Edit Draft" populates the fields correctly.
- [ ] Pressing Cancel on the "Edit Draft" or "Create Draft" dialog makes no changes.
- [ ] Attempting to use duplicate seeds shows a validation error dialog.
