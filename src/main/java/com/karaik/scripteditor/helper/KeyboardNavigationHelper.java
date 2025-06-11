package com.karaik.scripteditor.helper;

import com.karaik.scripteditor.controller.EditorController;
import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
// import javafx.scene.control.skin.VirtualFlow; // 移除对内部API的依赖
// import javafx.scene.control.IndexedCell; // 移除对内部API的依赖
// import javafx.scene.Node; // 如果不再使用 lookup(".virtual-flow")，也可以考虑移除


public class KeyboardNavigationHelper {

    private static final int ITEMS_TO_SCROLL_PER_KEY_PRESS = 1; // 每次按键滚动的条目数，可调整

    public static void setupKeyboardShortcuts(Stage stage, EditorController controller) {
        if (stage == null || stage.getScene() == null) return;

        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                if (controller.getFileHandlerController() != null) {
                    controller.getFileHandlerController().saveFile();
                }
                event.consume();
                return;
            }

            if (event.getTarget() instanceof javafx.scene.control.TextArea) return;

            Pagination pagination = controller.getPagination();
            ListView<SptEntry> listView = controller.getEntryListView();

            if (pagination == null || listView == null || listView.getItems().isEmpty()) return; // 如果列表为空，不处理滚动或翻页

            int currentPage = pagination.getCurrentPageIndex();
            int maxPage = pagination.getPageCount() - 1;
            maxPage = Math.max(0, maxPage);

            boolean isTryingToChangePage = false;
            if (event.getCode() == KeyCode.LEFT && currentPage > 0) {
                isTryingToChangePage = true;
            } else if (event.getCode() == KeyCode.RIGHT && currentPage < maxPage) {
                isTryingToChangePage = true;
            } else if (event.getCode() == KeyCode.UP && listView.getSelectionModel().getSelectedIndex() <= 0 && currentPage > 0) {
                // 如果选中项在顶部或未选中，并且不在第一页，则尝试向上翻页
                isTryingToChangePage = true;
            } else if (event.getCode() == KeyCode.DOWN && listView.getSelectionModel().getSelectedIndex() >= listView.getItems().size() - 1 && currentPage < maxPage) {
                // 如果选中项在底部，并且不在最后一页，则尝试向下翻页
                isTryingToChangePage = true;
            }


            if (isTryingToChangePage && !controller.canChangePage()) {
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

            boolean pageChangedByKeyboard = false;
            if (event.getCode() == KeyCode.LEFT && currentPage > 0) {
                pagination.setCurrentPageIndex(currentPage - 1);
                pageChangedByKeyboard = true;
            } else if (event.getCode() == KeyCode.RIGHT && currentPage < maxPage) {
                pagination.setCurrentPageIndex(currentPage + 1);
                pageChangedByKeyboard = true;
            } else if (event.getCode() == KeyCode.UP) {
                int currentIndex = listView.getSelectionModel().getSelectedIndex();
                if (currentIndex > 0) {
                    int targetIndex = Math.max(0, currentIndex - ITEMS_TO_SCROLL_PER_KEY_PRESS);
                    listView.scrollTo(targetIndex);
                    listView.getSelectionModel().select(targetIndex); // 更新选中项以匹配滚动
                    listView.requestFocus(); // 确保 ListView 有焦点以便后续键盘事件
                } else if (currentIndex == 0 && currentPage > 0) { // 已在列表顶部，翻到上一页
                    pagination.setCurrentPageIndex(currentPage - 1);
                    pageChangedByKeyboard = true;
                }
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                int currentIndex = listView.getSelectionModel().getSelectedIndex();
                if (currentIndex < listView.getItems().size() - 1) {
                    int targetIndex = Math.min(listView.getItems().size() - 1, currentIndex + ITEMS_TO_SCROLL_PER_KEY_PRESS);
                    listView.scrollTo(targetIndex);
                    listView.getSelectionModel().select(targetIndex); // 更新选中项以匹配滚动
                    listView.requestFocus();
                } else if (currentIndex == listView.getItems().size() - 1 && currentPage < maxPage) { // 已在列表底部，翻到下一页
                    pagination.setCurrentPageIndex(currentPage + 1);
                    pageChangedByKeyboard = true;
                }
                event.consume();
            }

            if (pageChangedByKeyboard) {
                controller.setLastPageChangeTime(System.currentTimeMillis());
                // event.consume(); // 已在各自的 if/else if 中消费
            }
        });
    }
}