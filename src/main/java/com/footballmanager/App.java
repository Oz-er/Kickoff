package com.footballmanager;

import com.footballmanager.persistence.DatabaseInitializer;
import com.footballmanager.persistence.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

public final class App extends Application {
    private final Label pageTitle = new Label();

    @Override
    public void start(Stage stage) {
        DatabaseManager databaseManager = new DatabaseManager(Path.of("data", "football_manager.db"));
        new DatabaseInitializer(databaseManager).initialize();

        BorderPane root = new BorderPane();
        root.setLeft(createNavigation());
        root.setCenter(createWelcomePanel());

        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(App.class.getResource("/styles/application.css").toExternalForm());
        stage.setTitle("Football Tournament Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createNavigation() {
        VBox navigation = new VBox(8);
        navigation.getStyleClass().add("navigation");
        Label brand = new Label("Tournament Manager");
        brand.getStyleClass().add("brand");
        navigation.getChildren().add(brand);
        List<String> destinations = List.of(
                "Dashboard",
                "Teams",
                "Players",
                "Tournaments",
                "Fixtures & Results",
                "Standings & Reports"
        );
        for (String destination : destinations) {
            Button button = new Button(destination);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> showPlaceholder(destination));
            navigation.getChildren().add(button);
        }
        return navigation;
    }

    private VBox createWelcomePanel() {
        pageTitle.setText("Dashboard");
        pageTitle.getStyleClass().add("page-title");
        Label message = new Label("The starter is ready. Follow prompt.md to build each feature in order.");
        VBox panel = new VBox(12, pageTitle, message);
        panel.setPadding(new Insets(32));
        return panel;
    }

    private void showPlaceholder(String destination) {
        pageTitle.setText(destination);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
