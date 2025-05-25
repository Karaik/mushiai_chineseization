package com.karaik.scripteditor.controller.child;

import com.karaik.scripteditor.controller.EditorController;
import com.karaik.scripteditor.entry.SptEntry;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell; // 导入 ListCell
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font; // 导入 Font for precise text measurement
import javafx.scene.text.Text; // 导入 Text for precise text measurement
import javafx.application.Platform; // 导入 Platform

import java.util.List;

public class EntryUIController {

    private EditorController mainController;

    public EntryUIController(EditorController mainController) {
        this.mainController = mainController;
    }

    // 核心修改：此方法现在返回一个 ListCell，而不是 VBox
    public ListCell<SptEntry> createSptEntryListCell() {
        return new ListCell<>() {
            private VBox entryUIRoot;
            private Text textMeasurer; // 用于精确测量 Text

            @Override
            protected void updateItem(SptEntry entry, boolean empty) {
                super.updateItem(entry, empty);

                // *** 解决问题2：禁用ListCell的默认选中样式和边距 ***
                // 仅设置背景透明，这样点击也不会有蓝色选中背景，且不会影响布局
                // 移除 border-color，因为 ListCell 默认有边框，可能会导致双重边框或意外效果
                setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-color: transparent;");

                if (empty || entry == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (entryUIRoot == null) {
                        // 首次创建 Cell 时初始化 UI 结构
                        entryUIRoot = new VBox(10); // 单个条目的垂直布局，控件之间间距为10
                        entryUIRoot.setPadding(new Insets(10));
                        // 保持内部卡片样式，与ListCell的透明背景区分
                        entryUIRoot.setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: white;");

                        Label metaLabel = new Label(); // 将文本设置延迟到 updateItem
                        metaLabel.setStyle("-fx-font-weight: bold;");
                        entryUIRoot.getChildren().add(metaLabel);

                        HBox contentColumnsBox = new HBox(15); // 原文和译文两列之间的间距
                        contentColumnsBox.setId("contentColumnsBox"); // 用于在更新时查找
                        entryUIRoot.getChildren().add(contentColumnsBox);

                        // 测量文本的工具初始化
                        textMeasurer = new Text();
                        // 初始字体设置（确保与TextArea默认字体一致，如果TextArea有自定义字体，这里也应同步）
                        textMeasurer.setFont(Font.font("System", 12));

                        // 仅在 Cell 首次创建时，为原文列和译文列添加空的 VBox
                        VBox originalColumn = new VBox(5);
                        originalColumn.setPrefWidth(350); // 设置推荐宽度，但会被 HBox.setHgrow 动态调整
                        HBox.setHgrow(originalColumn, Priority.ALWAYS);
                        originalColumn.getChildren().add(new Label("原文:"));
                        originalColumn.setId("originalColumn");
                        contentColumnsBox.getChildren().add(originalColumn);

                        VBox translatedColumn = new VBox(5);
                        translatedColumn.setPrefWidth(350); // 设置推荐宽度，但会被 HBox.setHgrow 动态调整
                        HBox.setHgrow(translatedColumn, Priority.ALWAYS);
                        translatedColumn.getChildren().add(new Label("译文:"));
                        translatedColumn.setId("translatedColumn");
                        contentColumnsBox.getChildren().add(translatedColumn);
                    }

                    // 每次更新 Item 时，根据新的 Entry 填充数据
                    Label metaLabel = (Label) entryUIRoot.getChildren().get(0);
                    metaLabel.setText(entry.getIndex() + " | " + entry.getAddress() + " | " + entry.getLength());

                    HBox contentColumnsBox = (HBox) entryUIRoot.lookup("#contentColumnsBox");
                    VBox originalColumn = (VBox) contentColumnsBox.lookup("#originalColumn");
                    VBox translatedColumn = (VBox) contentColumnsBox.lookup("#translatedColumn");

                    // --- 更新原文列 ---
                    originalColumn.getChildren().clear();
                    originalColumn.getChildren().add(new Label("原文:")); // 重新添加标题

                    List<ReadOnlyStringWrapper> originalSegments = entry.getOriginalSegments();
                    if (originalSegments.isEmpty() || (originalSegments.size() == 1 && originalSegments.get(0).get().isEmpty())) {
                        Label noOriginalTextLabel = new Label("(Original text is empty)");
                        noOriginalTextLabel.setPadding(new Insets(5));
                        originalColumn.getChildren().add(noOriginalTextLabel);
                    } else {
                        for (ReadOnlyStringWrapper segment : originalSegments) {
                            TextArea originalArea = new TextArea();
                            originalArea.setEditable(false);
                            originalArea.setWrapText(true);
                            originalArea.textProperty().bind(segment);
                            // *** 解决问题1：使用精确测量高度的方法 ***
                            autoResizeTextAreaHeight(originalArea, textMeasurer);
                            originalColumn.getChildren().add(originalArea);
                        }
                    }

                    // --- 更新译文列 ---
                    translatedColumn.getChildren().clear();
                    translatedColumn.getChildren().add(new Label("译文:")); // 重新添加标题

                    ObservableList<StringProperty> translatedSegments = entry.getTranslatedSegments();

                    for (int segIdx = 0; segIdx < translatedSegments.size(); segIdx++) {
                        StringProperty segmentProp = translatedSegments.get(segIdx);
                        HBox translatedSegmentRow = new HBox(5);
                        translatedSegmentRow.setAlignment(Pos.CENTER_LEFT);

                        TextArea translatedArea = new TextArea();
                        translatedArea.setWrapText(true);
                        translatedArea.textProperty().bindBidirectional(segmentProp);
                        // *** 解决问题1：使用精确测量高度的方法 ***
                        autoResizeTextAreaHeight(translatedArea, textMeasurer);
                        HBox.setHgrow(translatedArea, Priority.ALWAYS);
                        translatedArea.textProperty().addListener((obs, oldVal, newVal) -> mainController.markModified());

                        Button removeButton = new Button("-");
                        final int currentIndex = segIdx;
                        removeButton.setOnAction(e -> {
                            entry.removeTranslatedSegment(currentIndex);
                            mainController.markModified();
                            mainController.refreshCurrentPage(); // 通知主控制器刷新当前页
                        });
                        translatedSegmentRow.getChildren().addAll(translatedArea, removeButton);
                        translatedColumn.getChildren().add(translatedSegmentRow);
                    }

                    Button addTranslatedButton = new Button("(+)");
                    addTranslatedButton.setOnAction(e -> {
                        entry.addTranslatedSegment(""); // 添加一个新的空段
                        mainController.markModified();
                        mainController.refreshCurrentPage(); // 通知主控制器刷新当前页
                    });
                    VBox addButtonBar = new VBox(addTranslatedButton); // 用于对齐添加按钮
                    addButtonBar.setPadding(new Insets(5,0,0,0));
                    addButtonBar.setAlignment(Pos.CENTER_RIGHT);
                    translatedColumn.getChildren().add(addButtonBar);

                    setGraphic(entryUIRoot);
                }
            }

            /**
             * 精确测量 TextArea 的高度并设置。
             * @param area 要调整的 TextArea
             * @param measurer 用于测量文本的 Text 节点
             */
            private void autoResizeTextAreaHeight(TextArea area, Text measurer) {
                // 监听宽度变化，因为 TextArea 宽度会影响换行和高度
                // 使用 Platform.runLater 确保在布局周期内获取准确的宽度
                area.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                    Platform.runLater(() -> updateTextAreaHeight(area, measurer, newWidth.doubleValue()));
                });

                // 监听文本变化
                area.textProperty().addListener((obs, oldText, newText) -> {
                    // 在文本变化时，也重新计算高度
                    Platform.runLater(() -> {
                        updateTextAreaHeight(area, measurer, area.getWidth());
                    });
                });

                // 初始设置高度（可能在宽度未完全确定前，但至少提供一个初始值）
                Platform.runLater(() -> {
                    updateTextAreaHeight(area, measurer, area.getWidth());
                });
            }

            /**
             * 实际计算和设置 TextArea 高度的方法。
             * @param area 要调整的 TextArea
             * @param measurer 用于测量文本的 Text 节点
             * @param targetWidth TextArea 的目标宽度
             */
            private void updateTextAreaHeight(TextArea area, Text measurer, double targetWidth) {
                String text = area.getText();
                if (text == null || text.isEmpty()) {
                    area.setPrefHeight(25); // 至少一行高，经验值
                    area.setMinHeight(25);
                    return;
                }

                // 确保测量的 Text 节点具有和 TextArea 相同的字体和换行属性
                measurer.setFont(area.getFont()); // 获取 TextArea 的实际字体
                // 减去内边距和滚动条宽度估算。10-20是经验值，确保不出现滚动条
                // 这里的 15 是一个经验值，根据实际情况可以调整
                double usableWidth = targetWidth - (area.getPadding().getLeft() + area.getPadding().getRight() + 15);
                if (usableWidth < 1) usableWidth = 1; // 确保宽度不为负或0

                measurer.setWrappingWidth(usableWidth);
                measurer.setText(text);

                double textHeight = measurer.getLayoutBounds().getHeight();
                double padding = area.getPadding().getTop() + area.getPadding().getBottom();
                double totalHeight = textHeight + padding + 15; // 额外增加一些边距，确保滚动条不出现，15为经验值

                // 确保最小高度不小于一行，且不会无限缩小
                // 考虑到字体大小和内边距，估算一行高度
                double minLineHeight = area.getFont().getSize() * 1.5 + area.getPadding().getTop() + area.getPadding().getBottom() + 5; // 经验值
                area.setPrefHeight(Math.max(minLineHeight, totalHeight));
                area.setMinHeight(Math.max(minLineHeight, totalHeight));
            }
        };
    }
}