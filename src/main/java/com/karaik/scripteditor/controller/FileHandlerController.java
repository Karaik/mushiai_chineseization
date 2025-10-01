package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.helper.CrashSafeFileSaver;
import com.karaik.scripteditor.util.SptWriter;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        // ✅ 在弹出选择器之前，先针对“当前文件所在目录”扫描并提示 .bak 恢复
        File initialDir = (editorController.getCurrentFile() != null)
                ? editorController.getCurrentFile().getParentFile()
                : null;
        promptRestoreBakIfPresent(null, initialDir);

        if (editorController.getPrimaryStage() == null) return;
        File file = fileChooser.showOpenDialog(editorController.getPrimaryStage());
        if (file != null) {
            openSpecificFile(file);
        }
    }

    public void openSpecificFile(File file) {
        // ✅ 在真正加载此文件之前，若同名 .bak 存在则提示是否恢复
        promptRestoreBakIfPresent(file, null);

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
//            SptWriter.saveToFile(editorController.getEntries(), fileToSave);
            CrashSafeFileSaver.saveWithBak(editorController.getEntries(), fileToSave);
            editorController.markModified(false);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage());
            editorController.configureAlertOnTop(alert);
            alert.showAndWait();
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

    /**
     * 共通：根据上下文提示 .bak 恢复。
     * - 若 file != null：只检查“同名 .bak”，用于 openSpecificFile 入口；
     * - 否则若 dir != null：扫描该目录下所有 *.bak，给出单/多备份的恢复提示，用于 openFile 入口。
     * 任何恢复动作成功后会删除相应的 .bak（CrashSafeFileSaver 内部已处理）。
     */
    private void promptRestoreBakIfPresent(File file, File dir) {
        // 情况 1：针对单个文件的同名 .bak
        if (file != null) {
            try {
                if (CrashSafeFileSaver.hasBak(file)) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "检测到备份文件：" + file.getName() + ".bak\n" +
                                    "这通常表示上次保存被中断。是否从备份恢复？\n\n" +
                                    "【是】用 .bak 覆盖当前文件（推荐）\n【否】忽略备份并继续打开当前文件");
                    editorController.configureAlertOnTop(alert);
                    alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> res = alert.showAndWait();
                    if (res.isPresent() && res.get() == ButtonType.YES) {
                        CrashSafeFileSaver.restoreFromBak(file);
                    }
                }
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "恢复 .bak 失败：\n" + ex.getMessage());
                editorController.configureAlertOnTop(a);
                a.showAndWait();
            }
            return;
        }

        // 情况 2：针对目录扫描所有 *.bak（用于 openFile 入口）
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        File[] bakFiles = dir.listFiles((d, name) -> name.endsWith(".bak"));
        if (bakFiles == null || bakFiles.length == 0) return;

        try {
            if (bakFiles.length == 1) {
                File bak = bakFiles[0];
                String originName = bak.getName().substring(0, bak.getName().length() - 4); // 去掉 .bak
                File origin = new File(dir, originName);

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "检测到备份文件：" + bak.getName() + "\n" +
                                "这通常表示上次保存被中断。是否将其恢复为 " + originName + " ？\n\n" +
                                "【是】用 .bak 覆盖原文件（推荐）\n【否】忽略并继续");
                editorController.configureAlertOnTop(alert);
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                Optional<ButtonType> res = alert.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.YES) {
                    CrashSafeFileSaver.restoreFromBak(origin);
                }
            } else {
                // 多个 .bak：让用户选择一个恢复
                List<String> choices = Arrays.stream(bakFiles)
                        .map(File::getName)
                        .sorted()
                        .collect(Collectors.toList());
                ChoiceDialog<String> dlg = new ChoiceDialog<>(choices.get(0), choices);
                dlg.setTitle("发现多个备份");
                dlg.setHeaderText("检测到多个 .bak 备份（目录：" + dir.getAbsolutePath() + "）");
                dlg.setContentText("选择要恢复的备份（将覆盖同名原文件）：");
                // 注意：EditorController.configureAlertOnTop 仅适用于 Alert，这里不要调用
                Optional<String> sel = dlg.showAndWait();
                if (sel.isPresent()) {
                    String bakName = sel.get();
                    File origin = new File(dir, bakName.substring(0, bakName.length() - 4));
                    CrashSafeFileSaver.restoreFromBak(origin);
                }
            }
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "恢复 .bak 失败：\n" + ex.getMessage());
            editorController.configureAlertOnTop(a);
            a.showAndWait();
        }
    }
}
