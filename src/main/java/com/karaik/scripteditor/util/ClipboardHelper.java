package com.karaik.scripteditor.util;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

import java.util.List;

public class ClipboardHelper {

    public static void copyEntriesToClipboard(List<SptEntry> entries, int start, int end, Stage owner) {
        if (entries == null || entries.isEmpty() || start >= end) {
            showAlert("没有内容可以复制。", owner);
            return;
        }

        StringBuilder contentBuilder = new StringBuilder();
        for (int i = start; i < end; i++) {
            SptEntry entry = entries.get(i);
            contentBuilder
                    .append("○").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("○ ")
                    .append(entry.getFullOriginalText()).append("\n");
            contentBuilder
                    .append("●").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("● ")
                    .append(entry.getFullTranslatedText()).append("\n");
            if (i < end - 1) contentBuilder.append("\n");
        }

        ClipboardContent clipboard = new ClipboardContent();
        clipboard.putString(contentBuilder.toString().trim());
        Clipboard.getSystemClipboard().setContent(clipboard);
    }

    private static void showAlert(String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        if (owner != null) {
            alert.initOwner(owner);
            alert.setOnShown(e -> {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                if (alertStage != null) alertStage.setAlwaysOnTop(owner.isAlwaysOnTop());
            });
        }
        alert.showAndWait();
    }
}
