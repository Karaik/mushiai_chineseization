package com.karaik.scripteditor.helper;

import com.karaik.scripteditor.controller.EditorController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Pagination;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

import static com.karaik.scripteditor.controller.EditorController.PAGE_CHANGE_COOLDOWN;

public class PageChangeListener {

    private static long lastChangeTime = 0;
    private static boolean rollbackInProgress = false;

    public static void attach(
            Pagination pagination,
            Supplier<Boolean> isInitializing,
            Supplier<Boolean> isModified,
            Runnable saveFile,
            IntConsumer saveLastPageIndex,
            BooleanSupplier canChangePage,
            AtomicBoolean warningShown,
            EditorController controller
    ) {
        pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (rollbackInProgress) {
                rollbackInProgress = false;
                return;
            }

            if (oldVal == null || newVal == null || oldVal.intValue() == newVal.intValue()) return;


            if (isInitializing.get()) {
                saveLastPageIndex.accept(newVal.intValue());
                if (isModified.get()) saveFile.run();
                lastChangeTime = System.currentTimeMillis();
                return;
            }

            long now = System.currentTimeMillis();
            boolean tooFast = (now - lastChangeTime) < PAGE_CHANGE_COOLDOWN;

            if (!canChangePage.getAsBoolean() || tooFast) {
                rollbackInProgress = true;
                Platform.runLater(() -> pagination.setCurrentPageIndex(oldVal.intValue()));

                if (warningShown.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "别翻页太快，会崩的(～￣(OO)￣)ブ");
                        alert.setTitle("警告");
                        alert.setHeaderText(null);
                        alert.setOnCloseRequest(e -> warningShown.set(false));

                        if (controller != null) {
                            controller.configureAlertOnTop(alert);
                        } else {
                            if (pagination != null && pagination.getScene() != null && pagination.getScene().getWindow() instanceof Stage) {
                                Stage ownerStage = (Stage) pagination.getScene().getWindow();
                                alert.initOwner(ownerStage);
                                if (ownerStage.isAlwaysOnTop()) {
                                    alert.setOnShown(event -> {
                                        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                                        if (alertStage != null) {
                                            alertStage.setAlwaysOnTop(true);
                                        }
                                    });
                                }
                            }
                        }

                        alert.show();
                    });
                }
                return;
            }

            if (isModified.get()) {
                saveFile.run();
            }

            saveLastPageIndex.accept(newVal.intValue());
            lastChangeTime = now;
        });
    }
}