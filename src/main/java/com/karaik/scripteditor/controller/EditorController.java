package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class EditorController {

    @FXML
    private VBox originalContainer;

    @FXML
    private VBox translatedContainer;

    private List<SptEntry> entries = new ArrayList<>();

    public void initialize() {
        // 可在此加入默认加载测试文件
        try {
            loadSptFile(Paths.get("08.spt.txt")); // 替换成你实际的文件路径
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSptFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        entries.clear();

        for (int i = 0; i < lines.size() - 1; i++) {
            String line1 = lines.get(i);
            String line2 = lines.get(i + 1);

            if (line1.startsWith("○") && line2.startsWith("●")) {
                SptEntry entry = new SptEntry(line1, line2);
                entries.add(entry);
                i++; // skip next line
            }
        }

        refreshUI();
    }

    private void refreshUI() {
        originalContainer.getChildren().clear();
        translatedContainer.getChildren().clear();

        for (SptEntry entry : entries) {
            TextArea originalArea = new TextArea(entry.getOriginal());
            originalArea.setWrapText(true);
            originalArea.setPrefRowCount(3);
            originalArea.setEditable(false);

            TextArea translatedArea = new TextArea(entry.getTranslated());
            translatedArea.setWrapText(true);
            translatedArea.setPrefRowCount(3);

            originalContainer.getChildren().add(originalArea);
            translatedContainer.getChildren().add(translatedArea);
        }
    }

    // 可扩展保存等功能
}