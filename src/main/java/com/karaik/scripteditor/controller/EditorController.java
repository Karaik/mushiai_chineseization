package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.controller.consts.EditorConst;
import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextArea;
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
    private static final int ITEMS_PER_PAGE = 100;
    private boolean modified = false;
    private Stage primaryStage;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Scene scene = pagination.getScene();
            if (scene != null) {
                primaryStage = (Stage) scene.getWindow();

                // ctrl+s
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == KeyCode.S) {
                        handleSaveFile();
                        event.consume();
                    }
                });

                // 关闭前提示未保存
                primaryStage.setOnCloseRequest(event -> {
                    if (modified) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                        alert.showAndWait().ifPresent(button -> {
                            if (!button.getButtonData().isDefaultButton()) {
                                event.consume();
                            }
                        });
                    }
                });
            }
        });
    }

    @FXML
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要打开的文本文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本文件", "*.txt")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                currentFile = file;
                modified = false;
                updateTitle();

                entries = parseSptContent(content);
                int pageCount = (int) Math.ceil(entries.size() / (double) ITEMS_PER_PAGE);
                pagination.setPageCount(pageCount);
                pagination.setPageFactory(this::createPage);
            } catch (Exception e) {
                entryContainer.getChildren().setAll(new HBox(new TextArea("文件读取失败: " + e.getMessage())));
            }
        }
    }

    @FXML
    private void handleSaveFile() {
        if (currentFile == null) {
            new Alert(Alert.AlertType.WARNING, "未打开任何文件，无法保存。").showAndWait();
            return;
        }

        try {
            SptWriter.saveToFile(entries, currentFile);
            modified = false;
            updateTitle();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage()).showAndWait();
        }
    }

    private VBox createPage(int pageIndex) {
        entryContainer.getChildren().clear();

        int start = pageIndex * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());

        for (int i = start; i < end; i++) {
            SptEntry entry = entries.get(i);

            TextArea originalArea = new TextArea();
            originalArea.setWrapText(true);
            originalArea.setEditable(false);
            originalArea.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(originalArea, Priority.ALWAYS);
            originalArea.textProperty().bind(entry.originalProperty());
            autoResizeTextAreaHeight(originalArea);

            TextArea translatedArea = new TextArea();
            translatedArea.setWrapText(true);
            translatedArea.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(translatedArea, Priority.ALWAYS);
            translatedArea.textProperty().bindBidirectional(entry.translatedProperty());
            autoResizeTextAreaHeight(translatedArea);

            // 标记已修改
            translatedArea.textProperty().addListener((obs, old, val) -> {
                if (!modified) {
                    modified = true;
                    updateTitle();
                }
            });

            HBox row = new HBox(10, originalArea, translatedArea);
            entryContainer.getChildren().add(row);
        }

        return new VBox(); // dummy node for pagination
    }

    private void autoResizeTextAreaHeight(TextArea area) {
        area.setPrefRowCount(1);
        area.textProperty().addListener((obs, old, val) -> {
            int lines = val.split("\r\n|\r|\n").length;
            area.setPrefRowCount(Math.max(2, lines));
        });
    }

    private void updateTitle() {
        if (primaryStage != null && currentFile != null) {
            String mark = modified ? "*" : "";
            primaryStage.setTitle(mark + currentFile.getName());
        }
    }

    private List<SptEntry> parseSptContent(String content) {
        List<SptEntry> result = new ArrayList<>();
        String[] lines = content.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String originalLine = lines[i];
            String translatedLine = lines[i + 1];

            if (originalLine.startsWith("○") && translatedLine.startsWith("●")) {
                String[] originalMeta = originalLine.split("\\|");
                String[] translatedMeta = translatedLine.split("\\|");

                if (originalMeta.length >= 3 && translatedMeta.length >= 3) {
                    String index = originalMeta[0].substring(1);
                    String address = originalMeta[1];
                    String length = originalMeta[2].split("○")[0];

                    String original = originalLine.replaceFirst("^○\\d+\\|[0-9A-Fa-f]+\\|[0-9A-Fa-f]+○\\s*", "")
                            .replace(EditorConst.SWAP_FLAG, "\n");
                    String translated = translatedLine.replaceFirst("^●\\d+\\|[0-9A-Fa-f]+\\|[0-9A-Fa-f]+●\\s*", "")
                            .replace(EditorConst.SWAP_FLAG, "\n");

                    result.add(new SptEntry(index, address, length, index+"|"+address+"|"+" "+original, translated));
                    i++;
                }
            }
        }
        return result;
    }
}
