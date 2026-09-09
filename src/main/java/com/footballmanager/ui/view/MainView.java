package com.footballmanager.ui.view;

import com.footballmanager.application.ApplicationContext;
import com.footballmanager.ui.navigation.NavigationView;
import com.footballmanager.ui.navigation.ScreenId;
import com.footballmanager.ui.viewmodel.PlayerViewModel;
import com.footballmanager.ui.viewmodel.TeamViewModel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public final class MainView extends BorderPane {
    private final ApplicationContext context;
    private final NavigationView navigation;

    public MainView(ApplicationContext context) {
        this.context = context;
        this.navigation = new NavigationView(this::showScreen);
        setLeft(navigation);
        showScreen(ScreenId.DASHBOARD);
    }

    private void showScreen(ScreenId screen) {
        if (screen == ScreenId.TEAMS) {
            setCenter(new TeamView(new TeamViewModel(context.teamService())));
        } else if (screen == ScreenId.PLAYERS) {
            setCenter(new PlayerView(new PlayerViewModel(context.playerService(), context.teamService())));
        } else {
            setCenter(createEmptyScreen(screen));
        }
        navigation.select(screen);
    }

    private VBox createEmptyScreen(ScreenId screen) {
        Label title = new Label(screen.title());
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(screen.subtitle());
        subtitle.getStyleClass().add("page-subtitle");

        EmptyStateView emptyState = new EmptyStateView(screen.emptyTitle(), screen.emptyMessage());
        VBox.setVgrow(emptyState, javafx.scene.layout.Priority.ALWAYS);

        VBox content = new VBox(8, title, subtitle, emptyState);
        content.setPadding(new Insets(32));
        content.getStyleClass().add("page-content");
        return content;
    }
}
