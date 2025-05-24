package com.karaik.scripteditor.util;

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

                String originalForFile = entry.getFullOriginalText();
                String translatedForFile = entry.getFullTranslatedText();

                writer.write("○" + index + "|" + address + "|" + length + "○ " + originalForFile + "\n");
                writer.write("●" + index + "|" + address + "|" + length + "● " + translatedForFile + "\n");
                writer.write("\n"); // Blank line separator between entries
            }
        }
    }
}