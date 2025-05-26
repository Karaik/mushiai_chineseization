package com.karaik.scripteditor.helper;

import javafx.scene.control.Alert;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.BooleanSupplier;

public class PageJumpHandler {

    public static void handle(TextField input, Pagination pagination, Stage stage, BooleanSupplier canChangePage) {
        if (input == null || pagination == null || input.getText().isEmpty()) return;

        if (!canChangePage.getAsBoolean()) {
            showAlert("别翻页太快，会崩的(～￣(OO)￣)ブ", stage);
            return;
        }

        try {
            int targetPage = Integer.parseInt(input.getText()) - 1;
            if (targetPage >= 0 && targetPage < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(targetPage);
            } else {
                showAlert("页码超出范围。", stage);
            }
        } catch (NumberFormatException e) {
            showAlert("输入无效，请输入有效的页码。", stage);
        }

        input.clear();
    }

    private static void showAlert(String message, Stage stage) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        if (stage != null) {
            alert.initOwner(stage);
            alert.setOnShown(e -> {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                if (alertStage != null) {
                    alertStage.setAlwaysOnTop(stage.isAlwaysOnTop());
                }
            });
        }
        alert.showAndWait();
    }
}