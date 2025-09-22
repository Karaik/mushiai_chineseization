package com.karaik.scripteditor.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles creation and retention of history snapshots that live alongside the source file.
 */
public final class HistoryManager {

    private static final int MAX_HISTORY_FILES = 3;

    private HistoryManager() {
    }

    public static void storeSnapshot(File sourceFile) {
        if (sourceFile == null) {
            return;
        }
        Path sourcePath = sourceFile.toPath();
        if (!Files.isRegularFile(sourcePath)) {
            return;
        }
        Path historyDir = sourcePath.resolveSibling("history");
        try {
            Files.createDirectories(historyDir);
            String baseName = sourceFile.getName();
            String timestamp = String.valueOf(System.currentTimeMillis());
            Path snapshotPath = historyDir.resolve(baseName + "." + timestamp + ".autosave");
            Files.copy(sourcePath, snapshotPath, StandardCopyOption.REPLACE_EXISTING);
            pruneOldSnapshots(historyDir, baseName);
        } catch (IOException e) {
            System.err.println("Failed to create history snapshot for " + sourcePath + ": " + e.getMessage());
        }
    }

    private static void pruneOldSnapshots(Path historyDir, String baseName) throws IOException {
        try (Stream<Path> stream = Files.list(historyDir)) {
            List<Path> snapshots = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> matchesBaseName(path, baseName))
                    .sorted(Comparator.comparingLong(HistoryManager::lastModified).reversed())
                    .collect(Collectors.toList());
            for (int i = MAX_HISTORY_FILES; i < snapshots.size(); i++) {
                try {
                    Files.deleteIfExists(snapshots.get(i));
                } catch (IOException ex) {
                    System.err.println("Failed to delete old history snapshot " + snapshots.get(i) + ": " + ex.getMessage());
                }
            }
        }
    }

    private static boolean matchesBaseName(Path path, String baseName) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith(baseName + ".") && fileName.endsWith(".autosave");
    }

    private static long lastModified(Path path) {
        try {
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            return lastModifiedTime.toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }
}
