package com.footballmanager.ui.view;

import com.footballmanager.application.dto.SaveTeamRequest;
import com.footballmanager.application.dto.TeamDto;
import com.footballmanager.ui.dialog.UiDialogs;
import com.footballmanager.ui.viewmodel.TeamViewModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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

import java.util.Optional;
import java.util.List;

public final class TeamView extends VBox {
    private final TeamViewModel viewModel;
    private final UiDialogs dialogs = new UiDialogs();
    private final ObservableList<TeamDto> items = FXCollections.observableArrayList();
    private final TableView<TeamDto> table = new TableView<>(items);
    private final TextField searchField = new TextField();
    private final Label countLabel = new Label();

    public TeamView(TeamViewModel viewModel) {
        this.viewModel = viewModel;
        setPadding(new Insets(32));
        setSpacing(18);
        getStyleClass().add("page-content");

        PageHeaderView header = new PageHeaderView("Teams", "Register clubs and maintain tournament-ready team records.");
        HBox actions = createActions();
        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(header, actions, table);
        reload();
    }

    private HBox createActions() {
        searchField.setPromptText("Search team name");
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> reload());

        Button addButton = new Button("Add team");
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
        HBox bar = new HBox(10, countLabel, spacer, searchField, addButton, editButton, deleteButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("action-strip");
        return bar;
    }

    private void configureTable() {
        TableColumn<TeamDto, String> nameColumn = textColumn("Team", TeamDto::name, 280);
        TableColumn<TeamDto, String> codeColumn = textColumn("Code", TeamDto::shortCode, 110);
        TableColumn<TeamDto, String> coachColumn = textColumn(
                "Coach",
                team -> team.coachName() == null ? "Not provided" : team.coachName(),
                260
        );
        table.getColumns().setAll(List.of(nameColumn, codeColumn, coachColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No teams match the current search."));
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                showEditor(table.getSelectionModel().getSelectedItem());
            }
        });
    }

    private TableColumn<TeamDto, String> textColumn(
            String title,
            java.util.function.Function<TeamDto, String> value,
            double width
    ) {
        TableColumn<TeamDto, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void reload() {
        try {
            items.setAll(viewModel.search(searchField.getText()));
            countLabel.setText(items.size() + (items.size() == 1 ? " team" : " teams"));
        } catch (RuntimeException exception) {
            dialogs.showError(window(), "Could not load teams", exception);
        }
    }

    private void showEditor(TeamDto existing) {
        TeamForm initial = existing == null
                ? new TeamForm("", "", "")
                : new TeamForm(existing.name(), existing.shortCode(), textOrEmpty(existing.coachName()));
        TeamForm values = initial;
        while (true) {
            Optional<TeamForm> response = createEditor(existing, values).showAndWait();
            if (response.isEmpty()) {
                return;
            }
            values = response.get();
            try {
                TeamDto saved = viewModel.save(
                        existing == null ? null : existing.id(),
                        new SaveTeamRequest(values.name(), values.shortCode(), values.coachName())
                );
                items.setAll(viewModel.teams());
                updateCount();
                table.getSelectionModel().select(saved);
                return;
            } catch (RuntimeException exception) {
                dialogs.showError(window(), "Could not save team", exception);
            }
        }
    }

    private Dialog<TeamForm> createEditor(TeamDto existing, TeamForm values) {
        Dialog<TeamForm> dialog = new Dialog<>();
        dialog.initOwner(window());
        dialog.setTitle(existing == null ? "Add team" : "Edit team");
        dialog.setHeaderText(existing == null ? "Create a team record" : "Update " + existing.name());
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField name = new TextField(values.name());
        TextField code = new TextField(values.shortCode());
        TextField coach = new TextField(values.coachName());
        name.setPromptText("Required");
        code.setPromptText("2 to 5 characters");
        coach.setPromptText("Optional");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Team name"), name);
        form.addRow(1, new Label("Short code"), code);
        form.addRow(2, new Label("Coach name"), coach);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(code, Priority.ALWAYS);
        GridPane.setHgrow(coach, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == saveButton
                ? new TeamForm(name.getText(), code.getText(), coach.getText())
                : null);
        return dialog;
    }

    private void deleteSelected() {
        TeamDto selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !dialogs.confirmDelete(window(), selected.name(), "team")) {
            return;
        }
        try {
            viewModel.delete(selected.id());
            items.setAll(viewModel.teams());
            updateCount();
        } catch (RuntimeException exception) {
            dialogs.showError(window(), "Could not delete team", exception);
        }
    }

    private void updateCount() {
        countLabel.setText(items.size() + (items.size() == 1 ? " team" : " teams"));
    }

    private javafx.stage.Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record TeamForm(String name, String shortCode, String coachName) {
    }
}
