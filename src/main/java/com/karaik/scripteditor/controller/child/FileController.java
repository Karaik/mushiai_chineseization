package com.karaik.scripteditor.controller.child;

import com.karaik.scripteditor.controller.EditorController;
import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileController {

    private EditorController mainController;

    public FileController(EditorController mainController) {
        this.mainController = mainController;
    }

    /**
     * 处理打开文件操作。它会弹出一个文件选择器，并在用户选择文件后返回一个用于加载文件的Task。
     * @return 返回一个用于后台加载和解析文件的Task，如果用户取消选择文件则返回null。
     */
    public Task<List<SptEntry>> createLoadFileTask() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要打开的文本文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文本文件", "*.txt")
        );

        Stage primaryStage = mainController.getPrimaryStage();
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file == null) {
            return null; // 用户取消选择
        }

        mainController.setCurrentFile(file); // 设置当前文件，以便主控制器更新标题

        // 返回一个 Task 对象，由主控制器来执行和绑定进度
        return new Task<>() {
            @Override
            protected List<SptEntry> call() throws Exception {
                // Task.updateMessage() 和 updateProgress() 可以在非FX线程中安全调用
                updateMessage("正在读取文件内容...");

                // 读取文件内容
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                String[] lines = content.split("\\R");
                List<SptEntry> result = new ArrayList<>();

                // 解析文本并更新进度
                for (int i = 0; i < lines.length - 1; i++) {
                    if (isCancelled()) {
                        updateMessage("解析已取消"); // 更新取消消息
                        break;
                    }

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
                                i++; // 跳过已处理的译文行
                            }
                        }
                    }
                    // 更新进度和消息，这里的 getProgress() 是 Task 内部的属性，可以安全访问
                    // 但是通常 updateMessage 会根据你的业务逻辑来显示具体的文本
                    // 这里我们用百分比，直接通过已完成的行数和总行数计算
                    updateProgress(i + 1, lines.length); // i+1 因为i从0开始，总进度是到lines.length
                    updateMessage(String.format("解析进度: %.1f%%", (double)(i + 1) / lines.length * 100));
                }
                return result;
            }
        };
    }

    public void handleSaveFile() {
        if (mainController.getCurrentFile() == null && mainController.getEntries().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "未打开任何文件或无内容，无法保存。").showAndWait();
            return;
        }
        if (mainController.getCurrentFile() == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存译文文件");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
            File file = fileChooser.showSaveDialog(mainController.getPrimaryStage());
            if (file == null) {
                return;
            }
            mainController.setCurrentFile(file);
        }

        try {
            SptWriter.saveToFile(mainController.getEntries(), mainController.getCurrentFile());
            mainController.unmarkModified();
            new Alert(Alert.AlertType.INFORMATION, "文件已保存！").showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage()).showAndWait();
        }
    }
}