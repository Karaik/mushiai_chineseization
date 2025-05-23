package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PaginationUIController {

    private final EditorController mainController;

    public void setupPagination() {
        Pagination pagination = mainController.getPagination();
        if (pagination == null) {
            System.err.println("错误: Pagination控件在EditorController中未正确注入。");
            return;
        }
        updatePaginationView(); // 初始化分页视图
    }

    public void updatePaginationView() {
        Pagination pagination = mainController.getPagination();
        VBox entryContainer = mainController.getEntryContainer();
        if (pagination == null || entryContainer == null) return;

        List<SptEntry> entries = mainController.getEntries();
        if (entries.isEmpty()) {
            pagination.setPageCount(1); // 即使为空，也显示1页
            pagination.setPageFactory(idx -> {
                entryContainer.getChildren().setAll(new Label("无内容可显示。"));
                return new VBox(); // PageFactory需要返回Node
            });
        } else {
            int pageCount = (int) Math.ceil((double) entries.size() / EditorController.ITEMS_PER_PAGE);
            pagination.setPageCount(Math.max(1, pageCount)); // 至少1页
            pagination.setPageFactory(this::createPageForEntries); // 设置页面工厂
        }


        // 如果 pageCount 变小，setCurrentPageIndex 会自动调整到最后一页
        int currentIdx = pagination.getCurrentPageIndex();
        int newPageCount = pagination.getPageCount();

        if (newPageCount > 0) {
            if (currentIdx < 0 || currentIdx >= newPageCount) {
                pagination.setCurrentPageIndex(0); // 无效则重置为第一页 (会自动触发pageFactory)
            } else {

                createPageForEntries(currentIdx);
            }
        } else {
            entryContainer.getChildren().clear();
        }
    }

    private VBox createPageForEntries(int pageIndex) {
        VBox entryContainer = mainController.getEntryContainer();
        if (entryContainer == null) return new VBox();

        entryContainer.getChildren().clear();
        List<SptEntry> entries = mainController.getEntries();
        int start = pageIndex * EditorController.ITEMS_PER_PAGE;
        int end = Math.min(start + EditorController.ITEMS_PER_PAGE, entries.size());

        if (start >= entries.size() && !entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("无更多条目。"));
            return new VBox();
        }
        if (entries.isEmpty()){
            entryContainer.getChildren().setAll(new Label("无内容可显示。"));
            return new VBox();
        }

        for (int i = start; i < end; i++) {
            entryContainer.getChildren().add(createUIForSptEntry(entries.get(i)));
            if (i < end - 1) {
                Separator separator = new Separator();
                separator.setPadding(new Insets(5, 0, 5, 0));
                entryContainer.getChildren().add(separator);
            }
        }
        return new VBox(); // PageFactory 要求返回 Node
    }

    private VBox createUIForSptEntry(SptEntry entry) {
        VBox entryUIRoot = new VBox(5);
        entryUIRoot.setPadding(new Insets(5));
        Label metaLabel = new Label(entry.getIndex() + " | " + entry.getAddress() + " | " + entry.getLength());
        metaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em;");
        entryUIRoot.getChildren().add(metaLabel);

        HBox contentBox = new HBox(10);

        VBox originalCol = new VBox(3, new Label("原文:"));
        HBox.setHgrow(originalCol, Priority.ALWAYS);
        if (entry.getOriginalSegments().isEmpty() || (entry.getOriginalSegments().size()==1 && entry.getOriginalSegments().get(0).get().isEmpty())) {
            originalCol.getChildren().add(new Label("(原文为空)"));
        } else {
            entry.getOriginalSegments().forEach(segment -> {
                TextArea ta = new TextArea();
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.textProperty().bind(segment);
                autoResizeTextAreaHeight(ta);
                originalCol.getChildren().add(ta);
            });
        }

        VBox translatedCol = new VBox(3, new Label("译文:"));
        HBox.setHgrow(translatedCol, Priority.ALWAYS);
        ObservableList<StringProperty> translatedSegments = entry.getTranslatedSegments();
        for (int i = 0; i < translatedSegments.size(); i++) {
            StringProperty segProp = translatedSegments.get(i);
            TextArea ta = new TextArea();
            ta.setWrapText(true);
            ta.textProperty().bindBidirectional(segProp);
            autoResizeTextAreaHeight(ta);
            ta.textProperty().addListener((obs, o, n) -> mainController.markModified(true));

            Button removeBtn = new Button("-");
            final int idx = i;
            removeBtn.setOnAction(evt -> {
                entry.removeTranslatedSegment(idx);
                mainController.markModified(true);
                if (mainController.getPagination() != null) {
                    createPageForEntries(mainController.getPagination().getCurrentPageIndex());
                }
            });
            HBox row = new HBox(3, ta, removeBtn);
            HBox.setHgrow(ta, Priority.ALWAYS);
            translatedCol.getChildren().add(row);
        }
        Button addBtn = new Button("(+)");
        addBtn.setOnAction(evt -> {
            entry.addTranslatedSegment("");
            mainController.markModified(true);
            if (mainController.getPagination() != null) {
                createPageForEntries(mainController.getPagination().getCurrentPageIndex());
            }
        });
        HBox addBtnBox = new HBox(addBtn);
        addBtnBox.setAlignment(Pos.CENTER_RIGHT);
        addBtnBox.setPadding(new Insets(3,0,0,0));
        translatedCol.getChildren().add(addBtnBox);

        contentBox.getChildren().addAll(originalCol, translatedCol);
        entryUIRoot.getChildren().add(contentBox);
        return entryUIRoot;
    }

    private void autoResizeTextAreaHeight(TextArea area) {
        Runnable updateRows = () -> {
            String txt = area.getText();
            area.setPrefRowCount(Math.max(1, (txt == null ? 0 : txt.split("\\R", -1).length)));
        };
        area.textProperty().addListener((obs, o, n) -> updateRows.run());
        updateRows.run();
    }
}