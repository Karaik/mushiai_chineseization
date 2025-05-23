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

                String original = entry.getOriginal().replace("\n", "[\\r][\\n]");
                String translated = entry.getTranslated().replace("\n", "[\\r][\\n]");

                writer.write("○" + index + "|" + address + "|" + length + "○ " + original + "\n");
                writer.write("●" + index + "|" + address + "|" + length + "● " + translated + "\n");
                writer.write("\n");
            }
        }
    }
}
