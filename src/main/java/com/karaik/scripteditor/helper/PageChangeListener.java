package com.karaik.scripteditor.helper;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Pagination;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class PageChangeListener {

    private static long lastChangeTime = 0;
    private static final long COOLDOWN = 500;
    private static boolean rollbackInProgress = false;

    public static void attach(
            Pagination pagination,
            Supplier<Boolean> isInitializing,
            Supplier<Boolean> isModified,
            Runnable saveFile,
            IntConsumer saveLastPageIndex,
            BooleanSupplier canChangePage,
            AtomicBoolean warningShown
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
            boolean tooFast = (now - lastChangeTime) < COOLDOWN;

            if (!canChangePage.getAsBoolean() || tooFast) {
                rollbackInProgress = true;
                pagination.setCurrentPageIndex(oldVal.intValue());

                if (warningShown.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "别翻页太快，会崩的(～￣(OO)￣)ブ");
                        alert.setTitle("警告");
                        alert.setHeaderText(null);
                        alert.setOnCloseRequest(e -> warningShown.set(false));
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
