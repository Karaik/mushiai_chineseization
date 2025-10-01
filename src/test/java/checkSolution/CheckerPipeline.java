package checkSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 校验管线的统一入口。
 * 如果希望暂时关闭某一项校验，可直接在 {@link #evaluateTranslateLine(String, String, int)} 中注释掉对应的 addAll 语句。
 */
public final class CheckerPipeline {

    private CheckerPipeline() {
    }

    public static String sptFormatCheck(String line, String originalLine) {
        Optional<Violation> violation = evaluateTranslateLine(line, originalLine, -1);
        if (violation.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(System.lineSeparator())
                .append(line)
                .append(System.lineSeparator());
        for (String message : violation.get().messages()) {
            builder.append(message).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public static Optional<Violation> evaluateTranslateLine(String line, String originalLine, int lineIndex) {
        List<String> messages = new ArrayList<>();
        // 若不想执行格式规则，可注释掉下一行。
        messages.addAll(FormatChecker.sptCheckFormat(line, originalLine));
        // 若不想执行符号规则，可注释掉下一行。
        messages.addAll(SymbolChecker.sptCheckSymbol(line, originalLine));
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        String id = SptLineUtils.extractId(line).orElse(null);
        return Optional.of(new Violation(id, true, line, lineIndex, messages));
    }

    public static Optional<Violation> evaluateTranslateLine(String line, String originalLine) {
        return evaluateTranslateLine(line, originalLine, -1);
    }
}
