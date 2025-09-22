package com.karaik.scripteditor.helper;

import com.karaik.scripteditor.controller.EditorController;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.function.Supplier;

public class StageCloseHandler {

    public static void attach(Stage stage,
                              Supplier<Boolean> isModifiedSupplier,
                              Runnable onSave,
                              Runnable onClose,
                              EditorController controller) {
        stage.setOnCloseRequest(event -> {
            Runnable historyTask = () -> {
                if (controller != null) {
                    HistoryManager.storeSnapshot(controller.getCurrentFile());
                }
            };

            if (!isModifiedSupplier.get()) {
                historyTask.run();
                onClose.run();
                return;
            }

            event.consume();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("保存文件");
            alert.setHeaderText("文件已被修改");
            alert.setContentText("文件尚未保存，是否保存后关闭？");

            ButtonType save = new ButtonType("保存");
            ButtonType discard = new ButtonType("不保存");
            ButtonType cancel = new ButtonType("取消");
            alert.getButtonTypes().setAll(save, discard, cancel);

            if (controller != null) {
                controller.configureAlertOnTop(alert);
            } else if (stage != null) {
                alert.initOwner(stage);
                if (stage.isAlwaysOnTop()) {
                    alert.setOnShown(e -> {
                        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                        if (alertStage != null) {
                            alertStage.setAlwaysOnTop(true);
                        }
                    });
                }
            }

            alert.showAndWait().ifPresent(response -> {
                if (response == save) {
                    onSave.run();
                    historyTask.run();
                    onClose.run();
                } else if (response == discard) {
                    historyTask.run();
                    onClose.run();
                }
            });
        });
    }
}