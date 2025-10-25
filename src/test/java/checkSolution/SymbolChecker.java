package checkSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static checkSolution.SptConstants.MARK_TRANSLATE_CHAR;
import static checkSolution.SptConstants.SPLIT_REGEX;

/**
 * 符号规则校验集合。
 * 若暂时不需要符号检查，可在 {@link CheckerPipeline#evaluateTranslateLine(String, String, int)} 中
 * 注释掉对本方法的调用，或将下方实现整体注释。
 *
 * 规则摘要：
 *  1. 禁止出现“！？”或半角 ?! 组合；
 *  2. 仅允许全角 ～ 波浪线；
 *  3. 顿号前后必须是同一字符；
 *  4. 若逗号前后字符相同则应改为顿号；
 *  5. 仅允许全角破折号 ― 且需成对出现；
 *  6. 禁止所有半角符号；
 *  7. 引号必须使用中文“”并保持成对；
 *  8. 除对话开头外禁止出现半角或非开头全角空格；
 *  9. 省略号需要成双成对出现。
 */
public final class SymbolChecker {

    private static final Pattern ELLIPSIS_PATTERN = Pattern.compile("…+");

    private SymbolChecker() {
    }

    public static List<String> sptCheckSymbol(String line, String originalLine) {
        List<String> errors = new ArrayList<>();
        if (line == null) {
            return errors;
        }

        int headerEnd = line.indexOf(MARK_TRANSLATE_CHAR, 1);
        if (headerEnd == -1 || line.length() <= headerEnd + 2) {
            return errors;
        }

        String content = line.substring(headerEnd + 2);
        String[] segments = content.split(SPLIT_REGEX);

        boolean isDialog = determineDialog(segments);

        // 拼接去除换行的内容，供统一校验使用
        StringBuilder contentNoTag = new StringBuilder();
        for (String segment : segments) {
            contentNoTag.append(segment);
        }
        String fullText = contentNoTag.toString();

        // 1. 禁止“！？”或“?!”配对（全角或半角）
        if (fullText.contains("！？") || fullText.contains("!?") || fullText.contains("?!")) {
            errors.add("错误：问号应该在感叹号前且应该都为全角");
        }

        // 2. 禁止非法波浪线（仅允许全角 ～）
        char[] invalidTildes = {'~', '∼', '˜', '﹏', '〰'};
        for (char c : fullText.toCharArray()) {
            for (char bad : invalidTildes) {
                if (c == bad) {
                    errors.add("错误：禁止使用非法波浪线‘" + bad + "’，仅允许使用全角“～”");
                    break;
                }
            }
        }

        // 3. 顿号必须两字相同；逗号若两字相同应为顿号
//        for (int i = 1; i < fullText.length() - 1; i++) {
//            char prev = fullText.charAt(i - 1);
//            char curr = fullText.charAt(i);
//            char next = fullText.charAt(i + 1);
//
//            if (curr == '、' && prev != next) {
//                errors.add("错误：顿号‘、’前后两个字不一致，应为相同字");
//                break;
//            } else if (curr == '，' && prev == next) {
//                errors.add("错误：逗号‘，’前后两个字相同，应为顿号‘、’");
//                break;
//            }
//        }

        // 4. 破折号：仅允许使用 ―（U+2015），其它破折号全不允许，且必须成对出现
        long countValidDash = content.chars().filter(c -> c == '―').count();
//        if (countValidDash != 0 && countValidDash != 2) {
//            errors.add("错误：破折号‘―’必须成对出现，且应为两个");
//        }
        for (char ch : new char[]{'‐', '-', '–', '—', 'ー'}) {
            if (content.indexOf(ch) != -1) {
                errors.add("错误：出现非法破折号‘" + ch + "’，仅允许使用“―”（U+2015）");
            }
        }

        // 5. 禁止所有半角符号（ASCII 符号）
        for (char c : fullText.toCharArray()) {
            if (c >= 0x21 && c <= 0x7E && "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(c) != -1) {
                errors.add("错误：出现非法半角符号‘" + c + "’");
            }
        }

        // 6. 禁止非法引号，仅允许 “”
        boolean skippedQuotes = false;
        for (int i = 0; i < fullText.length(); i++) {
            char c = fullText.charAt(i);

            // 忽略对话框起始引号「」或『』（如果是对话）
            if (isDialog && !skippedQuotes) {
                if ( ((c == '「' || c == '『') || (c == '（' || c == '）'))
                        && i < fullText.length() - 1) {
                    char end = (c == '「') ? '」' : '』';
                    int closing = fullText.indexOf(end, i + 1);
                    if (closing != -1) {
                        skippedQuotes = true;
                        i = closing;
                        continue;
                    }
                }
            }

            // 非法引号检测
            if ("「」『』【】\"'‘’".indexOf(c) != -1) {
                errors.add("错误：禁止使用引号‘" + c + "’，仅允许使用中文引号“”，和全角括号（）");
            }
        }
        long quoteOpen = fullText.chars().filter(ch -> ch == '“').count();
        long quoteClose = fullText.chars().filter(ch -> ch == '”').count();
        if (quoteOpen != quoteClose) {
            errors.add("错误：中文引号‘“’与‘”’不成对");
        }

        // 7. 空格规则：禁止任何半角空格；全角空格不得出现在句尾。
        //    特例：若为对话文本，从第3行开始（segments[0]是说话人，因此阈值 i>=2），
        //    只统计“开头”的全角空格数量用于缩进判断；句中出现的全角空格不计入缩进、且允许存在。
        String first = segments.length > 1 ? segments[1].trim() : "";
        String last  = segments.length > 1 ? segments[segments.length - 1].trim() : "";
        boolean dialogByBrackets =
                ((first.startsWith("「") && last.endsWith("」")) ||
                        (first.startsWith("『") && last.endsWith("』")) ||
                        (first.startsWith("（") && last.endsWith("）")));
        boolean dialogLike = isDialog || dialogByBrackets;

        for (int i = 1; i < segments.length; i++) {
            String lineText = segments[i];

            // 半角空格：任何位置都不允许
            if (lineText.indexOf(' ') >= 0) {
                errors.add("错误：第" + (i + 1) + "行包含半角空格");
            }

            // 仅统计“开头”的全角空格数量（U+3000）；句中的全角空格不计入
            int leadingFW = 0;
            while (leadingFW < lineText.length() && lineText.charAt(leadingFW) == '　') {
                leadingFW++;
            }

            // 句尾全角空格不允许
            if (!lineText.isEmpty() && lineText.charAt(lineText.length() - 1) == '　') {
                errors.add("错误：第" + (i + 1) + "行不允许以全角空格结尾");
            }

            boolean mustIndent = dialogLike && i >= 2; // 对话从第3行起必须缩进（至少 1 个全角空格）
            if (mustIndent) {
                if (leadingFW < 1) {
                    errors.add("错误：第" + (i + 1) + "行（对白第3行起）开头必须至少 1 个全角空格");
                }
                // 允许 >=1 个开头全角空格；不限制上限
            } else {
                if (leadingFW > 0) {
                    errors.add("错误：第" + (i + 1) + "行不允许以全角空格开头");
                }
            }
        }

        // 8. 省略号应该是两个两个一组出现……
//        Matcher matcher = ELLIPSIS_PATTERN.matcher(content);
//        while (matcher.find()) {
//            int len = matcher.group().length();
//            if (len % 2 != 0) {
//                errors.add("错误：省略号连续出现‘…’的数量应该为2的倍数，当前为 " + len + " 个");
//            }
//        }

        // 9. 追加空格规则
        // 存在以下这样的文本
        // ●03169|0AB808|05E● （正确地说应该是接近拷问，[\r][\n]　肉体的破损状况非常严重。不想细写）[\r][\n]
        // 也就是第一行不带名字的，纯粹的内心独白文本，文本特点在于，第一行一定是“（”，最后一个换行符 [\r][\n] 前一定是 “）”
        // 在这种情况下，需要像规则 7 一样，查一下第一行以外的行数的第一个字符，是否有且只有一个全角空格
        if (segments.length > 1) {
            String firstLineTrim = segments[1].trim();
            String lastLineTrim  = segments[segments.length - 1].trim();
            boolean isMonologue =
                    firstLineTrim.startsWith("（") && lastLineTrim.endsWith("）");

            if (isMonologue) {
                for (int i = 2; i < segments.length; i++) {
                    String lineText = segments[i];
                    int leadingFW = 0;
                    while (leadingFW < lineText.length() && lineText.charAt(leadingFW) == '　') {
                        leadingFW++;
                    }
                    if (leadingFW != 1) {
                        errors.add("错误：第" + (i + 1) + "行（内心独白第2行起）开头必须且仅 1 个全角空格（当前 "
                                + leadingFW + " 个）");
                    }
                    // 句尾全角空格依旧不允许
                    if (!lineText.isEmpty() && lineText.charAt(lineText.length() - 1) == '　') {
                        errors.add("错误：第" + (i + 1) + "行不允许以全角空格结尾");
                    }
                }
            }
        }

        // 10. 检查换行符前是否有标点符号，标点符号只能为以下的几个
        // ，。……～！？―」』）
        // 如果不是，就要像之前的check规则一样报错，显示出换行符前的第一个字符是什么
        String allowed = "，。……～！？―」』）”";
        int startIdx = dialogLike ? 2 : 1;
        for (int i = startIdx; i < segments.length; i++) {
            String s = segments[i];
            if (s == null || s.isEmpty()) continue;

            int idx = s.length() - 1;
            while (idx >= 0 && s.charAt(idx) == '　') idx--;
            if (idx < 0) continue;

            char innerLast = s.charAt(idx);
            if (allowed.indexOf(innerLast) == -1) {
                errors.add("错误：第" + (i + 1) + "行换行符前应为标点（仅允许 " + allowed + "），当前为‘" + innerLast + "’");
            }
        }

        return errors;
    }

    private static boolean determineDialog(String[] lines) {
        if (lines.length < 2) {
            return false;
        }
        String first = lines[1].trim();
        String last = lines[lines.length - 1].trim();
        return (first.startsWith("「") && last.endsWith("」")) ||
                (first.startsWith("『") && last.endsWith("』"));
    }
}
