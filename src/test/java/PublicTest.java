import checkSolution.CheckerPipeline;
import checkSolution.SptConstants;
import checkSolution.SptLineUtils;
import checkSolution.Violation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 公共测试入口，可通过注释某些调用来临时关闭指定功能。
 */
public class PublicTest {

    private static final Path RESOURCES_PATH = Paths.get("src/main/resources");
    private static final Path SPT_PATH = RESOURCES_PATH.resolve("spt");
    private static final Path SPT_ORIGIN_PATH = RESOURCES_PATH.resolve("sptBluePrint");
    private static final Path RESULT_PATH = RESOURCES_PATH.resolve(SptConstants.RESULT_DIRECTORY);

    private static Map<String, Path> sptFiles;
    private static Map<String, Path> sptOriginFiles;

    @Test
    void checkerPipeline() throws IOException {
        sptFiles = findAllFiles(SPT_PATH);
        sptOriginFiles = findAllFiles(SPT_ORIGIN_PATH);

        Files.createDirectories(RESULT_PATH);

        List<String> reports = new ArrayList<>();
        for (Map.Entry<String, Path> entry : sptFiles.entrySet()) {
            String relativePath = entry.getKey();
            Path filePath = entry.getValue();
            CheckResult result = checkFile(relativePath, filePath);
            reports.add(result.report());
            // 若不想导出补丁文件，可注释掉下一行。
            writePatchFile(result);
        }

        Path reportFile = RESULT_PATH.resolve(SptConstants.REPORT_FILE_NAME);
        Files.writeString(reportFile,
                String.join(System.lineSeparator() + System.lineSeparator(), reports),
                StandardCharsets.UTF_8);
        System.out.println("检查完成，报告写入: " + reportFile.toAbsolutePath());
    }

    private static Map<String, Path> findAllFiles(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".spt.txt"))
                    .collect(Collectors.toMap(
                            path -> normalizeKey(root.relativize(path)),
                            path -> path,
                            (existing, duplicate) -> existing,
                            TreeMap::new
                    ));
        }
    }

    private static CheckResult checkFile(String relativePath, Path file) throws IOException {
        List<String> allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> originalLines = new ArrayList<>();
        List<String> translateLines = new ArrayList<>();
        for (String line : allLines) {
            if (SptLineUtils.isOriginalLine(line)) {
                originalLines.add(line);
            } else if (SptLineUtils.isTranslateLine(line)) {
                translateLines.add(line);
            }
        }

        List<String> reportLines = new ArrayList<>();
        reportLines.add("");
        reportLines.add("====================================");
        reportLines.add(relativePath);
        reportLines.add("====================================");

        List<Violation> violations = new ArrayList<>();

        List<Violation> originViolations = validateOriginSpt(relativePath, originalLines);
        if (!originViolations.isEmpty()) {
            reportLines.add("× 原文列与蓝本不一致");
            originViolations.forEach(v -> {
                violations.add(v);
                v.messages().forEach(reportLines::add);
                reportLines.add("");
            });
        }

        int pairCount = Math.min(translateLines.size(), originalLines.size());
        for (int i = 0; i < pairCount; i++) {
            String translateLine = translateLines.get(i);
            String originalLine = originalLines.get(i);
            CheckerPipeline.evaluateTranslateLine(translateLine, originalLine, i)
                    .ifPresent(v -> {
                        violations.add(v);
                        reportLines.add(translateLine);
                        v.messages().forEach(reportLines::add);
                        reportLines.add("");
                    });
        }

        if (translateLines.size() != originalLines.size()) {
            String message = "译文与原文行数不一致：译文 " + translateLines.size() + " 行，原文 " + originalLines.size() + " 行";
            reportLines.add(message);
        }

        return new CheckResult(relativePath,
                String.join(System.lineSeparator(), reportLines),
                List.copyOf(violations));
    }

    private static List<Violation> validateOriginSpt(String relativePath, List<String> originSpt) throws IOException {
        Path originFile = sptOriginFiles.get(relativePath);
        List<Violation> violations = new ArrayList<>();
        if (originFile == null || !Files.exists(originFile)) {
            violations.add(new Violation(null, false, "", -1, List.of("未找到对应的蓝本文件")));
            return violations;
        }

        List<String> blueprintLines = Files.readAllLines(originFile, StandardCharsets.UTF_8).stream()
                .filter(SptLineUtils::isOriginalLine)
                .collect(Collectors.toList());

        if (originSpt.size() != blueprintLines.size()) {
            violations.add(new Violation(null, false, "", -1,
                    List.of("原文行数不一致：译文文件 " + originSpt.size() + " 行，蓝本 " + blueprintLines.size() + " 行")));
        }

        int max = Math.max(originSpt.size(), blueprintLines.size());
        for (int i = 0; i < max; i++) {
            String originLine = i < originSpt.size() ? originSpt.get(i) : null;
            String blueprintLine = i < blueprintLines.size() ? blueprintLines.get(i) : null;

            if (originLine == null && blueprintLine != null) {
                String id = SptLineUtils.extractId(blueprintLine).orElse(null);
                violations.add(new Violation(id, false, blueprintLine, i,
                        List.of("译文文件缺少第 " + (i + 1) + " 行原文，请补齐。")));
                continue;
            }
            if (originLine != null && blueprintLine == null) {
                String id = SptLineUtils.extractId(originLine).orElse(null);
                violations.add(new Violation(id, false, originLine, i,
                        List.of("蓝本缺少第 " + (i + 1) + " 行原文，请核对蓝本或译文。")));
                continue;
            }
            if (originLine != null && blueprintLine != null && !originLine.equals(blueprintLine)) {
                String id = SptLineUtils.extractId(originLine)
                        .orElseGet(() -> SptLineUtils.extractId(blueprintLine).orElse(null));
                violations.add(new Violation(id, false, originLine, i, List.of(
                        "原文行与蓝本不一致",
                        "蓝本：" + blueprintLine,
                        "当前：" + originLine
                )));
            }
        }

        return violations;
    }

    private static void writePatchFile(CheckResult result) throws IOException {
        Path patchFile = RESULT_PATH.resolve(buildPatchFileName(result.relativePath()));
        StringBuilder builder = new StringBuilder();
        builder.append("# FILE: ").append(result.relativePath()).append(System.lineSeparator());

        List<Violation> violations = result.violations();
        if (violations.isEmpty()) {
            builder.append("# STATUS: CLEAN").append(System.lineSeparator());
        } else {
            for (Violation violation : violations) {
                if (!violation.hasId()) {
                    builder.append("# WARN: 缺少可用的锚点ID，无法生成补丁项").append(System.lineSeparator());
                    builder.append("# RAW: ").append(violation.rawLine()).append(System.lineSeparator());
                    builder.append(System.lineSeparator());
                    continue;
                }

                // 默认用原行，若触发“！？”修正规则则替换为修正后的整行
                String lineOut = violation.rawLine();

                builder.append("# ID: ").append(violation.id()).append(System.lineSeparator());
                for (String message : violation.messages()) {
                    builder.append("# ERR: ").append(message).append(System.lineSeparator());

                    // 命中该错误 -> 生成修正行（只移动全角“？”）
                    if (message.contains("问号应该在感叹号前且应该都为全角")) {
                        String fixed = rewriteBangQuestion(lineOut);
                        if (!fixed.equals(lineOut)) {
                            lineOut = fixed; // 用修正后的整行写入补丁
                        }
                    }
                }

                builder.append(lineOut).append(System.lineSeparator()).append(System.lineSeparator());
            }
        }

        Files.writeString(patchFile, builder.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    // 只移动“？”到其左侧连续“！”之前；只处理全角字符，不做其他归一化
    private static String rewriteBangQuestion(String fullLine) {
        int headerEnd = fullLine.indexOf(SptConstants.MARK_TRANSLATE_CHAR, 1);
        if (headerEnd == -1 || fullLine.length() <= headerEnd + 2) return fullLine;

        String prefix  = fullLine.substring(0, headerEnd + 2);
        String content = fullLine.substring(headerEnd + 2);

        String fixed = moveQBeforeBangRuns(content);
        return fixed.equals(content) ? fullLine : (prefix + fixed);
    }

    private static String moveQBeforeBangRuns(String s) {
        StringBuilder sb = new StringBuilder(s);
        int from = 0;
        boolean changed = false;

        while (true) {

            // 找下一枚全角问号
            int q = sb.indexOf("？", from);
            if (q == -1) break;

            int p = q - 1;

            // 左侧是感叹号串
            if (p >= 0 && sb.charAt(p) == '！') {
                int left = p;

                // 找到串起点前一个位置
                while (left >= 0 && sb.charAt(left) == '！') {
                    left--;
                }

                // 串起点
                int insertPos = left + 1;

                // 在串起点插入问号
                sb.insert(insertPos, '？');

                // 删除原问号（考虑插入位移）
                int removeIndex = q + (insertPos <= q ? 1 : 0);
                sb.deleteCharAt(removeIndex);

                changed = true;

                // 继续向后处理下一组
                from = insertPos + 1;
            } else {

                // 这枚问号不属于“！…？”组
                from = q + 1;
            }
        }
        return changed ? sb.toString() : s;
    }

    private static String buildPatchFileName(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        String base = normalized;
        if (base.endsWith(SptConstants.PATCH_FILE_SUFFIX)) {
            base = base.substring(0, base.length() - SptConstants.PATCH_FILE_SUFFIX.length());
        }
        String safe = base.replace("/", SptConstants.PATCH_PATH_SEPARATOR);
        return SptConstants.PATCH_FILE_PREFIX + safe + SptConstants.PATCH_FILE_SUFFIX;
    }

    private static String normalizeKey(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private record CheckResult(String relativePath, String report, List<Violation> violations) {
    }
}
