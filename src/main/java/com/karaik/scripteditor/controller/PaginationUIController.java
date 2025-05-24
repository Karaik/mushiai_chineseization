package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
            System.err.println("错误: Pagination控件在EditorController中未正确注入。");
            return;
        }
        pagination.setPageFactory(this::createPageForEntries);
        updatePaginationView();
    }

    public void updatePaginationView() {
        Pagination pagination = editorController.getPagination();
        VBox entryContainer = editorController.getEntryContainer();
        if (pagination == null || entryContainer == null) return;

        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

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
        VBox entryContainer = editorController.getEntryContainer();
        if (entryContainer == null) return new VBox();

        editorController.setRendering(true);

        entryContainer.getChildren().clear();
        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        if (start >= entries.size() && !entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("无更多条目。"));
            Platform.runLater(() -> {
                editorController.setRendering(false);
            });
            return new VBox();
        }

        List<Node> nodesToAdd = new ArrayList<>();
        for (int i = start; i < end; i++) {
            nodesToAdd.add(createUIForSptEntry(entries.get(i)));
            if (i < end - 1) {
                Separator sep = new Separator();
                sep.setPadding(new Insets(5, 0, 5, 0));
                nodesToAdd.add(sep);
            }
        }

        Platform.runLater(() -> {
            entryContainer.getChildren().setAll(nodesToAdd);
            editorController.setRendering(false);
        });

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
                ta.setPrefHeight(10);
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
            ta.setPrefHeight(10);
            ta.textProperty().addListener((obs, o, n) -> editorController.markModified(true));

            Button removeBtn = new Button("-");
            int idx = i;
            removeBtn.setOnAction(evt -> {
                entry.removeTranslatedSegment(idx);
                editorController.markModified(true);
                if (editorController.getPagination() != null) {
                    createPageForEntries(editorController.getPagination().getCurrentPageIndex());
                }
            });

            HBox row = new HBox(3, ta, removeBtn);
            HBox.setHgrow(ta, Priority.ALWAYS);
            translatedCol.getChildren().add(row);
        }

        Button addBtn = new Button("(+)");
        addBtn.setOnAction(evt -> {
            entry.addTranslatedSegment("");
            editorController.markModified(true);
            if (editorController.getPagination() != null) {
                createPageForEntries(editorController.getPagination().getCurrentPageIndex());
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