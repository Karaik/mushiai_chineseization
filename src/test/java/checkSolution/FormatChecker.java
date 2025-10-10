package checkSolution;

import java.util.ArrayList;
import java.util.List;

import static checkSolution.SptConstants.MARK_ORIGINAL;
import static checkSolution.SptConstants.MARK_ORIGINAL_CHAR;
import static checkSolution.SptConstants.MARK_TRANSLATE;
import static checkSolution.SptConstants.MARK_TRANSLATE_CHAR;
import static checkSolution.SptConstants.SPLIT_REGEX;

/**
 * 格式校验规则集合。
 * 需要临时关闭格式校验时，可在 {@link CheckerPipeline#evaluateTranslateLine(String, String, int)} 中
 * 注释掉对本方法的调用，或直接将整个方法体注释掉。
 *
 * 规则摘要：
 *  1. 原文行以○包裹，译文行以●包裹，锚点 ID 必须一致；
 *  2. ● 结束后必须紧跟一个半角空格，之后为正文；
 *  3. 正文使用 [\r][\n] 分行，每行不超过 24 个字符；
 *  4. 通过第二、最后一行的「」或『』判断是否为对话文本；
 *  5. 对话文本的引号需成对出现；
 *  6. 对话文本从第三行开始需包含且仅包含一个全角空格；
 *  7. 对话文本的结尾引号前不能为 。、，或空格。
 */
public final class FormatChecker {

    private FormatChecker() {
    }

    public static List<String> sptCheckFormat(String line, String originalLine) {
        List<String> errors = new ArrayList<>();
        if (line == null || originalLine == null) {
            errors.add("错误：缺少对应行用于格式校验");
            return errors;
        }

        if (!line.startsWith(MARK_TRANSLATE) || !originalLine.startsWith(MARK_ORIGINAL)) {
            errors.add("错误：开头标识不正确，应为●或○开头");
            return errors;
        }

        int headerEnd = line.indexOf(MARK_TRANSLATE_CHAR, 1);
        int originalHeaderEnd = originalLine.indexOf(MARK_ORIGINAL_CHAR, 1);

        // 1. 校验头标记一致（规则1）
        if (headerEnd == -1 || originalHeaderEnd == -1 ||
                !line.substring(1, headerEnd).equals(originalLine.substring(1, originalHeaderEnd))) {
            errors.add("错误：●...●内的标识与原文不一致");
        }

        if (headerEnd == -1) {
            return errors;
        }

        // 2. 校验●标识后必须有一个半角空格（规则2）
        if (line.length() <= headerEnd + 1 || line.charAt(headerEnd + 1) != ' ') {
            errors.add("错误：●标识后必须紧跟一个半角空格");
            return errors;
        }

        String content = line.substring(headerEnd + 2);
        String[] lines = content.split(SPLIT_REGEX);

        // 3. 校验普通文本每行不超过24字（规则3）
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 24) {
                errors.add("错误：第" + (i + 1) + "行超过24个字符，当前为 " + lines[i].length());
            }
        }

        // 4. 判断是否为对话文本（规则4）
        boolean isDialog = false;
        if (lines.length >= 2) {
            String first = lines[1].trim();
            String last = lines[lines.length - 1].trim();
            if ((first.startsWith("「") && last.endsWith("」")) ||
                    (first.startsWith("『") && last.endsWith("』"))) {
                isDialog = true;
            }
        }

        if (isDialog) {
            int open = 0;
            int close = 0;
            for (String l : lines) {
                for (char c : l.toCharArray()) {
                    if (c == '「' || c == '『') {
                        open++;
                    }
                    if (c == '」' || c == '』') {
                        close++;
                    }
                }
            }
            // 5. 校验对话框符号配对（规则5）
            if (open != close) {
                errors.add("错误：对话框符号数量不匹配");
            }

//            // 6. 语音句子第二行开始必须包含且仅包含一个全角空格
//            for (int i = 2; i < lines.length; i++) {
//                long spaceCount = lines[i].chars().filter(ch -> ch == '　').count();
//                if (spaceCount != 1) {
//                    errors.add("错误：第" + (i + 1) + "行开头应包含一个全角空格，当前为 " + spaceCount + " 个");
//                }
//            }

            // 7. 最后一行结尾前符号检查
            String lastLine = lines[lines.length - 1];
            if (lastLine.endsWith("」") || lastLine.endsWith("』")) {
                if (lastLine.length() >= 2) {
                    char before = lastLine.charAt(lastLine.length() - 2);
                    if (before == '。' || before == '，' || before == '、' || before == ' ' || before == '　') {
                        errors.add("错误：对话结尾符号前不能为 。、，或空格");
                    }
                }
            }

        }

        return errors;
    }

}
