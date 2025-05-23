package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
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
        updatePaginationView();
    }

    public void updatePaginationView() {
        Pagination pagination = mainController.getPagination();
        VBox entryContainer = mainController.getEntryContainer();
        if (pagination == null || entryContainer == null) return;

        List<SptEntry> entries = mainController.getEntries();
        int itemsPerPage = mainController.getItemsPerPage();

        if (entries.isEmpty()) {
            pagination.setPageCount(1);
            pagination.setPageFactory(idx -> {
                entryContainer.getChildren().setAll(new Label("无内容可显示。"));
                return new VBox();
            });
        } else {
            int pageCount = (int) Math.ceil((double) entries.size() / itemsPerPage);
            pagination.setPageCount(Math.max(1, pageCount));
            pagination.setPageFactory(this::createPageForEntries);
        }

        int currentIdx = pagination.getCurrentPageIndex();
        int newPageCount = pagination.getPageCount();

        if (newPageCount > 0) {
            if (currentIdx < 0 || currentIdx >= newPageCount) {
                currentIdx = 0;
                pagination.setCurrentPageIndex(currentIdx);
            } else {
                createPageForEntries(currentIdx);
            }
        } else {
            entryContainer.getChildren().clear();
        }
    }

    public VBox createPageForEntries(int pageIndex) {
        VBox entryContainer = mainController.getEntryContainer();
        if (entryContainer == null) return new VBox();

        entryContainer.getChildren().clear();
        List<SptEntry> entries = mainController.getEntries();
        int itemsPerPage = mainController.getItemsPerPage();

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        if (start >= entries.size()) {
            entryContainer.getChildren().setAll(new Label("无更多条目。"));
            return new VBox();
        }
        if (entries.isEmpty()) {
            entryContainer.getChildren().setAll(new Label("无内容可显示。"));
            return new VBox();
        }

        new Thread(() -> {
            List<javafx.scene.Node> nodes = new java.util.ArrayList<>();
            for (int i = start; i < end; i++) {
                nodes.add(createUIForSptEntry(entries.get(i)));
                if (i < end - 1) {
                    Separator sep = new Separator();
                    sep.setPadding(new Insets(5, 0, 5, 0));
                    nodes.add(sep);
                }
            }
            javafx.application.Platform.runLater(() -> entryContainer.getChildren().setAll(nodes));
        }).start();

        return new VBox();
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
        if (entry.getOriginalSegments().isEmpty() || (entry.getOriginalSegments().size() == 1 && entry.getOriginalSegments().get(0).get().isEmpty())) {
            originalCol.getChildren().add(new Label("(原文为空)"));
        } else {
            entry.getOriginalSegments().forEach(segment -> {
                TextArea ta = new TextArea(segment.get());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefHeight(30);
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
            ta.setPrefHeight(30);
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
        addBtnBox.setPadding(new Insets(3, 0, 0, 0));
        translatedCol.getChildren().add(addBtnBox);

        contentBox.getChildren().addAll(originalCol, translatedCol);
        entryUIRoot.getChildren().add(contentBox);
        return entryUIRoot;
    }
}
