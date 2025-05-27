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
import java.util.Optional;

@RequiredArgsConstructor
public class PaginationUIController {

    private final EditorController editorController;
    private static final int MAX_TEXT_LENGTH = 24;

    public void setupPagination() {
        Pagination pagination = editorController.getPagination();
        if (pagination == null) return;
        pagination.setPageFactory(this::createPageForEntries);
    }

    public void updatePaginationView() {
        Pagination pagination = editorController.getPagination();
        VBox entryContainer = editorController.getEntryContainer();
        if (pagination == null || entryContainer == null) return;

        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

        int newPageCount;
        if (entries.isEmpty()) {
            newPageCount = 1;
        } else {
            newPageCount = (int) Math.ceil((double) entries.size() / itemsPerPage);
            newPageCount = Math.max(1, newPageCount);
        }

        int currentPageBeforeUpdate = pagination.getCurrentPageIndex();
        pagination.setPageCount(newPageCount);
        int currentPageAfterUpdate = pagination.getCurrentPageIndex();

        if (currentPageBeforeUpdate != currentPageAfterUpdate || newPageCount == 1 || entries.size() > 0) {
            createPageForEntries(currentPageAfterUpdate);
        } else if (entries.isEmpty() && currentPageAfterUpdate == 0) {
            createPageForEntries(0);
        }
    }

    public VBox createPageForEntries(int pageIndex) {
        VBox sharedEntryContainer = editorController.getEntryContainer();
        if (sharedEntryContainer == null) {
            return new VBox(new Label("Error: UI container not found."));
        }

        editorController.setRendering(true);
        Pagination pagination = editorController.getPagination();
        if (pagination != null) pagination.setDisable(true);

        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();
        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        List<Node> nodesToAdd = new ArrayList<>();
        if (entries.isEmpty() && pageIndex == 0) {
            Label noContentLabel = new Label("无内容可显示。");
            noContentLabel.setPadding(new Insets(10));
            nodesToAdd.add(noContentLabel);
        } else if (start >= entries.size() && !entries.isEmpty()) {
            Label errorLabel = new Label("页码超出范围或无条目。");
            errorLabel.setPadding(new Insets(10));
            nodesToAdd.add(errorLabel);
        }
        else {
            for (int i = start; i < end; i++) {
                nodesToAdd.add(createUIForSptEntry(entries.get(i)));
                if (i < end - 1) nodesToAdd.add(new Separator());
            }
        }

        Platform.runLater(() -> {
            sharedEntryContainer.getChildren().setAll(nodesToAdd);
            editorController.setRendering(false);
            if (editorController.getPagination() != null) editorController.getPagination().setDisable(false);
            if (editorController.getMainContentScrollPane() != null) {
                editorController.getMainContentScrollPane().setVvalue(0.0);
            }
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
            Label emptyOriginal = new Label("(原文为空)");
            emptyOriginal.setPadding(new Insets(2));
            originalCol.getChildren().add(emptyOriginal);
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

            int insertIndex;
            Optional<Node> foundAddBtnBox = translatedCol.getChildren().stream().filter(node -> "addBtnBox".equals(node.getId())).findFirst();
            if (foundAddBtnBox.isPresent()) {
                insertIndex = translatedCol.getChildren().indexOf(foundAddBtnBox.get());
            } else {
                if (translatedCol.getChildren().size() > 0) insertIndex = translatedCol.getChildren().size() -1;
                else insertIndex = 0;
            }
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
            if (realIndex != -1) {
                entry.removeTranslatedSegment(realIndex);
                editorController.markModified(true);
                parentTranslatedCol.getChildren().remove(removeBtn.getParent());

                Node addBtnBoxNode = parentTranslatedCol.lookup("#addBtnBox");
                if (addBtnBoxNode instanceof HBox && !((HBox) addBtnBoxNode).getChildren().isEmpty()) {
                    Node firstChild = ((HBox) addBtnBoxNode).getChildren().get(0);
                    if (firstChild instanceof Button) {
                        updateAddBtnState((Button)firstChild, entry.getTranslatedSegments());
                    }
                }
            }
        });

        VBox textAreaWithCount = new VBox(3, ta, charCountLabel);
        HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
        HBox row = new HBox(3, textAreaWithCount, removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void applyLengthStyle(TextArea ta) {
        if (ta.getText() != null && ta.getText().length() > MAX_TEXT_LENGTH) {
            ta.setStyle("-fx-text-fill: red;");
        } else {
            ta.setStyle("");
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