package com.footballmanager.ui.viewmodel;

import com.footballmanager.application.dto.PlayerDto;
import com.footballmanager.application.dto.SavePlayerRequest;
import com.footballmanager.application.dto.SaveTeamRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.service.PlayerService;
import com.footballmanager.application.service.TeamService;
import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import com.footballmanager.persistence.repository.sqlite.JdbcPlayerRepository;
import com.footballmanager.persistence.repository.sqlite.JdbcTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerViewModelTest {
    @TempDir
    Path temporaryDirectory;

    private TeamService teamService;
    private PlayerViewModel viewModel;

    @BeforeEach
    void initializeViewModel() {
        DatabaseManager databaseManager = new DatabaseManager(temporaryDirectory.resolve("player-view-model.db"));
        new DatabaseInitializer(databaseManager).initialize();
        JdbcTeamRepository teams = new JdbcTeamRepository(databaseManager);
        teamService = new TeamService(teams);
        viewModel = new PlayerViewModel(
                new PlayerService(new JdbcPlayerRepository(databaseManager), teams),
                teamService
        );
    }

    @Test
    void filtersByTeamAndCaseInsensitivePlayerNameThenMaintainsTheFilter() {
        TeamDto dhaka = teamService.create(new SaveTeamRequest("Dhaka Eagles", "DE", null));
        TeamDto sylhet = teamService.create(new SaveTeamRequest("Sylhet Stars", "SS", null));
        PlayerDto player = viewModel.save(null, new SavePlayerRequest(dhaka.id(), "Karim Hasan", 10, "Forward"));
        viewModel.save(null, new SavePlayerRequest(sylhet.id(), "Karim Ahmed", 7, "Midfielder"));

        assertEquals(1, viewModel.filter(dhaka.id(), "KARIM").size());
        assertEquals("Dhaka Eagles", viewModel.players().getFirst().teamName());

        viewModel.save(player.id(), new SavePlayerRequest(dhaka.id(), "Rafi Hasan", 10, "Forward"));

        assertTrue(viewModel.players().isEmpty());

        viewModel.filter(dhaka.id(), "rafi");
        viewModel.delete(player.id());

        assertTrue(viewModel.players().isEmpty());
    }

    @Test
    void exposesAvailableTeamsForThePlayerEditor() {
        teamService.create(new SaveTeamRequest("Dhaka Eagles", "DE", null));

        assertEquals("Dhaka Eagles", viewModel.availableTeams().getFirst().name());
    }
}
