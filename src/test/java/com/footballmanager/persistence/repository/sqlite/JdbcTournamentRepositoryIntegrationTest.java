package com.footballmanager.persistence.repository.sqlite;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.UpdateTournamentRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.application.exception.DataIntegrityException;
import com.footballmanager.application.service.TournamentService;
import com.footballmanager.domain.model.Tournament;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTournamentRepositoryIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private TournamentRepository tournaments;
    private TournamentService service;

    @BeforeEach
    void initializeDatabase() {
        databaseManager = new DatabaseManager(temporaryDirectory.resolve("tournaments.db"));
        new DatabaseInitializer(databaseManager).initialize();
        tournaments = new JdbcTournamentRepository(databaseManager);
        service = new TournamentService(tournaments);
    }

    @Test
    void createsUpdatesFindsSearchesAndDeletesAcrossConnections() {
        TournamentDto created = service.create(new CreateTournamentRequest(
                "Dhaka Championship",
                TournamentFormat.ROUND_ROBIN,
                LocalDate.of(2026, 11, 1)
        ));

        Tournament reloaded = new JdbcTournamentRepository(databaseManager).findById(created.id()).orElseThrow();
        assertEquals(TournamentStatus.DRAFT, reloaded.status());
        assertTrue(reloaded.createdAt() != null);

        TournamentDto updated = service.update(created.id(), new UpdateTournamentRequest(
                "Dhaka Knockout Cup",
                TournamentFormat.KNOCKOUT,
                LocalDate.of(2026, 11, 2)
        ));
        assertEquals(TournamentFormat.KNOCKOUT, updated.format());
        assertEquals(created.id(), service.searchByName("KNOCKOUT").getFirst().id());
        assertEquals(1, service.findAll().size());

        service.delete(created.id());
        assertFalse(new JdbcTournamentRepository(databaseManager).findById(created.id()).isPresent());
    }

    @Test
    void enforcesCaseInsensitiveTournamentNameUniqueness() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        service.create(new CreateTournamentRequest("Dhaka Cup", TournamentFormat.ROUND_ROBIN, date));

        assertThrows(DataIntegrityException.class, () -> service.create(
                new CreateTournamentRequest("dhaka cup", TournamentFormat.KNOCKOUT, date)
        ));
    }

    @Test
    void preventsEditingAndDeletingANonDraftTournament() {
        TournamentDto created = service.create(new CreateTournamentRequest(
                "Closed Cup",
                TournamentFormat.ROUND_ROBIN,
                LocalDate.of(2026, 11, 1)
        ));
        Tournament stored = tournaments.findById(created.id()).orElseThrow();
        tournaments.update(new Tournament(
                stored.id(),
                stored.name(),
                stored.format(),
                TournamentStatus.REGISTRATION_CLOSED,
                stored.startDate(),
                stored.createdAt()
        ));

        UpdateTournamentRequest update = new UpdateTournamentRequest(
                "Changed Cup",
                TournamentFormat.KNOCKOUT,
                LocalDate.of(2026, 12, 1)
        );
        assertThrows(BusinessRuleException.class, () -> service.update(created.id(), update));
        assertThrows(BusinessRuleException.class, () -> service.delete(created.id()));
        assertTrue(tournaments.findById(created.id()).isPresent());
    }
}
