package com.footballmanager.ui.view;

import com.footballmanager.application.dto.MatchDto;
import com.footballmanager.application.dto.TournamentDto;
import com.footballmanager.application.event.MatchResultChangedEvent;
import com.footballmanager.domain.model.MatchStatus;
import com.footballmanager.ui.dialog.UiDialogs;
import com.footballmanager.ui.viewmodel.FixturesViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class FixturesView extends VBox {
    private final FixturesViewModel viewModel;
    private final UiDialogs dialogs = new UiDialogs();
    
    private final ObservableList<TournamentDto> tournaments = FXCollections.observableArrayList();
    private final ComboBox<TournamentDto> tournamentCombo = new ComboBox<>(tournaments);
    
    private final ObservableList<Integer> rounds = FXCollections.observableArrayList();
    private final ComboBox<Integer> roundCombo = new ComboBox<>(rounds);
    
    private final ComboBox<MatchStatus> statusCombo = new ComboBox<>();
    
    private final ObservableList<MatchDto> matches = FXCollections.observableArrayList();
    private final TableView<MatchDto> table = new TableView<>(matches);
    
    private final Button undoBtn = new Button("Undo Last Result");
    
    private final Consumer<Object> eventListener = event -> {
        if (event instanceof MatchResultChangedEvent) {
            reloadMatches();
            updateUndoButton();
        }
    };

    public FixturesView(FixturesViewModel viewModel, com.footballmanager.application.event.ApplicationEventPublisher eventPublisher) {
        this.viewModel = viewModel;
        setPadding(new Insets(32));
        setSpacing(18);
        getStyleClass().add("page-content");

        PageHeaderView header = new PageHeaderView("Fixtures & Results", "Manage schedule, record match results, and monitor progression.");
        
        HBox filters = createFilters();
        configureTable();
        
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(header, filters, table);
        
        eventPublisher.subscribe(eventListener);
        
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                eventPublisher.unsubscribe(eventListener);
            }
        });
        
        reloadTournaments();
    }

    private Window window() {
        return getScene() != null ? getScene().getWindow() : null;
    }

    private HBox createFilters() {
        tournamentCombo.setPromptText("Tournament");
        tournamentCombo.setPrefWidth(200);
        tournamentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TournamentDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        tournamentCombo.setButtonCell(tournamentCombo.getCellFactory().call(null));
        tournamentCombo.valueProperty().addListener((obs, oldVal, newVal) -> onTournamentSelected(newVal));
        
        roundCombo.setPromptText("All Rounds");
        roundCombo.getItems().add(null);
        roundCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "All Rounds" : "Round " + item));
            }
        });
        roundCombo.setButtonCell(roundCombo.getCellFactory().call(null));
        roundCombo.valueProperty().addListener((obs, oldVal, newVal) -> reloadMatches());
        
        statusCombo.setPromptText("All Statuses");
        statusCombo.getItems().add(null);
        statusCombo.getItems().addAll(MatchStatus.values());
        statusCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MatchStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "All Statuses" : item.name()));
            }
        });
        statusCombo.setButtonCell(statusCombo.getCellFactory().call(null));
        statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> reloadMatches());
        
        undoBtn.setOnAction(e -> {
            try {
                viewModel.undoResult();
            } catch (Exception ex) {
                dialogs.showError(window(), "Cannot undo", ex);
            }
        });
        undoBtn.setDisable(true);
        
        HBox bar = new HBox(10, new Label("Filter:"), tournamentCombo, roundCombo, statusCombo, undoBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void configureTable() {
        TableColumn<MatchDto, String> matchCol = new TableColumn<>("Match");
        matchCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().homeTeamName() + " vs " + data.getValue().awayTeamName()
        ));
        matchCol.setPrefWidth(300);
        
        TableColumn<MatchDto, String> roundCol = new TableColumn<>("Round");
        roundCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().roundNumber())));
        
        TableColumn<MatchDto, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));
        
        TableColumn<MatchDto, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(data -> {
            MatchDto m = data.getValue();
            if (m.homeScore() == null || m.awayScore() == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(m.homeScore() + " - " + m.awayScore());
        });
        
        TableColumn<MatchDto, String> progCol = new TableColumn<>("Progression");
        progCol.setCellValueFactory(data -> {
            MatchDto m = data.getValue();
            if (m.nextMatchId() != null && m.nextMatchSlot() != null) {
                return new SimpleStringProperty("Winner plays as " + m.nextMatchSlot() + " in Match " + m.nextMatchId());
            }
            return new SimpleStringProperty("");
        });
        progCol.setPrefWidth(250);
        
        table.getColumns().setAll(List.of(roundCol, matchCol, statusCol, scoreCol, progCol));
        table.setPlaceholder(new Label("Select a tournament to view fixtures"));
        
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                showScoreEditor(table.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void reloadTournaments() {
        tournaments.setAll(viewModel.loadTournaments());
    }
    
    private void onTournamentSelected(TournamentDto t) {
        if (t != null) {
            viewModel.setFilters(t.id(), roundCombo.getValue(), statusCombo.getValue());
            rounds.setAll(viewModel.getAvailableRounds());
            rounds.add(0, null); // Add 'All' option
        }
        reloadMatches();
    }
    
    private void reloadMatches() {
        viewModel.setFilters(
                tournamentCombo.getValue() != null ? tournamentCombo.getValue().id() : null,
                roundCombo.getValue(),
                statusCombo.getValue()
        );
        matches.setAll(viewModel.loadMatches());
        updateUndoButton();
    }
    
    private void updateUndoButton() {
        undoBtn.setDisable(!viewModel.canUndo());
    }

    private void showScoreEditor(MatchDto match) {
        if (match.status() == MatchStatus.PENDING) {
            dialogs.showError(window(), "Cannot edit score", new RuntimeException("Match is pending. Both teams must be determined first."));
            return;
        }

        ScoreForm values = new ScoreForm(
                match.homeScore() != null ? String.valueOf(match.homeScore()) : "",
                match.awayScore() != null ? String.valueOf(match.awayScore()) : ""
        );
        
        while (true) {
            Optional<ScoreForm> response = createEditor(match, values).showAndWait();
            if (response.isEmpty()) {
                return;
            }
            values = response.get();
            try {
                int homeScore = Integer.parseInt(values.homeScore());
                int awayScore = Integer.parseInt(values.awayScore());
                
                if (match.status() == MatchStatus.COMPLETED) {
                    viewModel.correctResult(match.id(), homeScore, awayScore);
                } else {
                    viewModel.recordResult(match.id(), homeScore, awayScore);
                }
                return;
            } catch (NumberFormatException exception) {
                dialogs.showError(window(), "Invalid input", new RuntimeException("Scores must be valid integers"));
            } catch (RuntimeException exception) {
                dialogs.showError(window(), "Could not save result", exception);
            }
        }
    }
    
    private Dialog<ScoreForm> createEditor(MatchDto match, ScoreForm values) {
        Dialog<ScoreForm> dialog = new Dialog<>();
        dialog.setTitle(match.status() == MatchStatus.COMPLETED ? "Correct Result" : "Record Result");
        dialog.setHeaderText("Enter scores for " + match.homeTeamName() + " vs " + match.awayTeamName());
        
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField homeScoreField = new TextField(values.homeScore());
        TextField awayScoreField = new TextField(values.awayScore());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label(match.homeTeamName() + " Score:"), 0, 0);
        grid.add(homeScoreField, 1, 0);
        grid.add(new Label(match.awayTeamName() + " Score:"), 0, 1);
        grid.add(awayScoreField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == saveButton) {
                return new ScoreForm(homeScoreField.getText(), awayScoreField.getText());
            }
            return null;
        });

        return dialog;
    }
    
    private record ScoreForm(String homeScore, String awayScore) {}
}
