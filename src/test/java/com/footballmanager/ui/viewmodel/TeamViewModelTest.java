package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.SaveTeamRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.service.TeamService;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamViewModelTest {
    @TempDir
    Path temporaryDirectory;

    private TeamViewModel viewModel;

    @BeforeEach
    void initializeViewModel() {
        DatabaseManager databaseManager = new DatabaseManager(temporaryDirectory.resolve("team-view-model.db"));
        new DatabaseInitializer(databaseManager).initialize();
        viewModel = new TeamViewModel(new TeamService(new JdbcTeamRepository(databaseManager)));
    }

    @Test
    void createsUpdatesAndDeletesWhileKeepingTheCurrentSearch() {
        TeamDto dhaka = viewModel.save(null, new SaveTeamRequest("Dhaka Eagles", "DE", "Amina Khan"));
        viewModel.save(null, new SaveTeamRequest("Chattogram Waves", "CW", null));

        assertEquals(1, viewModel.search("EAGLES").size());

        TeamDto updated = viewModel.save(dhaka.id(), new SaveTeamRequest("Dhaka United", "DU", null));

        assertEquals("Dhaka United", updated.name());
        assertTrue(viewModel.teams().isEmpty());

        viewModel.search("dhaka");
        viewModel.delete(dhaka.id());

        assertTrue(viewModel.teams().isEmpty());
    }
}
