package com.footballmanager.ui.view;

import com.footballmanager.application.dto.CreateTournamentRequest;
import com.footballmanager.application.dto.RegisteredTeamDto;
import com.footballmanager.application.dto.SchedulePreviewDto;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.dto.TournamentRegistrationRequest;
import com.footballmanager.application.exception.BusinessRuleException;
import com.footballmanager.domain.model.TournamentFormat;
import com.footballmanager.domain.model.TournamentStatus;
import com.footballmanager.ui.dialog.UiDialogs;
import com.footballmanager.ui.viewmodel.TournamentViewModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public final class TournamentView extends VBox {
    private final TournamentViewModel viewModel;
    private final UiDialogs dialogs = new UiDialogs();
    
    private final ObservableList<TournamentDto> tournaments = FXCollections.observableArrayList();
    private final ComboBox<TournamentDto> tournamentSelector = new ComboBox<>(tournaments);
    
    private final ObjectProperty<TournamentDto> currentTournament = new SimpleObjectProperty<>();
    
    private final VBox workflowContainer = new VBox(20);
    
    // Workflow sections
    private final VBox draftSection = new VBox(10);
    private final VBox schedulingSection = new VBox(10);
    private final VBox activeSection = new VBox(10);
    
    // Draft components
    private final ObservableList<TeamDto> availableTeams = FXCollections.observableArrayList();
    private final ObservableList<RegisteredTeamDto> registeredTeams = FXCollections.observableArrayList();
    private final ListView<TeamDto> availableList = new ListView<>(availableTeams);
    private final ListView<RegisteredTeamDto> registeredList = new ListView<>(registeredTeams);
    private final Label teamCountGuidance = new Label();
    
    // Scheduling components
    private final ObservableList<com.footballmanager.application.dto.FixturePreviewDto> previewMatches = FXCollections.observableArrayList();
    private final TableView<com.footballmanager.application.dto.FixturePreviewDto> previewTable = new TableView<>(previewMatches);
    private SchedulePreviewDto currentPreview = null;

    public TournamentView(TournamentViewModel viewModel) {
        this.viewModel = viewModel;
        setPadding(new Insets(32));
        setSpacing(24);
        getStyleClass().add("page-content");

        PageHeaderView header = new PageHeaderView("Tournaments", "Setup tournaments, assign teams, and generate fixtures.");
        
        HBox topBar = createTopBar();
        
        workflowContainer.getChildren().addAll(draftSection, schedulingSection, activeSection);
        VBox.setVgrow(workflowContainer, Priority.ALWAYS);
        
        getChildren().addAll(header, topBar, workflowContainer);
        
        setupDraftSection();
        setupSchedulingSection();
        setupActiveSection();
        
        currentTournament.addListener((obs, oldVal, newVal) -> updateWorkflow(newVal));
        
        reloadTournaments(null);
    }

    private javafx.stage.Window window() {
        return getScene() != null ? getScene().getWindow() : null;
    }

    private HBox createTopBar() {
        tournamentSelector.setPromptText("Select a tournament");
        tournamentSelector.setPrefWidth(300);
        tournamentSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TournamentDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " (" + item.status() + ")");
            }
        });
        tournamentSelector.setButtonCell(tournamentSelector.getCellFactory().call(null));
        tournamentSelector.valueProperty().bindBidirectional(currentTournament);
        
        Button createBtn = new Button("Create Draft");
        createBtn.getStyleClass().add("primary-button");
        createBtn.setOnAction(e -> showEditor(null));
        
        Button editBtn = new Button("Edit Draft");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> showEditor(currentTournament.get()));
        editBtn.disableProperty().bind(
            currentTournament.isNull().or(
                javafx.beans.binding.Bindings.createBooleanBinding(
                    () -> currentTournament.get() != null && currentTournament.get().status() != TournamentStatus.DRAFT,
                    currentTournament
                )
            )
        );
        
        HBox bar = new HBox(16, new Label("Active Tournament:"), tournamentSelector, createBtn, editBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void setupDraftSection() {
        draftSection.getStyleClass().add("card");
        draftSection.setPadding(new Insets(16));
        
        Label title = new Label("1. Draft & Registration");
        title.getStyleClass().add("section-title");
        
        availableList.setPrefHeight(200);
        availableList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TeamDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " [" + item.shortCode() + "]");
            }
        });
        
        registeredList.setPrefHeight(200);
        registeredList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RegisteredTeamDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "(Seed " + item.seedNumber() + ") " + item.teamName());
            }
        });
        
        Button registerBtn = new Button("Register >>");
        registerBtn.setOnAction(e -> registerSelectedTeam());
        
        Button unregisterBtn = new Button("<< Unregister");
        unregisterBtn.setOnAction(e -> unregisterSelectedTeam());
        
        VBox controls = new VBox(10, registerBtn, unregisterBtn);
        controls.setAlignment(Pos.CENTER);
        
        HBox lists = new HBox(16, 
            new VBox(5, new Label("Available Teams"), availableList),
            controls,
            new VBox(5, new Label("Registered Teams"), registeredList)
        );
        HBox.setHgrow(lists.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lists.getChildren().get(2), Priority.ALWAYS);
        
        teamCountGuidance.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        
        Button closeRegBtn = new Button("Close Registration");
        closeRegBtn.getStyleClass().add("primary-button");
        closeRegBtn.setOnAction(e -> closeRegistration());
        
        draftSection.getChildren().addAll(title, teamCountGuidance, lists, closeRegBtn);
    }
    
    private void setupSchedulingSection() {
        schedulingSection.getStyleClass().add("card");
        schedulingSection.setPadding(new Insets(16));
        
        Label title = new Label("2. Scheduling & Fixture Generation");
        title.getStyleClass().add("section-title");
        
        TableColumn<com.footballmanager.application.dto.FixturePreviewDto, String> roundCol = new TableColumn<>("Round");
        roundCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty("Round " + data.getValue().roundNumber()));
        
        TableColumn<com.footballmanager.application.dto.FixturePreviewDto, String> matchCol = new TableColumn<>("Match");
        matchCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().homeTeamName() + " vs " + data.getValue().awayTeamName()));
        
        previewTable.getColumns().addAll(roundCol, matchCol);
        previewTable.setPrefHeight(200);
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        Button previewBtn = new Button("Preview Schedule");
        previewBtn.setOnAction(e -> previewSchedule());
        
        Button generateBtn = new Button("Confirm & Generate Fixtures");
        generateBtn.getStyleClass().add("primary-button");
        generateBtn.setOnAction(e -> generateFixtures());
        
        HBox buttons = new HBox(10, previewBtn, generateBtn);
        
        schedulingSection.getChildren().addAll(title, previewTable, buttons);
    }
    
    private void setupActiveSection() {
        activeSection.getStyleClass().add("card");
        activeSection.setPadding(new Insets(16));
        
        Label title = new Label("Tournament is Active");
        title.getStyleClass().add("section-title");
        
        Label info = new Label("Fixtures have been generated. Head over to Fixtures & Results to record matches.");
        
        activeSection.getChildren().addAll(title, info);
    }

    private void updateWorkflow(TournamentDto t) {
        draftSection.setVisible(false);
        draftSection.setManaged(false);
        schedulingSection.setVisible(false);
        schedulingSection.setManaged(false);
        activeSection.setVisible(false);
        activeSection.setManaged(false);
        
        if (t == null) return;
        
        if (t.status() == TournamentStatus.DRAFT) {
            draftSection.setVisible(true);
            draftSection.setManaged(true);
            
            if (t.format() == TournamentFormat.KNOCKOUT) {
                teamCountGuidance.setText("Knockout format requires exactly a power of 2 teams (e.g. 2, 4, 8, 16) to close registration.");
            } else {
                teamCountGuidance.setText("Round Robin format requires at least 2 teams.");
            }
            
            reloadTeams(t.id());
        } else if (t.status() == TournamentStatus.REGISTRATION_CLOSED) {
            schedulingSection.setVisible(true);
            schedulingSection.setManaged(true);
            previewMatches.clear();
            currentPreview = null;
        } else {
            activeSection.setVisible(true);
            activeSection.setManaged(true);
        }
    }

    private void reloadTournaments(Long selectId) {
        tournaments.setAll(viewModel.loadTournaments());
        if (selectId != null) {
            tournaments.stream().filter(t -> t.id() == selectId).findFirst().ifPresent(currentTournament::set);
        }
    }

    private void reloadTeams(long tournamentId) {
        availableTeams.setAll(viewModel.loadAvailableTeams(tournamentId));
        registeredTeams.setAll(viewModel.loadRegisteredTeams(tournamentId));
    }

    private void showEditor(TournamentDto existing) {
        TournamentForm initial = existing == null
                ? new TournamentForm("", TournamentFormat.ROUND_ROBIN, LocalDate.now())
                : new TournamentForm(existing.name(), existing.format(), existing.startDate());
        TournamentForm values = initial;
        while (true) {
            Optional<TournamentForm> response = createEditor(existing, values).showAndWait();
            if (response.isEmpty()) {
                return;
            }
            values = response.get();
            try {
                if (existing == null) {
                    TournamentDto created = viewModel.create(new CreateTournamentRequest(values.name(), values.format(), values.startDate()));
                    reloadTournaments(created.id());
                } else {
                    TournamentDto updated = viewModel.update(existing.id(), new com.footballmanager.application.dto.UpdateTournamentRequest(values.name(), values.format(), values.startDate()));
                    reloadTournaments(updated.id());
                }
                return;
            } catch (RuntimeException exception) {
                dialogs.showError(window(), "Could not save tournament", exception);
            }
        }
    }

    private javafx.scene.control.Dialog<TournamentForm> createEditor(TournamentDto existing, TournamentForm values) {
        javafx.scene.control.Dialog<TournamentForm> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(existing == null ? "Create Tournament" : "Edit Tournament");
        dialog.setHeaderText(existing == null ? "Create a new tournament" : "Update " + existing.name());
        javafx.scene.control.ButtonType saveButton = new javafx.scene.control.ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.control.TextField name = new javafx.scene.control.TextField(values.name());
        ComboBox<TournamentFormat> formatCombo = new ComboBox<>(FXCollections.observableArrayList(TournamentFormat.values()));
        formatCombo.setValue(values.format());
        javafx.scene.control.DatePicker startDatePicker = new javafx.scene.control.DatePicker(values.startDate());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Format:"), 0, 1);
        grid.add(formatCombo, 1, 1);
        grid.add(new Label("Start Date:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        dialog.getDialogPane().setContent(grid);

        // Optional: Ensure cancellation makes no changes by tying button to result converter
        dialog.setResultConverter(b -> {
            if (b == saveButton) {
                return new TournamentForm(name.getText(), formatCombo.getValue(), startDatePicker.getValue());
            }
            return null;
        });

        return dialog;
    }

    private record TournamentForm(String name, TournamentFormat format, LocalDate startDate) {}

    private void registerSelectedTeam() {
        TournamentDto t = currentTournament.get();
        TeamDto selected = availableList.getSelectionModel().getSelectedItem();
        if (t != null && selected != null) {
            TextInputDialog seedDialog = new TextInputDialog(String.valueOf(registeredTeams.size() + 1));
            seedDialog.setTitle("Assign Seed");
            seedDialog.setHeaderText("Enter seed number for " + selected.name() + ":");
            Optional<String> seedStr = seedDialog.showAndWait();
            if (seedStr.isPresent()) {
                try {
                    int seed = Integer.parseInt(seedStr.get());
                    viewModel.registerTeams(t.id(), List.of(new TournamentRegistrationRequest(selected.id(), seed)));
                    reloadTeams(t.id());
                } catch (NumberFormatException ex) {
                    dialogs.showError(window(), "Invalid input", new RuntimeException("Seed must be a number"));
                } catch (Exception ex) {
                    dialogs.showError(window(), "Registration failed", ex);
                }
            }
        }
    }

    private void unregisterSelectedTeam() {
        TournamentDto t = currentTournament.get();
        RegisteredTeamDto selected = registeredList.getSelectionModel().getSelectedItem();
        if (t != null && selected != null) {
            try {
                viewModel.unregisterTeam(t.id(), selected.teamId());
                reloadTeams(t.id());
            } catch (Exception ex) {
                dialogs.showError(window(), "Unregister failed", ex);
            }
        }
    }

    private void closeRegistration() {
        TournamentDto t = currentTournament.get();
        if (t != null) {
            try {
                TournamentDto updated = viewModel.closeRegistration(t.id());
                reloadTournaments(updated.id());
            } catch (Exception ex) {
                dialogs.showError(window(), "Cannot close registration", ex);
            }
        }
    }

    private void previewSchedule() {
        TournamentDto t = currentTournament.get();
        if (t != null) {
            try {
                currentPreview = viewModel.previewSchedule(t.id());
                previewMatches.setAll(currentPreview.fixtures());
            } catch (Exception ex) {
                dialogs.showError(window(), "Preview failed", ex);
            }
        }
    }

    private void generateFixtures() {
        if (currentPreview != null) {
            try {
                viewModel.generateSchedule(currentPreview);
                // Status changed to FIXTURES_GENERATED, reload tournament
                TournamentDto t = currentTournament.get();
                reloadTournaments(t.id());
            } catch (Exception ex) {
                dialogs.showError(window(), "Generation failed", ex);
            }
        }
    }
}
