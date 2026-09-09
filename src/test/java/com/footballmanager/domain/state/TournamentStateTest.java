package com.footballmanager.domain.state;

import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TournamentStateTest {
    @Test
    void draftPermitsEditingRegistrationAndValidClosure() {
        TournamentState state = new DraftTournamentState();

        assertDoesNotThrow(state::ensureCanEdit);
        assertDoesNotThrow(state::ensureCanDelete);
        assertDoesNotThrow(state::ensureCanChangeRegistration);
        assertThrows(BusinessRuleException.class, state::ensureCanGenerateFixtures);
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, state.closeRegistration(context(TournamentFormat.ROUND_ROBIN, 3, 0, 0)));
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, state.closeRegistration(context(TournamentFormat.KNOCKOUT, 4, 0, 0)));
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, state.closeRegistration(context(TournamentFormat.KNOCKOUT, 8, 0, 0)));
    }

    @Test
    void draftRejectsInvalidCountsAndLaterTransitions() {
        TournamentState state = new DraftTournamentState();

        assertThrows(BusinessRuleException.class, () -> state.closeRegistration(context(TournamentFormat.ROUND_ROBIN, 2, 0, 0)));
        assertThrows(BusinessRuleException.class, () -> state.closeRegistration(context(TournamentFormat.KNOCKOUT, 3, 0, 0)));
        assertThrows(BusinessRuleException.class, () -> state.closeRegistration(context(TournamentFormat.KNOCKOUT, 5, 0, 0)));
        assertThrows(BusinessRuleException.class, () -> state.markFixturesGenerated(context(TournamentFormat.ROUND_ROBIN, 3, 1, 1)));
        assertThrows(BusinessRuleException.class, () -> state.complete(context(TournamentFormat.ROUND_ROBIN, 3, 1, 0)));
    }

    @Test
    void registrationClosedPermitsOnlyFixtureGenerationAfterFixturesExist() {
        TournamentState state = new RegistrationClosedTournamentState();

        assertEquals(TournamentStatus.FIXTURES_GENERATED, state.markFixturesGenerated(context(TournamentFormat.ROUND_ROBIN, 3, 3, 3)));
        assertThrows(BusinessRuleException.class, state::ensureCanEdit);
        assertThrows(BusinessRuleException.class, state::ensureCanDelete);
        assertThrows(BusinessRuleException.class, state::ensureCanChangeRegistration);
        assertDoesNotThrow(state::ensureCanGenerateFixtures);
        assertThrows(BusinessRuleException.class, () -> state.closeRegistration(context(TournamentFormat.ROUND_ROBIN, 3, 0, 0)));
        assertThrows(BusinessRuleException.class, () -> state.markFixturesGenerated(context(TournamentFormat.ROUND_ROBIN, 3, 0, 0)));
        assertThrows(BusinessRuleException.class, () -> state.complete(context(TournamentFormat.ROUND_ROBIN, 3, 3, 0)));
    }

    @Test
    void fixturesGeneratedPermitsCompletionOnlyAfterEveryFixtureFinishes() {
        TournamentState state = new FixturesGeneratedTournamentState();

        assertEquals(TournamentStatus.COMPLETED, state.complete(context(TournamentFormat.ROUND_ROBIN, 3, 3, 0)));
        assertThrows(BusinessRuleException.class, () -> state.complete(context(TournamentFormat.ROUND_ROBIN, 3, 3, 1)));
        assertThrows(BusinessRuleException.class, () -> state.complete(context(TournamentFormat.ROUND_ROBIN, 3, 0, 0)));
        assertThrows(BusinessRuleException.class, state::ensureCanEdit);
        assertThrows(BusinessRuleException.class, state::ensureCanDelete);
        assertThrows(BusinessRuleException.class, state::ensureCanChangeRegistration);
        assertThrows(BusinessRuleException.class, state::ensureCanGenerateFixtures);
        assertThrows(BusinessRuleException.class, () -> state.closeRegistration(context(TournamentFormat.ROUND_ROBIN, 3, 3, 0)));
        assertThrows(BusinessRuleException.class, () -> state.markFixturesGenerated(context(TournamentFormat.ROUND_ROBIN, 3, 3, 0)));
    }

    @Test
    void completedRejectsEveryLifecycleOperation() {
        TournamentState state = new CompletedTournamentState();
        TournamentLifecycleContext context = context(TournamentFormat.ROUND_ROBIN, 3, 3, 0);

        assertThrows(BusinessRuleException.class, state::ensureCanEdit);
        assertThrows(BusinessRuleException.class, state::ensureCanDelete);
        assertThrows(BusinessRuleException.class, state::ensureCanChangeRegistration);
        assertThrows(BusinessRuleException.class, state::ensureCanGenerateFixtures);
        assertThrows(BusinessRuleException.class, () -> state.closeRegistration(context));
        assertThrows(BusinessRuleException.class, () -> state.markFixturesGenerated(context));
        assertThrows(BusinessRuleException.class, () -> state.complete(context));
    }

    @Test
    void resolverReturnsTheConcreteStateForEveryStatus() {
        TournamentStateResolver resolver = new TournamentStateResolver();

        assertInstanceOf(DraftTournamentState.class, resolver.resolve(TournamentStatus.DRAFT));
        assertInstanceOf(RegistrationClosedTournamentState.class, resolver.resolve(TournamentStatus.REGISTRATION_CLOSED));
        assertInstanceOf(FixturesGeneratedTournamentState.class, resolver.resolve(TournamentStatus.FIXTURES_GENERATED));
        assertInstanceOf(CompletedTournamentState.class, resolver.resolve(TournamentStatus.COMPLETED));
    }

    private TournamentLifecycleContext context(
            TournamentFormat format,
            int teamCount,
            int totalFixtures,
            int incompleteFixtures
    ) {
        return new TournamentLifecycleContext(format, teamCount, totalFixtures, incompleteFixtures);
    }
}
