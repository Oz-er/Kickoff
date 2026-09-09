package com.footballmanager.ui.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class PageHeaderView extends VBox {
    public PageHeaderView(String titleText, String subtitleText) {
        setSpacing(4);
        Label title = new Label(titleText);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("page-subtitle");
        getChildren().addAll(title, subtitle);
    }
}
