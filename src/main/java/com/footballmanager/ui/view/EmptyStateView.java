package com.footballmanager.ui.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class EmptyStateView extends VBox {
    public EmptyStateView(String title, String message) {
        setAlignment(Pos.CENTER);
        setSpacing(8);
        getStyleClass().add("empty-state");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("empty-state-message");
        getChildren().addAll(titleLabel, messageLabel);
    }
}
