package com.footballmanager.ui.navigation;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class NavigationView extends VBox {
    private final Map<ScreenId, Button> buttons = new EnumMap<>(ScreenId.class);

    public NavigationView(Consumer<ScreenId> selectionHandler) {
        Objects.requireNonNull(selectionHandler);
        setSpacing(8);
        getStyleClass().add("navigation");

        Label brand = new Label("Kickoff");
        brand.getStyleClass().add("brand");
        Label productName = new Label("Tournament Manager");
        productName.getStyleClass().add("product-name");
        getChildren().addAll(brand, productName);

        for (ScreenId screen : ScreenId.values()) {
            Button button = new Button(screen.title());
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("navigation-button");
            button.setOnAction(event -> selectionHandler.accept(screen));
            buttons.put(screen, button);
            getChildren().add(button);
        }
    }

    public void select(ScreenId selectedScreen) {
        buttons.forEach((screen, button) -> button.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"),
                screen == selectedScreen
        ));
    }
}
