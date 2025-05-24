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
        Pagination pagination = editorController.getPagination();
        if (pagination != null) {
            pagination.setDisable(true); // 禁用分页，防止同时翻页
        }

        entryContainer.getChildren().clear();
        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        if (start >= entries.size() && !entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("无更多条目。"));
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

        Button copyEntryBtn = new Button("复制本条");
        copyEntryBtn.setOnAction(evt -> {
            StringBuilder entryClipboardContent = new StringBuilder();
            entryClipboardContent
                    .append("○").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("○ ")
                    .append(entry.getFullOriginalText()).append("\n");
            entryClipboardContent
                    .append("●").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("● ")
                    .append(entry.getFullTranslatedText());

            ClipboardContent content = new ClipboardContent();
            content.putString(entryClipboardContent.toString());
            Clipboard.getSystemClipboard().setContent(content);
        });

        metaRow.getChildren().addAll(metaLabel, copyEntryBtn);
        HBox.setHgrow(metaLabel, Priority.ALWAYS);

        entryUIRoot.getChildren().add(metaRow);

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

        // 为每个现有的翻译片段创建 UI
        for (int i = 0; i < translatedSegments.size(); i++) {
            translatedCol.getChildren().add(createTranslatedSegmentUI(entry, i, translatedSegments.get(i), translatedCol));
        }

        Button addBtn = new Button("(+)");
        // 这里的 disable 逻辑可以直接基于 translatedSegments 的当前大小
        if (translatedSegments.size() >= 4) {
            addBtn.setDisable(true);
            addBtn.setTooltip(new Tooltip("最多只能有4个译文段落"));
        }
        addBtn.setOnAction(evt -> {
            // 添加新的翻译片段到 SptEntry
            StringProperty newSegmentProp = entry.addTranslatedSegment("");
            editorController.markModified(true);

            // 为新添加的片段创建 UI 元素
            Node newSegmentNode = createTranslatedSegmentUI(entry, translatedSegments.size() - 1, newSegmentProp, translatedCol);

            // 在 addBtnBox 之前插入新的片段 UI
            // addBtnBox 是 translatedCol 的最后一个子节点
            translatedCol.getChildren().add(translatedCol.getChildren().size() - 1, newSegmentNode);

            // 更新 addBtn 的禁用状态
            if (translatedSegments.size() >= 4) {
                addBtn.setDisable(true);
                addBtn.setTooltip(new Tooltip("最多只能有4个译文段落"));
            }
        });
        HBox addBtnBox = new HBox(addBtn);
        addBtnBox.setAlignment(Pos.CENTER_RIGHT);
        addBtnBox.setPadding(new Insets(3, 0, 0, 0));
        translatedCol.getChildren().add(addBtnBox); // 将 addBtnBox 添加到 translatedCol 的末尾

        contentBox.getChildren().addAll(originalCol, translatedCol);
        entryUIRoot.getChildren().add(contentBox);
        return entryUIRoot;
    }

    // 提取的用于创建单个翻译片段 UI 的方法
    private HBox createTranslatedSegmentUI(SptEntry entry, int index, StringProperty segProp, VBox parentTranslatedCol) {
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
                ta.setStyle("-fx-text-fill: red;");
            } else {
                ta.setStyle("-fx-text-fill: black;");
            }

            applyLengthStyle(ta);
            editorController.markModified(true);
        });

        String initialText = segProp.get();
        charCountLabel.setText((initialText != null ? initialText.length() : 0) + "/" + MAX_TEXT_LENGTH);
        applyLengthStyle(ta);

        Button removeBtn = new Button("-");
        // 使用 final int 确保 lambda 表达式可以捕获
        final int segmentIndex = index;
        removeBtn.setOnAction(evt -> {
            // 从 SptEntry 中移除翻译片段
            entry.removeTranslatedSegment(segmentIndex);
            editorController.markModified(true);

            // 从 UI 中移除对应的 HBox 节点
            // removeBtn.getParent() 就是 HBox (newRow)
            parentTranslatedCol.getChildren().remove(removeBtn.getParent());

            // 重新调整索引，因为 SptEntry 中的片段数量和顺序可能改变
            // 这是一个更复杂的场景，如果需要，可以考虑重新遍历并设置所有 removeBtn 的 action
            // 但对于移除单个元素，通常直接移除节点就足够了，除非你需要依赖精确的索引
            // 简单起见，这里假设 removeTranslatedSegment 会正确处理列表内部的移除
            // 如果后续出现索引错乱问题，可能需要更复杂的 UI 刷新逻辑，但不是整个分页刷新
            // 这里我们更新一下 (+) 按钮的禁用状态
            Button addBtn = (Button)((HBox)parentTranslatedCol.getChildren().get(parentTranslatedCol.getChildren().size()-1)).getChildren().get(0);
            if (entry.getTranslatedSegments().size() < 4) {
                addBtn.setDisable(false);
                addBtn.setTooltip(null);
            }
        });

        VBox textAreaWithCount = new VBox(3, ta, charCountLabel);
        HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
        HBox row = new HBox(3, textAreaWithCount, removeBtn);
        HBox.setHgrow(textAreaWithCount, Priority.ALWAYS);
        return row;
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