package com.footballmanager.ui.view;

import com.footballmanager.application.dto.DashboardSummaryDto;
import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.ui.viewmodel.DashboardViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public final class DashboardView extends VBox {
    private final DashboardViewModel viewModel;

    private final ObservableList<TournamentDto> tournaments = FXCollections.observableArrayList();
    private final ComboBox<TournamentDto> tournamentCombo = new ComboBox<>(tournaments);

    private final Label totalTeamsLabel = new Label("—");
    private final Label totalPlayersLabel = new Label("—");
    private final Label totalTournamentsLabel = new Label("—");

    private final ObservableList<MatchDto> upcomingItems = FXCollections.observableArrayList();
    private final TableView<MatchDto> upcomingTable = new TableView<>(upcomingItems);

    public DashboardView(DashboardViewModel viewModel) {
        this.viewModel = viewModel;
        setPadding(new Insets(32));
        setSpacing(24);
        getStyleClass().add("page-content");

        PageHeaderView header = new PageHeaderView("Dashboard", "Tournament overview and upcoming fixtures.");

        HBox filterBar = createFilterBar();
        HBox statsRow = createStatsRow();
        VBox upcomingSection = createUpcomingSection();
        VBox.setVgrow(upcomingSection, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, statsRow, upcomingSection);

        reloadTournaments();
        refresh(null);
    }

    private HBox createFilterBar() {
        tournamentCombo.setPromptText("All Tournaments");
        tournamentCombo.setPrefWidth(260);
        tournamentCombo.getItems().add(null);
        tournamentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TournamentDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "All Tournaments" : item.name() + " (" + item.status() + ")"));
            }
        });
        tournamentCombo.setButtonCell(tournamentCombo.getCellFactory().call(null));
        tournamentCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                refresh(newVal == null ? null : newVal.id()));

        HBox bar = new HBox(12, new Label("Tournament:"), tournamentCombo);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox createStatsRow() {
        VBox teamsCard = statCard("Teams", totalTeamsLabel);
        VBox playersCard = statCard("Players", totalPlayersLabel);
        VBox tournamentsCard = statCard("Tournaments", totalTournamentsLabel);

        HBox row = new HBox(16, teamsCard, playersCard, tournamentsCard);
        HBox.setHgrow(teamsCard, Priority.ALWAYS);
        HBox.setHgrow(playersCard, Priority.ALWAYS);
        HBox.setHgrow(tournamentsCard, Priority.ALWAYS);
        return row;
    }

    private VBox statCard(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-card-title");
        valueLabel.getStyleClass().add("stat-card-value");
        VBox card = new VBox(6, titleLabel, valueLabel);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private VBox createUpcomingSection() {
        Label heading = new Label("Upcoming Fixtures");
        heading.getStyleClass().add("section-title");

        TableColumn<MatchDto, String> roundCol = new TableColumn<>("Round");
        roundCol.setCellValueFactory(data -> new SimpleStringProperty("Round " + data.getValue().roundNumber()));
        roundCol.setPrefWidth(80);

        TableColumn<MatchDto, String> matchCol = new TableColumn<>("Match");
        matchCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().homeTeamName() + " vs " + data.getValue().awayTeamName()));
        matchCol.setPrefWidth(300);

        TableColumn<MatchDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));

        upcomingTable.getColumns().setAll(List.of(roundCol, matchCol, statusCol));
        upcomingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        upcomingTable.setPlaceholder(new Label("No upcoming fixtures for the selected tournament."));
        upcomingTable.setPrefHeight(240);
        VBox.setVgrow(upcomingTable, Priority.ALWAYS);

        VBox section = new VBox(10, heading, upcomingTable);
        section.getStyleClass().add("card");
        section.setPadding(new Insets(16));
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }

    private void reloadTournaments() {
        List<TournamentDto> list = viewModel.loadTournaments();
        tournaments.clear();
        tournaments.add(null);
        tournaments.addAll(list);
    }

    private void refresh(Long tournamentId) {
        DashboardSummaryDto summary = viewModel.loadSummary(tournamentId);
        totalTeamsLabel.setText(String.valueOf(summary.totalTeams()));
        totalPlayersLabel.setText(String.valueOf(summary.totalPlayers()));
        totalTournamentsLabel.setText(String.valueOf(summary.totalTournaments()));
        upcomingItems.setAll(summary.upcomingFixtures());
    }
}
