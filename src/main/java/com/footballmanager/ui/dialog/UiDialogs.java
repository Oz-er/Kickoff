package com.footballmanager.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class UiDialogs {
    private final UserMessageMapper messageMapper = new UserMessageMapper();

    public void showError(Window owner, String title, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(messageMapper.messageFor(exception));
        alert.showAndWait();
    }

    public boolean confirmDelete(Window owner, String itemName, String itemType) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Delete " + itemType);
        alert.setHeaderText("Delete " + itemName + "?");
        alert.setContentText("This action cannot be undone.");
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
}
