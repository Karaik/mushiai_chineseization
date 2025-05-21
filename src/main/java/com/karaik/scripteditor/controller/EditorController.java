package com.karaik.scripteditor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class EditorController {

    @FXML private TextArea originalTextArea;
    @FXML private TextArea translatedTextArea;

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
                originalTextArea.setText(content);
                translatedTextArea.setText(""); // 或 copy 一份 content
            } catch (Exception e) {
                originalTextArea.setText("文件读取失败: " + e.getMessage());
            }
        }
    }
}