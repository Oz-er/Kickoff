package com.footballmanager.ui.view;

import com.footballmanager.application.dto.PlayerDto;
import com.footballmanager.application.dto.SavePlayerRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.application.exception.ValidationException;
import com.footballmanager.ui.dialog.UiDialogs;
import com.footballmanager.ui.viewmodel.PlayerViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

public final class PlayerView extends VBox {
    private final PlayerViewModel viewModel;
    private final UiDialogs dialogs = new UiDialogs();
    private final ObservableList<PlayerDto> items = FXCollections.observableArrayList();
    private final TableView<PlayerDto> table = new TableView<>(items);
    private final ComboBox<TeamFilter> teamFilter = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final Label countLabel = new Label();

    public PlayerView(PlayerViewModel viewModel) {
        this.viewModel = viewModel;
        setPadding(new Insets(32));
        setSpacing(18);
        getStyleClass().add("page-content");

        PageHeaderView header = new PageHeaderView("Players", "Maintain squads and filter players by their registered team.");
        HBox actions = createActions();
        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(header, actions, table);
        loadTeamFilter();
        reload();
    }

    private HBox createActions() {
        teamFilter.setPromptText("All teams");
        teamFilter.setPrefWidth(190);
        teamFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(TeamFilter item) {
                return item == null ? "All teams" : item.label();
            }

            @Override
            public TeamFilter fromString(String value) {
                return null;
            }
        });
        teamFilter.valueProperty().addListener((observable, oldValue, newValue) -> reload());

        searchField.setPromptText("Search player name");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> reload());

        Button addButton = new Button("Add player");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> showEditor(null));

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("secondary-button");
        editButton.disableProperty().bind(Bindings.isNull(table.getSelectionModel().selectedItemProperty()));
        editButton.setOnAction(event -> showEditor(table.getSelectionModel().getSelectedItem()));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.disableProperty().bind(Bindings.isNull(table.getSelectionModel().selectedItemProperty()));
        deleteButton.setOnAction(event -> deleteSelected());

        countLabel.getStyleClass().add("record-count");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(10, countLabel, spacer, teamFilter, searchField, addButton, editButton, deleteButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("action-strip");
        return bar;
    }

    private void configureTable() {
        TableColumn<PlayerDto, String> nameColumn = textColumn("Player", PlayerDto::fullName, 230);
        TableColumn<PlayerDto, String> teamColumn = textColumn("Team", PlayerDto::teamName, 220);
        TableColumn<PlayerDto, Number> shirtColumn = new TableColumn<>("Shirt");
        shirtColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().shirtNumber()));
        shirtColumn.setPrefWidth(80);
        TableColumn<PlayerDto, String> positionColumn = textColumn(
                "Position",
                player -> player.position() == null ? "Not provided" : player.position(),
                180
        );
        table.getColumns().setAll(List.of(nameColumn, teamColumn, shirtColumn, positionColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No players match the current filters."));
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                showEditor(table.getSelectionModel().getSelectedItem());
            }
        });
    }

    private TableColumn<PlayerDto, String> textColumn(
            String title,
            java.util.function.Function<PlayerDto, String> value,
            double width
    ) {
        TableColumn<PlayerDto, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void loadTeamFilter() {
        try {
            List<TeamFilter> options = viewModel.availableTeams().stream()
                    .map(team -> new TeamFilter(team.id(), team.name()))
                    .toList();
            teamFilter.getItems().setAll(new TeamFilter(null, "All teams"));
            teamFilter.getItems().addAll(options);
            teamFilter.getSelectionModel().selectFirst();
        } catch (RuntimeException exception) {
            dialogs.showError(window(), "Could not load team filters", exception);
        }
    }

    private void reload() {
        try {
            TeamFilter selected = teamFilter.getValue();
            Long teamId = selected == null ? null : selected.teamId();
            items.setAll(viewModel.filter(teamId, searchField.getText()));
            updateCount();
        } catch (RuntimeException exception) {
            dialogs.showError(window(), "Could not load players", exception);
        }
    }

    private void showEditor(PlayerDto existing) {
        List<TeamDto> availableTeams;
        try {
            availableTeams = viewModel.availableTeams();
        } catch (RuntimeException exception) {
            dialogs.showError(window(), "Could not load teams", exception);
            return;
        }
        if (availableTeams.isEmpty()) {
            dialogs.showError(window(), "Cannot add player", new ValidationException("Create a team before adding players"));
            return;
        }

        PlayerForm values = existing == null
                ? new PlayerForm(availableTeams.getFirst(), "", "", "")
                : new PlayerForm(
                        findTeam(availableTeams, existing.teamId()),
                        existing.fullName(),
                        Integer.toString(existing.shirtNumber()),
                        textOrEmpty(existing.position())
                );
        while (true) {
            Optional<PlayerForm> response = createEditor(existing, values, availableTeams).showAndWait();
            if (response.isEmpty()) {
                return;
            }
            values = response.get();
            try {
                int shirtNumber = Integer.parseInt(values.shirtNumber().trim());
                PlayerDto saved = viewModel.save(
                        existing == null ? null : existing.id(),
                        new SavePlayerRequest(
                                values.team().id(),
                                values.fullName(),
                                shirtNumber,
                                values.position()
                        )
                );
                items.setAll(viewModel.players());
                updateCount();
                table.getSelectionModel().select(saved);
                return;
            } catch (NumberFormatException exception) {
                dialogs.showError(window(), "Could not save player", new ValidationException("Shirt number must be a whole number from 1 to 99"));
            } catch (RuntimeException exception) {
                dialogs.showError(window(), "Could not save player", exception);
            }
        }
    }

    private Dialog<PlayerForm> createEditor(PlayerDto existing, PlayerForm values, List<TeamDto> availableTeams) {
        Dialog<PlayerForm> dialog = new Dialog<>();
        dialog.initOwner(window());
        dialog.setTitle(existing == null ? "Add player" : "Edit player");
        dialog.setHeaderText(existing == null ? "Create a player record" : "Update " + existing.fullName());
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        ComboBox<TeamDto> team = new ComboBox<>(FXCollections.observableArrayList(availableTeams));
        team.setMaxWidth(Double.MAX_VALUE);
        team.setConverter(teamConverter());
        team.getSelectionModel().select(values.team());
        TextField name = new TextField(values.fullName());
        TextField shirt = new TextField(values.shirtNumber());
        TextField position = new TextField(values.position());
        name.setPromptText("Required");
        shirt.setPromptText("1 to 99");
        position.setPromptText("Optional");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Team"), team);
        form.addRow(1, new Label("Player name"), name);
        form.addRow(2, new Label("Shirt number"), shirt);
        form.addRow(3, new Label("Position"), position);
        GridPane.setHgrow(team, Priority.ALWAYS);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(shirt, Priority.ALWAYS);
        GridPane.setHgrow(position, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == saveButton
                ? new PlayerForm(team.getValue(), name.getText(), shirt.getText(), position.getText())
                : null);
        return dialog;
    }

    private StringConverter<TeamDto> teamConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(TeamDto team) {
                return team == null ? "" : team.name() + " (" + team.shortCode() + ")";
            }

            @Override
            public TeamDto fromString(String value) {
                return null;
            }
        };
    }

    private void deleteSelected() {
        PlayerDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !dialogs.confirmDelete(window(), selected.fullName(), "player")) {
            return;
        }
        try {
            viewModel.delete(selected.id());
            items.setAll(viewModel.players());
            updateCount();
        } catch (RuntimeException exception) {
            dialogs.showError(window(), "Could not delete player", exception);
        }
    }

    private TeamDto findTeam(List<TeamDto> teams, long teamId) {
        return teams.stream()
                .filter(team -> team.id() == teamId)
                .findFirst()
                .orElseThrow(() -> new ValidationException("The player's team is no longer available"));
    }

    private void updateCount() {
        countLabel.setText(items.size() + (items.size() == 1 ? " player" : " players"));
    }

    private javafx.stage.Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record TeamFilter(Long teamId, String label) {
    }

    private record PlayerForm(TeamDto team, String fullName, String shirtNumber, String position) {
    }
}
