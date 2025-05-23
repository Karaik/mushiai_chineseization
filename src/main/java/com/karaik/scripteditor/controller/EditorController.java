package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.controller.consts.EditorConst;
import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EditorController {

    @FXML private VBox entryContainer;
    @FXML private Pagination pagination;

    private File currentFile;
    private List<SptEntry> entries = new ArrayList<>();
    private static final int ITEMS_PER_PAGE = 10; // 每一页显示的文本行数，越多越慢
    private boolean modified = false;
    private Stage primaryStage;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Scene scene = pagination.getScene();
            if (scene != null) {
                primaryStage = (Stage) scene.getWindow();
                // 添加 Ctrl+S 快捷键保存支持
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == KeyCode.S) {
                        handleSaveFile();
                        event.consume();
                    }
                });
                // 在关闭窗口前检查是否有未保存修改
                primaryStage.setOnCloseRequest(event -> {
                    if (modified) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                        alert.showAndWait().ifPresent(buttonType -> {
                            if (buttonType != ButtonType.OK && buttonType != ButtonType.YES) {
                                event.consume();
                            }
                        });
                    }
                });
                updateTitle(); // 初始化时更新标题
            } else {
                System.err.println("Scene not available at initialize for primary stage setup.");
            }
        });
        if (primaryStage == null && entryContainer.getScene() != null && entryContainer.getScene().getWindow() instanceof Stage) {
            primaryStage = (Stage) entryContainer.getScene().getWindow();
            updateTitle(); // 初始化时更新标题
        }
    }

    private void markModified() {
        if (!modified) {
            modified = true;
            updateTitle();
        }
    }

    @FXML
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要打开的文本文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本文件", "*.txt")
        );

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                currentFile = file;
                modified = false;
                updateTitle();

                entries = parseSptContent(content);
                if (entries.isEmpty()) {
                    pagination.setPageCount(1);
                    pagination.setPageFactory(pageIndex -> {
                        entryContainer.getChildren().clear();
                        entryContainer.getChildren().add(new Label("文件内容为空或格式不正确。"));
                        return new VBox();
                    });
                } else {
                    int pageCount = (int) Math.ceil(entries.size() / (double) ITEMS_PER_PAGE);
                    pagination.setPageCount(Math.max(1, pageCount));
                    pagination.setPageFactory(this::createPage);
                }
                pagination.setCurrentPageIndex(0);
                if (pagination.getPageCount() > 0) {
                    createPage(0); // 显式调用第一页的渲染方法
                }

            } catch (Exception e) {
                e.printStackTrace();
                entryContainer.getChildren().setAll(new HBox(new TextArea("文件读取失败: " + e.getMessage())));
            }
        }
    }

    @FXML
    private void handleSaveFile() {
        if (currentFile == null && entries.isEmpty()) { // 同时检查当前文件和内容是否为空
            new Alert(Alert.AlertType.WARNING, "未打开任何文件或无内容，无法保存。").showAndWait();
            return;
        }
        if (currentFile == null) { // 如果当前没有指定文件但有内容，提示用户选择路径
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存译文文件");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file == null) {
                return; // 用户取消保存对话框
            }
            currentFile = file; // 设置当前文件以用于之后的保存
        }

        try {
            SptWriter.saveToFile(entries, currentFile);
            modified = false;
            updateTitle();
            new Alert(Alert.AlertType.INFORMATION, "文件已保存！").showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage()).showAndWait();
        }
    }

    private VBox createPage(int pageIndex) {
        entryContainer.getChildren().clear();

        int start = pageIndex * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());

        if (start >= entries.size() && !entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("无更多条目。"));
            return new VBox();
        }
        if (entries.isEmpty()) {
            entryContainer.getChildren().add(new Label("没有可显示的条目。"));
            return new VBox();
        }

        for (int i = start; i < end; i++) {
            SptEntry entry = entries.get(i);
            entryContainer.getChildren().add(createUIForSptEntry(entry));
            if (i < end - 1) {
                Separator separator = new Separator();
                separator.setPadding(new Insets(10, 0, 10, 0));
                entryContainer.getChildren().add(separator);
            }
        }
        return new VBox(); // 占位节点
    }

    private VBox createUIForSptEntry(SptEntry entry) {
        VBox entryUIRoot = new VBox(10); // 单个条目的垂直布局，控件之间间距为10
        entryUIRoot.setPadding(new Insets(10));

        Label metaLabel = new Label( entry.getIndex() + " | " + entry.getAddress() + " | " + entry.getLength());
        metaLabel.setStyle("-fx-font-weight: bold;");
        entryUIRoot.getChildren().add(metaLabel);

        HBox contentColumnsBox = new HBox(15); // 原文和译文两列之间的间距

        // --- 原文列 ---
        VBox originalColumn = new VBox(5);
        originalColumn.setPrefWidth(350); // 设置推荐宽度
        HBox.setHgrow(originalColumn, Priority.ALWAYS);
        originalColumn.getChildren().add(new Label("Original:"));
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
                autoResizeTextAreaHeight(originalArea);
                originalColumn.getChildren().add(originalArea);
            }
        }

        // --- 译文列 ---
        VBox translatedColumn = new VBox(5);
        translatedColumn.setPrefWidth(350);
        HBox.setHgrow(translatedColumn, Priority.ALWAYS);
        translatedColumn.getChildren().add(new Label("Translated:"));

        ObservableList<StringProperty> translatedSegments = entry.getTranslatedSegments();

        for (int segIdx = 0; segIdx < translatedSegments.size(); segIdx++) {
            StringProperty segmentProp = translatedSegments.get(segIdx);
            HBox translatedSegmentRow = new HBox(5);
            translatedSegmentRow.setAlignment(Pos.CENTER_LEFT);

            TextArea translatedArea = new TextArea();
            translatedArea.setWrapText(true);
            translatedArea.textProperty().bindBidirectional(segmentProp);
            autoResizeTextAreaHeight(translatedArea);
            HBox.setHgrow(translatedArea, Priority.ALWAYS);
            translatedArea.textProperty().addListener((obs, oldVal, newVal) -> markModified());

            Button removeButton = new Button("-");
            final int currentIndex = segIdx;
            removeButton.setOnAction(e -> {
                entry.removeTranslatedSegment(currentIndex);
                markModified();
                createPage(pagination.getCurrentPageIndex()); // 简单刷新当前页
            });
            translatedSegmentRow.getChildren().addAll(translatedArea, removeButton);
            translatedColumn.getChildren().add(translatedSegmentRow);
        }

        Button addTranslatedButton = new Button("(+)");
        addTranslatedButton.setOnAction(e -> {
            entry.addTranslatedSegment(""); // 添加一个新的空段
            markModified();
            createPage(pagination.getCurrentPageIndex()); // 简单刷新当前页
        });
        VBox addButtonBar = new VBox(addTranslatedButton); // 用于对齐添加按钮
        addButtonBar.setPadding(new Insets(5,0,0,0));
        addButtonBar.setAlignment(Pos.CENTER_RIGHT);
        translatedColumn.getChildren().add(addButtonBar);

        contentColumnsBox.getChildren().addAll(originalColumn, translatedColumn);
        entryUIRoot.getChildren().add(contentColumnsBox);

        return entryUIRoot;
    }

    private void autoResizeTextAreaHeight(TextArea area) {
        area.setPrefRowCount(1);
        area.textProperty().addListener((obs, oldText, newText) -> {
            int lines = newText.split("\r\n|\r|\n", -1).length;
            area.setPrefRowCount(Math.max(1, lines));
        });
        int initialLines = area.getText().split("\r\n|\r|\n", -1).length;
        area.setPrefRowCount(Math.max(1, initialLines));
    }

    private void updateTitle() {
        if (primaryStage != null) {
            String baseTitle = "虫爱少女汉化文本编辑器（仅用于汉化组内部使用，禁止外传）";
            String fileName = (currentFile != null) ? currentFile.getName() : "请选择文件";
            String mark = modified ? "*" : "";
            primaryStage.setTitle(mark + fileName + " - " + baseTitle);
        }
    }

    private List<SptEntry> parseSptContent(String content) {
        List<SptEntry> result = new ArrayList<>();
        String[] lines = content.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String originalLineWithHeader = lines[i].trim();
            String translatedLineWithHeader = lines[i + 1].trim();

            if (originalLineWithHeader.startsWith("○") && translatedLineWithHeader.startsWith("●")) {
                String[] originalParts = originalLineWithHeader.split("○", 3);
                String[] translatedParts = translatedLineWithHeader.split("●", 3);

                if (originalParts.length > 1 && translatedParts.length > 1) {
                    String[] originalMetaParts = originalParts[1].split("\\|");
                    if (originalMetaParts.length >= 3) {
                        String index = originalMetaParts[0];
                        String address = originalMetaParts[1];
                        String length = originalMetaParts[2];

                        String originalTextContentWithSwapFlags = (originalParts.length > 2) ? originalParts[2].trim() : "";
                        String translatedTextContentWithSwapFlags = (translatedParts.length > 2) ? translatedParts[2].trim() : "";

                        result.add(new SptEntry(index, address, length, originalTextContentWithSwapFlags, translatedTextContentWithSwapFlags));
                        i++;
                    }
                }
            }
        }
        return result;
    }
}