package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class PaginationUIController {

    private final EditorController editorController;
    private static final int MAX_TEXT_LENGTH = 24;

    public void setupPagination() {
        Pagination pagination = editorController.getPagination();
        if (pagination == null) return;
        pagination.setPageFactory(this::createPageForEntries);
        updatePaginationView();
    }

    public void updatePaginationView() {
        Pagination pagination = editorController.getPagination();
        VBox entryContainer = editorController.getEntryContainer();
        if (pagination == null || entryContainer == null) return;

        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();
        
        pagination.setPageFactory(this::createPageForEntries);

        if (entries.isEmpty()) {
            pagination.setPageCount(1);
            pagination.setPageFactory(idx -> {
                entryContainer.getChildren().setAll(new Label("无内容可显示。"));
                return new VBox();
            });
        } else {
            int pageCount = (int) Math.ceil((double) entries.size() / itemsPerPage);
            pagination.setPageCount(Math.max(1, pageCount));
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
        Pagination pagination = editorController.getPagination();
        if (pagination != null) pagination.setDisable(true);

        entryContainer.getChildren().clear();
        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        if (start >= entries.size() && !entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("无更多条目。"));
            Platform.runLater(() -> {
                editorController.setRendering(false);
                if (pagination != null) pagination.setDisable(false);
            });
            return new VBox();
        }

        List<Node> nodesToAdd = new ArrayList<>();
        for (int i = start; i < end; i++) {
            nodesToAdd.add(createUIForSptEntry(entries.get(i)));
            if (i < end - 1) nodesToAdd.add(new Separator());
        }

        Platform.runLater(() -> {
            entryContainer.getChildren().setAll(nodesToAdd);
            editorController.setRendering(false);
            if (pagination != null) pagination.setDisable(false);
        });

        return new VBox();
    }

    private VBox createUIForSptEntry(SptEntry entry) {
        VBox entryUIRoot = new VBox(5);
        entryUIRoot.setPadding(new Insets(5));

        HBox metaRow = new HBox(5);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label metaLabel = new Label(entry.getIndex() + " | " + entry.getAddress() + " | " + entry.getLength());
        metaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em;");

        Button copyEntryBtn = new Button("复制本条");
        copyEntryBtn.setOnAction(evt -> {
            StringBuilder content = new StringBuilder();
            content.append("○").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("○ ")
                    .append(entry.getFullOriginalText()).append("\n")
                    .append("●").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("● ")
                    .append(entry.getFullTranslatedText());
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content.toString());
            Clipboard.getSystemClipboard().setContent(clipboardContent);
        });

        metaRow.getChildren().addAll(metaLabel, copyEntryBtn);
        HBox.setHgrow(metaLabel, Priority.ALWAYS);
        entryUIRoot.getChildren().add(metaRow);

        HBox contentBox = new HBox(10);
        VBox originalCol = new VBox(3, new Label("原文:"));
        VBox translatedCol = new VBox(3, new Label("译文:"));
        HBox.setHgrow(originalCol, Priority.ALWAYS);
        HBox.setHgrow(translatedCol, Priority.ALWAYS);

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

        ObservableList<StringProperty> translatedSegments = entry.getTranslatedSegments();
        for (int i = 0; i < translatedSegments.size(); i++) {
            translatedCol.getChildren().add(createTranslatedSegmentUI(entry, i, translatedSegments.get(i), translatedCol));
        }

        Button addBtn = new Button("(+)");
        HBox addBtnBox = new HBox(addBtn);
        addBtnBox.setId("addBtnBox");
        addBtnBox.setAlignment(Pos.CENTER_RIGHT);
        addBtnBox.setPadding(new Insets(3, 0, 0, 0));
        updateAddBtnState(addBtn, translatedSegments);
        addBtn.setOnAction(evt -> {
            StringProperty newProp = entry.addTranslatedSegment("");
            editorController.markModified(true);
            Node ui = createTranslatedSegmentUI(entry, translatedSegments.size() - 1, newProp, translatedCol);
            int insertIndex = IntStream.range(0, translatedCol.getChildren().size())
                    .filter(i -> "addBtnBox".equals(translatedCol.getChildren().get(i).getId()))
                    .findFirst().orElse(translatedCol.getChildren().size());
            translatedCol.getChildren().add(insertIndex, ui);
            updateAddBtnState(addBtn, translatedSegments);
        });
        translatedCol.getChildren().add(addBtnBox);

        contentBox.getChildren().addAll(originalCol, translatedCol);
        entryUIRoot.getChildren().add(contentBox);
        return entryUIRoot;
    }

    private HBox createTranslatedSegmentUI(SptEntry entry, int index, StringProperty segProp, VBox parentTranslatedCol) {
        TextArea ta = new TextArea();
        ta.setWrapText(true);
        ta.textProperty().bindBidirectional(segProp);
        ta.setPrefHeight(10);

        Label charCountLabel = new Label();
        charCountLabel.setStyle("-fx-font-size: 0.8em; -fx-text-fill: grey;");

        ta.textProperty().addListener((obs, oldVal, newVal) -> {
            String text = (newVal != null) ? newVal : "";
            charCountLabel.setText(text.length() + "/" + MAX_TEXT_LENGTH);
            applyLengthStyle(ta);
            editorController.markModified(true);
        });

        charCountLabel.setText((segProp.get() != null ? segProp.get().length() : 0) + "/" + MAX_TEXT_LENGTH);
        applyLengthStyle(ta);

        Button removeBtn = new Button("-");
        removeBtn.setOnAction(evt -> {
            int realIndex = entry.getTranslatedSegments().indexOf(segProp);
            entry.removeTranslatedSegment(realIndex);
            editorController.markModified(true);
            parentTranslatedCol.getChildren().remove(removeBtn.getParent());
            Button addBtn = (Button)((HBox) parentTranslatedCol.lookup("#addBtnBox")).getChildren().get(0);
            updateAddBtnState(addBtn, entry.getTranslatedSegments());
        });

        VBox textAreaWithCount = new VBox(3, ta, charCountLabel);
        HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
        HBox row = new HBox(3, textAreaWithCount, removeBtn);
        HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
        return row;
    }

    private void applyLengthStyle(TextArea ta) {
        if (ta.getText() != null && ta.getText().length() > MAX_TEXT_LENGTH) {
            ta.setStyle("-fx-text-fill: red;");
        } else {
            ta.setStyle("-fx-text-fill: black;");
        }
    }

    private void updateAddBtnState(Button addBtn, ObservableList<StringProperty> segments) {
        if (segments.size() >= 4) {
            addBtn.setDisable(true);
            addBtn.setTooltip(new Tooltip("最多只能有4个译文段落"));
        } else {
            addBtn.setDisable(false);
            addBtn.setTooltip(null);
        }
    }
}
