import com.karaik.scripteditor.entry.SptEntry;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptTest {
    private static final Path RESOURCES_PATH = Paths.get("src/main/resources");
    private static final Path SPT_PATH = RESOURCES_PATH.resolve("spt");
    private static final Path SPT_ORIGIN_PATH = RESOURCES_PATH.resolve("sptBluePrint");

    private static Map<String, File> sptFiles;

    @Test
    void countDuplicateSentencesTest() throws IOException {
        HashMap<String, Integer> sentencesMap = new HashMap<>();
        sptFiles = findAllFiles();
        sptFiles.forEach((key, file) -> {
            String content = null;
            try {
                content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<SptEntry> sptEntries = parseSptContent(content);
            sptEntries.forEach((entry) -> {
                List<ReadOnlyStringWrapper> originalSegments = entry.getOriginalSegments();
                originalSegments.forEach((segment) -> {
                    String originalSpt = segment.get();
                    if (!sentencesMap.containsKey(originalSpt)) {
                        sentencesMap.put(originalSpt, 1);
                    } else {
                        sentencesMap.put(originalSpt, sentencesMap.get(originalSpt) + 1);
                    }
                });
            });
        });

        // 把 map里统计的句子出现数量，从大到小逆序排列，写到一个 countDuplicateSentences.txt里面
        Path out = RESOURCES_PATH.resolve("countDuplicateSentences.txt");
        List<String> lines = sentencesMap.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(
                        Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                                .thenComparing(Map.Entry::getKey)
                )
                .map(e -> e.getValue() + "\t" + e.getKey())
                .collect(Collectors.toList());

        // 写入文件
        Files.createDirectories(out.getParent());
        Files.write(out, lines, StandardCharsets.UTF_8);
        System.out.println("Duplicate sentences written to: " + out.toAbsolutePath());
    }

    private static Map<String, File> findAllFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(SPT_PATH)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".spt.txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toMap(
                            File::getName,
                            file -> file,
                            (f1, f2) -> f1
                    ));
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
}
