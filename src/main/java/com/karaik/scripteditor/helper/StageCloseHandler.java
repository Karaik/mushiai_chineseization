package com.karaik.scripteditor.helper;

import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.function.Supplier;

public class StageCloseHandler {

    public static void attach(Stage stage, Supplier<Boolean> isModifiedSupplier, Runnable onSave, Runnable onClose)
    {
        stage.setOnCloseRequest(event -> {
            if (!isModifiedSupplier.get()) {
                onClose.run();
                return;
            }

            event.consume(); // 拦截默认关闭行为

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("保存文件");
            alert.setHeaderText("文件已被修改");
            alert.setContentText("文件尚未保存，是否保存后关闭？");

            ButtonType save = new ButtonType("保存");
            ButtonType discard = new ButtonType("不保存");
            ButtonType cancel = new ButtonType("取消");
            alert.getButtonTypes().setAll(save, discard, cancel);

            alert.setOnShown(e -> {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                if (alertStage != null) {
                    alertStage.setAlwaysOnTop(stage.isAlwaysOnTop());
                }
            });

            alert.showAndWait().ifPresent(response -> {
                if (response == save) {
                    onSave.run();
                    onClose.run();
                } else if (response == discard) {
                    onClose.run();
                }
            });
        });
    }
}
