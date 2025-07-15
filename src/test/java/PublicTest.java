import checkSolution.CheckerPipeline;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PublicTest {

    private static final Path RESOURCES_PATH = Paths.get("src/main/resources");
    private static final Path SPT_PATH = RESOURCES_PATH.resolve("spt");
    private static final Path SPT_ORIGIN_PATH = RESOURCES_PATH.resolve("sptBluePrint");

    private static Map<String, File> sptFiles;
    private static Map<String, File> sptOriginFiles;

    @Test
    void checkerPipeline() throws IOException {

        // 1. 查找所有的 .spt.txt 文件
        sptFiles = findAllFiles();
        sptOriginFiles = findAllOriginFiles();

        // 2. 校验所有文件
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, File> entry : sptFiles.entrySet()) {
            File file = entry.getValue();
            result.append(checkFile(file)).append("\n\n");
        }

        // 3. 写入检查结果
        String fileName = "result.txt";
        Path outputFile = RESOURCES_PATH.resolve(fileName);
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, result.toString(), StandardCharsets.UTF_8);
        System.out.println("检查完成，结果写入：" + outputFile.toAbsolutePath());

    }

    private static Map<String, File> findAllFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(SPT_PATH)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".spt.txt"))
                    .map(Path::toFile)
                    .collect(Collectors.toMap(
                            File::getName, // key: 文件名（不含路径）
                            file -> file, // value: 文件对象
                            (f1, f2) -> f1 // 如果重名，保留第一个
                    ));
        }
    }

    private static Map<String, File> findAllOriginFiles() throws IOException {
        try (Stream<Path> stream = Files.walk(SPT_ORIGIN_PATH)) {
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

    private String checkFile(File file) {
        StringBuilder report = new StringBuilder();
        String fileName = file.getName();

        report.append("\n====================================\n")
                .append(fileName)
                .append("\n====================================\n");

        try {
            // 1. 原文段落对比（● 行）
            List<String> originalLines = getOriginalLines(file);
            List<String> originalErrors = validateOriginSpt(originalLines, fileName);
            if (!originalErrors.isEmpty()) {
                report.append("× 原文列与源文件内的原文列不一致\n")
                        .append(String.join("\n\n", originalErrors));
                report.append("\n\n");
            }

            // 2. 校验译文列格式（○ 行）
            List<String> translateLines = getTranslateLines(file);
            List<String> translateErrors = new ArrayList<>();
            for (int i = 0; i < translateLines.size(); i++) {
                String line = translateLines.get(i);
                String originalLine = originalLines.get(i);
                report.append(CheckerPipeline.sptFormatCheck(line, originalLine));
            }

        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }

        return report.toString();
    }

    private List<String> validateOriginSpt(List<String> originSpt, String fileName) throws IOException {
        List<String> errorLines = new ArrayList<>();
        List<String> bluePrint = getOriginalLines(sptOriginFiles.get(fileName));
        if (bluePrint.isEmpty()) {
            errorLines.add("未找到对应源文件");
            return errorLines;
        }
        for (int i = 0; i < originSpt.size(); i++) {
            try {
                if (!bluePrint.get(i).equals(originSpt.get(i))) {
                    errorLines.add(
                            "源文件原文："+bluePrint.get(i)+
                            "\n"+
                            "译文件原文："+originSpt.get(i));
                }
            } catch (Exception e) {
                System.out.println("");
            }
        }
        return errorLines;
    }

    private List<String> getOriginalLines(File file) throws IOException {
        if (file==null || !file.exists()) {return new ArrayList<>();}
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return lines.stream().filter(line -> line.startsWith("○")).toList();
    }

    private List<String> getTranslateLines(File file) throws IOException {
        if (file==null || !file.exists()) {return new ArrayList<>();}
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return lines.stream().filter(line -> line.startsWith("●")).toList();
    }


}
