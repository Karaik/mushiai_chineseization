package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PaginationUIController {

    private final EditorController editorController;

    public void setupPagination() {
        Pagination pagination = editorController.getPagination();
        if (pagination == null) {
            System.err.println("Pagination control is null in setupPagination.");
            return;
        }
        if (editorController.getEntryListView() == null) {
            System.err.println("ListView is null when setting up pagination. PageFactory might not work correctly.");
        }
        pagination.setPageFactory(this::pageFactoryCallback);
    }

    public void updatePaginationView() {
        Pagination pagination = editorController.getPagination();
        ListView<SptEntry> listView = editorController.getEntryListView();

        if (pagination == null || listView == null) {
            System.err.println("Pagination or ListView is null in updatePaginationView. Skipping update.");
            return;
        }

        List<SptEntry> allEntries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();
        if (itemsPerPage <= 0) {
            itemsPerPage = 1;
        }

        int newPageCount = allEntries.isEmpty() ? 1 : Math.max(1, (int) Math.ceil((double) allEntries.size() / itemsPerPage));

        if (pagination.getPageCount() != newPageCount) {
            pagination.setPageCount(newPageCount);
        }

        int currentValidPageIndex = pagination.getCurrentPageIndex();
        if (currentValidPageIndex >= newPageCount && newPageCount > 0) {
            currentValidPageIndex = newPageCount - 1;
        }
        currentValidPageIndex = Math.max(0, currentValidPageIndex);

        if (pagination.getCurrentPageIndex() != currentValidPageIndex) {
            pagination.setCurrentPageIndex(currentValidPageIndex);
        } else {
            loadDataForPage(currentValidPageIndex);
        }
    }


    private Node pageFactoryCallback(int pageIndex) {
        loadDataForPage(pageIndex);
        return new VBox(); // 返回占位符
    }

    public void loadDataForPage(int pageIndex) {
        ListView<SptEntry> listView = editorController.getEntryListView();
        if (listView == null) {
            System.err.println("ListView is null in loadDataForPage for page " + pageIndex);
            return;
        }

        editorController.setRendering(true);

        List<SptEntry> allEntries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();
        if (itemsPerPage <= 0) itemsPerPage = 1;

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allEntries.size());

        List<SptEntry> entriesForThisPage = new ArrayList<>();
        if (start >= 0 && start < allEntries.size()) {
            for (int i = start; i < end; i++) {
                entriesForThisPage.add(allEntries.get(i));
            }
        }

        Platform.runLater(() -> {
            listView.setPlaceholder(null);

            if (allEntries.isEmpty()) {
                listView.setPlaceholder(createPlaceholderLabel("无内容可显示。"));
                listView.getItems().clear();
            } else if (entriesForThisPage.isEmpty()) {
                listView.setPlaceholder(createPlaceholderLabel("当前页无条目。"));
                listView.getItems().clear();
            } else {
                listView.getItems().setAll(entriesForThisPage); // 更新 ListView 的数据
            }

            // 在数据加载完成后，将 ListView 滚动到顶部
            if (!entriesForThisPage.isEmpty()) { // 只有在有条目时才滚动
                listView.scrollTo(0); // 滚动到当前页的第一个条目 (索引为0)
            }

            editorController.setRendering(false);
        });
    }

    private Label createPlaceholderLabel(String text) {
        Label placeholder = new Label(text);
        placeholder.setPadding(new Insets(10));
        placeholder.setStyle("-fx-font-style: italic; -fx-text-fill: grey;");
        return placeholder;
    }
}