package com.footballmanager.ui.view;

import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.StandingsRowDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.ui.dialog.UiDialogs;
import com.footballmanager.ui.viewmodel.StandingsViewModel;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public final class StandingsView extends VBox {
    private final StandingsViewModel viewModel;
    private final UiDialogs dialogs = new UiDialogs();

    private final ObservableList<TournamentDto> tournaments = FXCollections.observableArrayList();
    private final ComboBox<TournamentDto> tournamentCombo = new ComboBox<>(tournaments);

    private final VBox standingsPanel = new VBox(12);
    private final VBox knockoutPanel = new VBox(12);
    private final TextArea summaryArea = new TextArea();

    private final ObservableList<StandingsRowDto> standingsRows = FXCollections.observableArrayList();
    private final TableView<StandingsRowDto> standingsTable = new TableView<>(standingsRows);

    private final ObservableList<MatchDto> knockoutMatches = FXCollections.observableArrayList();
    private final TableView<MatchDto> knockoutTable = new TableView<>(knockoutMatches);

    public StandingsView(StandingsViewModel viewModel) {
        this.viewModel = viewModel;
        setPadding(new Insets(32));
        setSpacing(20);
        getStyleClass().add("page-content");

        PageHeaderView header = new PageHeaderView("Standings & Reports", "League tables, knockout progress, and tournament summaries.");

        HBox filterBar = createFilterBar();
        configureStandingsTable();
        configureKnockoutTable();
        configureSummaryArea();

        standingsPanel.getChildren().addAll(new Label("League Standings"), standingsTable);
        standingsPanel.getStyleClass().add("card");
        standingsPanel.setPadding(new Insets(16));
        VBox.setVgrow(standingsTable, Priority.ALWAYS);

        knockoutPanel.getChildren().addAll(new Label("Knockout Progress"), knockoutTable);
        knockoutPanel.getStyleClass().add("card");
        knockoutPanel.setPadding(new Insets(16));
        VBox.setVgrow(knockoutTable, Priority.ALWAYS);

        VBox reportPanel = createReportPanel();

        VBox mainContent = new VBox(16, standingsPanel, knockoutPanel, reportPanel);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, mainContent);

        reloadTournaments();
        clearPanels();
    }

    private Window window() {
        return getScene() != null ? getScene().getWindow() : null;
    }

    private HBox createFilterBar() {
        tournamentCombo.setPromptText("Select a tournament");
        tournamentCombo.setPrefWidth(280);
        tournamentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TournamentDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " (" + item.format() + ")");
            }
        });
        tournamentCombo.setButtonCell(tournamentCombo.getCellFactory().call(null));
        tournamentCombo.valueProperty().addListener((obs, oldVal, newVal) -> onTournamentSelected(newVal));

        HBox bar = new HBox(12, new Label("Tournament:"), tournamentCombo);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void configureStandingsTable() {
        TableColumn<StandingsRowDto, String> teamCol = new TableColumn<>("Team");
        teamCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().teamName()));
        teamCol.setPrefWidth(180);

        TableColumn<StandingsRowDto, Number> pCol = tableIntCol("P", row -> row.played());
        TableColumn<StandingsRowDto, Number> wCol = tableIntCol("W", row -> row.won());
        TableColumn<StandingsRowDto, Number> dCol = tableIntCol("D", row -> row.drawn());
        TableColumn<StandingsRowDto, Number> lCol = tableIntCol("L", row -> row.lost());
        TableColumn<StandingsRowDto, Number> gfCol = tableIntCol("GF", row -> row.goalsFor());
        TableColumn<StandingsRowDto, Number> gaCol = tableIntCol("GA", row -> row.goalsAgainst());
        TableColumn<StandingsRowDto, Number> gdCol = tableIntCol("GD", row -> row.goalDifference());
        TableColumn<StandingsRowDto, Number> ptsCol = tableIntCol("Pts", row -> row.points());

        standingsTable.getColumns().setAll(List.of(teamCol, pCol, wCol, dCol, lCol, gfCol, gaCol, gdCol, ptsCol));
        standingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        standingsTable.setPlaceholder(new Label("No standings data available."));
        standingsTable.setPrefHeight(240);
    }

    private TableColumn<StandingsRowDto, Number> tableIntCol(String title, java.util.function.Function<StandingsRowDto, Integer> getter) {
        TableColumn<StandingsRowDto, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new SimpleIntegerProperty(getter.apply(data.getValue())));
        col.setPrefWidth(50);
        return col;
    }

    private void configureKnockoutTable() {
        TableColumn<MatchDto, String> roundCol = new TableColumn<>("Round");
        roundCol.setCellValueFactory(data -> new SimpleStringProperty("Round " + data.getValue().roundNumber()));
        roundCol.setPrefWidth(80);

        TableColumn<MatchDto, String> matchCol = new TableColumn<>("Match");
        matchCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().homeTeamName() + " vs " + data.getValue().awayTeamName()));
        matchCol.setPrefWidth(250);

        TableColumn<MatchDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));

        TableColumn<MatchDto, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(data -> {
            MatchDto m = data.getValue();
            if (m.homeScore() == null || m.awayScore() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(m.homeScore() + " - " + m.awayScore());
        });

        knockoutTable.getColumns().setAll(List.of(roundCol, matchCol, statusCol, scoreCol));
        knockoutTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        knockoutTable.setPlaceholder(new Label("No knockout matches available."));
        knockoutTable.setPrefHeight(200);
    }

    private void configureSummaryArea() {
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPrefHeight(140);
        summaryArea.setPromptText("Select a tournament to view the summary report.");
    }

    private VBox createReportPanel() {
        Label heading = new Label("Tournament Summary Report");
        heading.getStyleClass().add("section-title");

        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setOnAction(e -> {
            String text = summaryArea.getText();
            if (!text.isBlank()) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(text);
                clipboard.setContent(content);
            }
        });

        Button saveBtn = new Button("Save to File…");
        saveBtn.setOnAction(e -> saveReportToFile());

        HBox btnRow = new HBox(10, copyBtn, saveBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(10, heading, summaryArea, btnRow);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(16));
        return panel;
    }

    private void reloadTournaments() {
        tournaments.setAll(viewModel.loadTournaments());
    }

    private void onTournamentSelected(TournamentDto tournament) {
        clearPanels();
        if (tournament == null) return;

        if (viewModel.isKnockout(tournament)) {
            standingsPanel.setVisible(false);
            standingsPanel.setManaged(false);
            knockoutPanel.setVisible(true);
            knockoutPanel.setManaged(true);
            knockoutMatches.setAll(viewModel.loadKnockoutMatches(tournament.id()));
        } else {
            knockoutPanel.setVisible(false);
            knockoutPanel.setManaged(false);
            standingsPanel.setVisible(true);
            standingsPanel.setManaged(true);
            try {
                standingsRows.setAll(viewModel.loadStandings(tournament.id()));
            } catch (BusinessRuleException ex) {
                dialogs.showError(window(), "Cannot load standings", ex);
                standingsRows.clear();
            }
        }

        try {
            summaryArea.setText(viewModel.generateSummaryReport(tournament.id()));
        } catch (Exception ex) {
            summaryArea.setText("");
        }
    }

    private void clearPanels() {
        standingsRows.clear();
        knockoutMatches.clear();
        summaryArea.clear();
        standingsPanel.setVisible(false);
        standingsPanel.setManaged(false);
        knockoutPanel.setVisible(false);
        knockoutPanel.setManaged(false);
    }

    private void saveReportToFile() {
        String text = summaryArea.getText();
        if (text.isBlank()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Tournament Summary");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        chooser.setInitialFileName("tournament-summary.txt");
        File file = chooser.showSaveDialog(window());
        if (file != null) {
            try {
                Files.writeString(file.toPath(), text);
            } catch (IOException ex) {
                dialogs.showError(window(), "Could not save report", ex);
            }
        }
    }
}
