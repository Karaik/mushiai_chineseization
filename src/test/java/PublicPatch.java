import checkSolution.SptConstants;
import checkSolution.SptLineUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据补丁文件回写资源文件；若要暂时停用可直接注释整个测试方法或 applyPatch 调用。
 */
public class PublicPatch {

    private static final Path RESOURCES_PATH = Paths.get("src/main/resources");
    private static final Path RESULT_PATH = RESOURCES_PATH.resolve(SptConstants.RESULT_DIRECTORY);
    private static final Path SPT_PATH = RESOURCES_PATH.resolve("spt");

    @Test
    void patch() throws IOException {
        List<PatchFile> patchFiles = loadPatchFiles();
        if (patchFiles.isEmpty()) {
            System.out.println("No patch files found. Run checkerPipeline() first.");
            return;
        }

        int totalUpdated = 0;
        int totalMissing = 0;
        List<String> missingIds = new ArrayList<>();

        for (PatchFile patchFile : patchFiles) {
            // 若不想回写，可注释掉下一行。
            PatchOutcome outcome = applyPatch(patchFile);
            totalUpdated += outcome.updated();
            totalMissing += outcome.missing();
            missingIds.addAll(outcome.missingIds());
        }

        System.out.println("Patch completed: updated " + totalUpdated + " entries, "
                + totalMissing + " unmatched.");
        if (!missingIds.isEmpty()) {
            System.out.println("Missing IDs:");
            missingIds.stream().distinct().forEach(id -> System.out.println(" - " + id));
        }
    }

    private static List<PatchFile> loadPatchFiles() throws IOException {
        if (!Files.exists(RESULT_PATH)) {
            return List.of();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                RESULT_PATH,
                SptConstants.PATCH_FILE_PREFIX + "*" + SptConstants.PATCH_FILE_SUFFIX)) {
            List<PatchFile> patches = new ArrayList<>();
            for (Path patchPath : stream) {
                PatchFile patchFile = parsePatchFile(patchPath);
                if (!patchFile.entries().isEmpty()) {
                    patches.add(patchFile);
                }
            }
            return patches;
        }
    }

    private static PatchFile parsePatchFile(Path patchPath) throws IOException {
        List<String> lines = Files.readAllLines(patchPath, StandardCharsets.UTF_8);
        String targetPath = lines.stream()
                .filter(line -> line.startsWith("# FILE:"))
                .map(line -> line.substring("# FILE:".length()).trim())
                .findFirst()
                .orElseGet(() -> inferRelativePathFromName(patchPath.getFileName().toString()));

        if (targetPath == null || targetPath.isEmpty()) {
            throw new IllegalStateException("Unable to resolve target file for patch: " + patchPath);
        }

        List<PatchEntry> entries = new ArrayList<>();
        String currentId = null;
        List<String> currentMessages = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("# ID:")) {
                currentId = line.substring("# ID:".length()).trim();
                currentMessages = new ArrayList<>();
            } else if (line.startsWith("# ERR:")) {
                if (currentId != null) {
                    currentMessages.add(line.substring("# ERR:".length()).trim());
                }
            } else if (line.startsWith("#")) {
                // ignore other comments
            } else if (!line.isBlank()) {
                String rawLine = line;
                String id = currentId;
                if (id == null || id.isEmpty()) {
                    id = SptLineUtils.extractId(rawLine).orElse(null);
                }
                if (id != null && !id.isBlank()) {
                    boolean translate = SptLineUtils.isTranslateLine(rawLine);
                    entries.add(new PatchEntry(id, translate, rawLine, List.copyOf(currentMessages)));
                }
                currentId = null;
                currentMessages = new ArrayList<>();
            }
        }

        return new PatchFile(targetPath, List.copyOf(entries), patchPath);
    }

    private static PatchOutcome applyPatch(PatchFile patchFile) throws IOException {
        Path target = SPT_PATH.resolve(patchFile.relativePath());
        if (!Files.exists(target)) {
            System.out.println("Target file not found, skip: " + target);
            List<String> ids = patchFile.entries().stream().map(PatchEntry::id).collect(Collectors.toList());
            return new PatchOutcome(0, ids.size(), ids);
        }

        List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
        Map<String, Integer> indexByKey = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            final int index = i;
            SptLineUtils.extractId(line).ifPresent(id -> {
                String key = buildKey(id, SptLineUtils.isTranslateLine(line));
                indexByKey.putIfAbsent(key, index);
            });
        }

        int updated = 0;
        int missing = 0;
        List<String> missingIds = new ArrayList<>();
        boolean changed = false;

        for (PatchEntry entry : patchFile.entries()) {
            String key = buildKey(entry.id(), entry.translate());
            Integer index = indexByKey.get(key);
            if (index == null) {
                missing++;
                missingIds.add(entry.id());
                continue;
            }
            String current = lines.get(index);
            if (current.equals(entry.rawLine())) {
                continue;
            }
            lines.set(index, entry.rawLine());
            updated++;
            changed = true;
        }

        if (changed) {
            Path backup = target.resolveSibling(target.getFileName().toString() + ".bak");
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.write(target, lines, StandardCharsets.UTF_8);
                Files.deleteIfExists(backup);
            } catch (IOException ex) {
                Files.move(backup, target, StandardCopyOption.REPLACE_EXISTING);
                throw ex;
            }
        }

        return new PatchOutcome(updated, missing, missingIds);
    }

    private static String buildKey(String id, boolean translate) {
        return id + "|" + (translate ? "T" : "O");
    }

    private static String inferRelativePathFromName(String fileName) {
        if (!fileName.startsWith(SptConstants.PATCH_FILE_PREFIX)
                || !fileName.endsWith(SptConstants.PATCH_FILE_SUFFIX)) {
            return "";
        }
        String core = fileName.substring(
                SptConstants.PATCH_FILE_PREFIX.length(),
                fileName.length() - SptConstants.PATCH_FILE_SUFFIX.length());
        String restored = core.replace(SptConstants.PATCH_PATH_SEPARATOR, "/");
        return restored + SptConstants.PATCH_FILE_SUFFIX;
    }

    private record PatchFile(String relativePath, List<PatchEntry> entries, Path path) {
    }

    private record PatchEntry(String id, boolean translate, String rawLine, List<String> messages) {
    }

    private record PatchOutcome(int updated, int missing, List<String> missingIds) {
    }
}
