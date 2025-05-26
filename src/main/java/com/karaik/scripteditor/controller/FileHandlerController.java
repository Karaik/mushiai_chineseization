package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Data
public class FileHandlerController {

    private final EditorController editorController;

    public void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文本文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));

        File current = editorController.getCurrentFile();
        if (current != null && current.getParentFile().exists()) {
            fileChooser.setInitialDirectory(current.getParentFile());
        }

        if (editorController.getPrimaryStage() == null) return;
        File file = fileChooser.showOpenDialog(editorController.getPrimaryStage());

        if (file != null) {
            openSpecificFile(file);
        }
    }

    public void openSpecificFile(File file) {
        new Thread(() -> {

            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                List<SptEntry> sptEntries = parseSptContent(content);

                Platform.runLater(() -> {
                    editorController.markModified(false);
                    editorController.setEntries(sptEntries);
                    editorController.setCurrentFile(file);
                    editorController.restoreLastPage();
                    editorController.setInitializing(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "无法打开文件: " + e.getMessage());
                    editorController.configureAlertOnTop(alert);
                    alert.showAndWait();
                    editorController.getEntries().clear();
                    editorController.setEntries(new ArrayList<>());
                    editorController.setCurrentFile(null);
                    editorController.setInitializing(false);
                });
            }
        }).start();
    }

    public void saveFile() {
        File fileToSave = editorController.getCurrentFile();
        if (fileToSave == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存文件");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));

            File current = editorController.getCurrentFile();
            if (current != null && current.getParentFile().exists()) {
                fileChooser.setInitialDirectory(current.getParentFile());
            }

            fileToSave = fileChooser.showSaveDialog(editorController.getPrimaryStage());
            if (fileToSave == null) return;
            editorController.setCurrentFile(fileToSave);
        }

        try {
            SptWriter.saveToFile(editorController.getEntries(), fileToSave);
            editorController.markModified(false);
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
                    }
                }
            }
        }
        return result;
    }
}