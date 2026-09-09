package com.footballmanager;

import com.footballmanager.application.ApplicationContext;
import com.footballmanager.application.exception.ApplicationException;
import com.footballmanager.ui.view.MainView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class App extends Application {
    private static final double DEFAULT_WIDTH = 1100;
    private static final double DEFAULT_HEIGHT = 720;
    private static final double MINIMUM_WIDTH = 900;
    private static final double MINIMUM_HEIGHT = 600;

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(createRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add(App.class.getResource("/styles/application.css").toExternalForm());
        stage.setTitle("Football Tournament Manager");
        stage.setMinWidth(MINIMUM_WIDTH);
        stage.setMinHeight(MINIMUM_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    private javafx.scene.Parent createRoot() {
        try {
            ApplicationContext context = ApplicationContext.createDefault();
            context.initialize();
            return new MainView();
        } catch (ApplicationException exception) {
            return createStartupFailure(exception.getMessage());
        }
    }

    private VBox createStartupFailure(String message) {
        Label title = new Label("Application could not start");
        title.getStyleClass().add("page-title");
        Label detail = new Label(message);
        detail.setWrapText(true);
        VBox panel = new VBox(12, title, detail);
        panel.setPadding(new Insets(32));
        return panel;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
