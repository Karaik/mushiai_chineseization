import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.karaik.scripteditor.entry.SptEntry;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptWordsTest {

    private static final Path RESOURCES_PATH = Paths.get("src/main/resources");
    private static final Path SPT_PATH = RESOURCES_PATH.resolve("spt");

    private static Map<String, File> sptFiles;

    @Test
    void countDuplicateWordsTest() throws IOException {
        Map<String, Integer> wordFreq = new HashMap<>();

        // 读取所有 .spt.txt
        sptFiles = findAllFiles();

        // Kuromoji 分词器（ipadic）
        Tokenizer tokenizer = new Tokenizer();

        sptFiles.forEach((name, file) -> {
            String content;
            try {
                content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 解析 SPT 为条目
            List<SptEntry> sptEntries = parseSptContent(content);

            // 合并后再分词
            for (SptEntry entry : sptEntries) {
                List<ReadOnlyStringWrapper> segments = entry.getOriginalSegments();

                // 1) 合并：用 "。" 作为句边界，避免词粘连
                String merged = segments.stream()
                        .map(ReadOnlyStringWrapper::get)
                        .filter(Objects::nonNull)
                        .map(s -> java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC).trim())
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("。"));

                if (merged.isEmpty()) continue;

                // 2) Kuromoji 分词
                List<com.atilika.kuromoji.ipadic.Token> tokens = tokenizer.tokenize(merged);
                for (var t : tokens) {
                    // 过滤标点等（POS Level1 == "記号"）
                    if ("記号".equals(t.getPartOfSpeechLevel1())) continue;

                    String base = t.getBaseForm();
                    String word = (base != null && !"*".equals(base)) ? base : t.getSurface();
                    if (word == null || word.isBlank()) continue;

                    word = word.toLowerCase(Locale.ROOT);
                    wordFreq.merge(word, 1, Integer::sum);
                }
            }
        });

        // 输出：只保留出现>1 的词，频次倒序，其次按词典序
        Path out = RESOURCES_PATH.resolve("countDuplicateWords.txt");
        List<String> lines = wordFreq.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(
                        Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                                .thenComparing(Map.Entry::getKey)
                )
                .map(e -> e.getValue() + "\t" + e.getKey())
                .collect(Collectors.toList());

        Files.createDirectories(out.getParent());
        Files.write(out, lines, StandardCharsets.UTF_8);
        System.out.println("Duplicate words written to: " + out.toAbsolutePath());
    }

    private static Map<String, File> findAllFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(SPT_PATH)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".spt.txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toMap(
                            File::getName,
                            f -> f,
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
