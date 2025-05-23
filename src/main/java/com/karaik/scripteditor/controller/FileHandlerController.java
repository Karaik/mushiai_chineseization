package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class FileHandlerController {

    private final EditorController mainController;

    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文本文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));

        File current = mainController.getCurrentFile();
        if (current != null && current.getParentFile().exists()) {
            fileChooser.setInitialDirectory(current.getParentFile());
        }

        if (mainController.getPrimaryStage() == null) return;
        File file = fileChooser.showOpenDialog(mainController.getPrimaryStage());

        if (file != null) {
            openSpecificFile(file);
        }
    }

    public void openSpecificFile(File file) {

        new Thread(() -> {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                List<SptEntry> entries = parseSptContent(content);

                Platform.runLater(() -> {
                    mainController.setCurrentFile(file);
                    mainController.setEntries(entries);
                    mainController.markModified(false);
                    mainController.rememberFile(file);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    mainController.getEntryContainer().getChildren().setAll(new Label("文件读取失败: " + e.getMessage()));
                    mainController.setEntries(new ArrayList<>());
                });
            }
        }).start();
    }

    public void saveFile() {
        File fileToSave = mainController.getCurrentFile();
        if (fileToSave == null && mainController.getEntries().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "无内容或未指定文件，无法保存。").showAndWait();
            return;
        }

        if (fileToSave == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存译文文件");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
            if (mainController.getPrimaryStage() == null) return;
            fileToSave = fileChooser.showSaveDialog(mainController.getPrimaryStage());
            if (fileToSave == null) return;
            mainController.setCurrentFile(fileToSave);
        }

        try {
            SptWriter.saveToFile(mainController.getEntries(), fileToSave);
            mainController.markModified(false);
            new Alert(Alert.AlertType.INFORMATION, "文件已保存！").showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage()).showAndWait();
        } finally {
        }
    }

    private List<SptEntry> parseSptContent(String content) {
        List<SptEntry> result = new ArrayList<>();
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length - 1; i++) {
            String ol = lines[i].trim();
            String tl = lines[i + 1].trim();
            if (ol.startsWith("○") && tl.startsWith("●")) {
                String[] op = ol.split("○", 3);
                String[] tp = tl.split("●", 3);
                if (op.length > 1 && tp.length > 1) {
                    String[] meta = op[1].split("\\|");
                    if (meta.length >= 3) {
                        result.add(new SptEntry(meta[0], meta[1], meta[2],
                                (op.length > 2 ? op[2].trim() : ""),
                                (tp.length > 2 ? tp[2].trim() : "")));
                        i++;
                    }
                }
            }
        }
        return result;
    }
}
