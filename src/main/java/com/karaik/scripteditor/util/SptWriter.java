package com.karaik.scripteditor.util;

import com.karaik.scripteditor.controller.consts.EditorConst; // Keep for reference, though not directly used in replace
import com.karaik.scripteditor.entry.SptEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SptWriter {

    public static void saveToFile(List<SptEntry> entries, File outputFile) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            for (SptEntry entry : entries) {
                String index = entry.getIndex();
                String address = entry.getAddress();
                String length = entry.getLength();

                // SptEntry.getFullOriginalText() and getFullTranslatedText()
                // now return strings with segments already joined by EditorConst.SWAP_FLAG.
                String originalForFile = entry.getFullOriginalText();
                String translatedForFile = entry.getFullTranslatedText();

                // The .replace("\n", EditorConst.SWAP_FLAG) calls are NO LONGER NEEDED here.

                writer.write("○" + index + "|" + address + "|" + length + "○ " + originalForFile + "\n");
                writer.write("●" + index + "|" + address + "|" + length + "● " + translatedForFile + "\n");
                writer.write("\n"); // Blank line separator between entries
            }
        }
    }
}