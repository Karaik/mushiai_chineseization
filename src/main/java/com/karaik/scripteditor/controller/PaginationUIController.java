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

@RequiredArgsConstructor
public class PaginationUIController {

    private final EditorController editorController;
    private static final int MAX_TEXT_LENGTH = 24; // 定义最大字符数常量

    public void setupPagination() {
        Pagination pagination = editorController.getPagination();
        if (pagination == null) {
            System.err.println("\u9519\u8bef: Pagination\u63a7\u4ef6\u5728EditorController\u4e2d\u672a\u6b63\u786e\u6ce8\u5165\u3002");
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
                entryContainer.getChildren().setAll(new Label("\u65e0\u5185\u5bb9\u53ef\u663e\u793a\u3002"));
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
        Pagination pagination = editorController.getPagination();
        if (pagination != null) {
            pagination.setDisable(true);
        }

        entryContainer.getChildren().clear();
        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        if (start >= entries.size() && !entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("\u65e0\u66f4\u591a\u6761\u76ee\u3002"));
            Platform.runLater(() -> {
                editorController.setRendering(false);
                if (pagination != null) {
                    pagination.setDisable(false);
                }
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
            if (pagination != null) {
                pagination.setDisable(false);
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

        Button copyEntryBtn = new Button("\u590d\u5236\u672c\u6761");
        copyEntryBtn.setOnAction(evt -> {
            StringBuilder entryClipboardContent = new StringBuilder();
            entryClipboardContent
                    .append("\u25cb").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("\u25cb ")
                    .append(entry.getFullOriginalText()).append("\n");
            entryClipboardContent
                    .append("\u25cf").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("\u25cf ")
                    .append(entry.getFullTranslatedText());

            ClipboardContent content = new ClipboardContent();
            content.putString(entryClipboardContent.toString());
            Clipboard.getSystemClipboard().setContent(content);
            System.out.println("\u5355\u6761\u5185\u5bb9\u5df2\u590d\u5236\u5230\u526a\u8d34\u677f\u3002");
        });

        metaRow.getChildren().addAll(metaLabel, copyEntryBtn);
        HBox.setHgrow(metaLabel, Priority.ALWAYS);

        entryUIRoot.getChildren().add(metaRow);

        HBox contentBox = new HBox(10);

        VBox originalCol = new VBox(3, new Label("\u539f\u6587:"));
        HBox.setHgrow(originalCol, Priority.ALWAYS);
        if (entry.getOriginalSegments().isEmpty() || (entry.getOriginalSegments().size() == 1 && entry.getOriginalSegments().get(0).get().isEmpty())) {
            originalCol.getChildren().add(new Label("(\u539f\u6587\u4e3a\u7a7a)"));
        } else {
            entry.getOriginalSegments().forEach(segment -> {
                TextArea ta = new TextArea(segment.get());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefHeight(10);
                originalCol.getChildren().add(ta);
            });
        }

        VBox translatedCol = new VBox(3, new Label("\u8bd1\u6587:"));
        HBox.setHgrow(translatedCol, Priority.ALWAYS);
        ObservableList<StringProperty> translatedSegments = entry.getTranslatedSegments();
        for (int i = 0; i < translatedSegments.size(); i++) {
            StringProperty segProp = translatedSegments.get(i);
            TextArea ta = new TextArea();
            ta.setWrapText(true);
            ta.textProperty().bindBidirectional(segProp);
            ta.setPrefHeight(10);

            Label charCountLabel = new Label();
            charCountLabel.setStyle("-fx-font-size: 0.8em; -fx-text-fill: grey;");

            ta.textProperty().addListener((obs, oldValue, newValue) -> {
                String currentText = (newValue != null) ? newValue : "";
                int currentLength = currentText.length();
                charCountLabel.setText(currentLength + "/" + MAX_TEXT_LENGTH);

                if (currentLength > MAX_TEXT_LENGTH) {
                    Platform.runLater(() -> {
                        ta.setText(oldValue != null ? oldValue : "");
                        charCountLabel.setText((oldValue != null ? oldValue.length() : 0) + "/" + MAX_TEXT_LENGTH);
                    });
                    return;
                }

                applyLengthStyle(ta);
                editorController.markModified(true);
            });

            String initialText = segProp.get();
            charCountLabel.setText((initialText != null ? initialText.length() : 0) + "/" + MAX_TEXT_LENGTH);
            applyLengthStyle(ta);

            Button removeBtn = new Button("-");
            int idx = i;
            removeBtn.setOnAction(evt -> {
                entry.removeTranslatedSegment(idx);
                editorController.markModified(true);
                editorController.getPaginationUIController().createPageForEntries(editorController.getPagination().getCurrentPageIndex());
            });

            VBox textAreaWithCount = new VBox(3, ta, charCountLabel);
            HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
            HBox row = new HBox(3, textAreaWithCount, removeBtn);
            HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
            translatedCol.getChildren().add(row);
        }

        Button addBtn = new Button("(+)");
        if (translatedSegments.size() >= 4) {
            addBtn.setDisable(true);
            addBtn.setTooltip(new Tooltip("\u6700\u591a\u53ea\u80fd\u67094\u4e2a\u8bd1\u6587\u6bb5\u843d"));
        }
        addBtn.setOnAction(evt -> {
            entry.addTranslatedSegment("");
            editorController.markModified(true);
            editorController.getPaginationUIController().createPageForEntries(editorController.getPagination().getCurrentPageIndex());
        });
        HBox addBtnBox = new HBox(addBtn);
        addBtnBox.setAlignment(Pos.CENTER_RIGHT);
        addBtnBox.setPadding(new Insets(3, 0, 0, 0));
        translatedCol.getChildren().add(addBtnBox);

        contentBox.getChildren().addAll(originalCol, translatedCol);
        entryUIRoot.getChildren().add(contentBox);
        return entryUIRoot;
    }

    private void applyLengthStyle(TextArea ta) {
        if (ta.getText() != null && ta.getText().length() > MAX_TEXT_LENGTH) {
            if (!ta.getStyleClass().contains("text-area-red-overflow")) {
                ta.getStyleClass().add("text-area-red-overflow");
            }
        } else {
            ta.getStyleClass().remove("text-area-red-overflow");
        }
    }
}
