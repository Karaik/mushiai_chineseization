package com.karaik.scripteditor.helper;

import com.karaik.scripteditor.controller.EditorController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class KeyboardNavigationHelper {

    public static void setupKeyboardShortcuts(Stage stage, EditorController controller) {
        if (stage == null || stage.getScene() == null) return;

        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Ctrl+S 快捷保存
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                controller.getFileHandlerController().saveFile();
                event.consume();
                return;
            }

            // 忽略 TextArea 中的方向键
            if (event.getTarget() instanceof javafx.scene.control.TextArea) return;

            Pagination pagination = controller.getPagination();
            if (pagination == null) return;

            int currentPage = pagination.getCurrentPageIndex();
            int maxPage = pagination.getPageCount() - 1;

            if (!controller.canChangePage()) {
                if (controller.getWarningShown().compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "别翻页太快，会崩的(～￣(OO)￣)ブ");
                        controller.configureAlertOnTop(alert);
                        alert.setOnCloseRequest(e -> controller.getWarningShown().set(false));
                        alert.show();
                    });
                }
                event.consume();
                return;
            }

            boolean changed = false;
            if (event.getCode() == KeyCode.LEFT && currentPage > 0) {
                pagination.setCurrentPageIndex(currentPage - 1);
                changed = true;
            } else if (event.getCode() == KeyCode.RIGHT && currentPage < maxPage) {
                pagination.setCurrentPageIndex(currentPage + 1);
                changed = true;
            } else if (event.getCode() == KeyCode.DOWN) {
                ScrollPane scrollPane = controller.getMainContentScrollPane();
                if (scrollPane != null && scrollPane.getContent() != null) {
                    double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
                    double viewportHeight = scrollPane.getViewportBounds().getHeight();

                    if (contentHeight <= viewportHeight && currentPage < maxPage) {
                        pagination.setCurrentPageIndex(currentPage + 1);
                        changed = true;
                    } else if (contentHeight > viewportHeight) {
                        double currentV = scrollPane.getVvalue();
                        double delta = EditorController.KEYBOARD_SCROLL_AMOUNT / (contentHeight - viewportHeight);
                        scrollPane.setVvalue(Math.min(currentV + delta, 1.0));
                    }
                }
                event.consume();
            }

            if (changed) event.consume();
        });
    }
}
