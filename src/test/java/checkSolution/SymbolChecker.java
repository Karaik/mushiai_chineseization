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

        // 1. 核心格式判断模块
        boolean isDialogWithSpeaker = false;
        boolean isMonologue = false;

        // 优先判断是否为“带名字的对话”（结构 A）
        // 特征：至少有两行，且第二行以「或『开头
        if (segments.length >= 2) {
            String secondLine = segments[1].trim();
            String lastLine = segments[segments.length - 1].trim();
            if ((secondLine.startsWith("「") && lastLine.endsWith("」")) ||
                    (secondLine.startsWith("『") && lastLine.endsWith("』")) ||
                    (secondLine.startsWith("（") && lastLine.endsWith("）"))) {
                isDialogWithSpeaker = true;
            }
        }

        // 如果不是“带名字的对话”，再判断是否为“内心独白”（结构 B）
        // 特征：第一行以（开头，最后一行以）结尾
        if (!isDialogWithSpeaker && segments.length > 0) {
            String firstLine = segments[0].trim();
            String lastLine = segments[segments.length - 1].trim();
            if (firstLine.startsWith("（") && lastLine.endsWith("）")) {
                isMonologue = true;
            }
        }

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
                errors.add("错误：出现非法半角符号‘ " + c + "’");
            }
        }

        // 6. 禁止非法引号，仅允许 “”
        boolean skippedQuotes = false;
        for (int i = 0; i < fullText.length(); i++) {
            char c = fullText.charAt(i);

            // 忽略对话框起始引号「」或『』（如果是对话）
            // MODIFICATION START: Changed `isDialog` to `isDialogWithSpeaker`
            if (isDialogWithSpeaker && !skippedQuotes) {
                // MODIFICATION END
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

        // 7. 整合后的空格与缩进规则
        for (int i = 0; i < segments.length; i++) {
            String lineText = segments[i];

            // 7.1. 通用规则：禁止半角空格和行尾全角空格
            if (lineText.indexOf(' ') >= 0) {
                errors.add("错误：第 " + (i + 1) + " 行包含半角空格");
            }
            if (!lineText.isEmpty() && lineText.charAt(lineText.length() - 1) == '　') {
                errors.add("错误：第 " + (i + 1) + " 行不允许以全角空格结尾");
            }

            // 7.2. 缩进规则：根据文本类型进行判断
            int leadingFW = 0;
            while (leadingFW < lineText.length() && lineText.charAt(leadingFW) == '　') {
                leadingFW++;
            }

            if (isDialogWithSpeaker) {
                // 带名字的对话：第1行(名字)和第2行(对话开头)不缩进，后续行必须缩进
                if (i == 0) { // 第1行是名字，不允许缩进
                    if (leadingFW > 0) errors.add("错误：第 " + (i + 1) + " 行（说话人）不允许以空格开头");
                } else if (i >= 2) { // 对话从第3行起 (数组索引为2) 必须缩进
                    if (leadingFW < 1) errors.add("错误：第 " + (i + 1) + " 行（对话内容）开头必须至少有 1 个全角空格");
                }
            } else if (isMonologue) {
                // 内心独白：第1行不缩进，后续行必须且仅有1个空格
                if (i >= 1) {
                    if (leadingFW != 1) {
                        errors.add("错误：第 " + (i + 1) + " 行（内心独白）开头必须有且仅有1个全角空格 (当前 " + leadingFW + " 个)");
                    }
                }
            } else {
                // 普通文本：任何行都不允许开头有空格
                if (leadingFW > 0) {
                    errors.add("错误：第 " + (i + 1) + " 行（普通文本）不允许以空格开头");
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

        // 10. 检查换行符前是否有标点符号，标点符号只能为以下的几个
        // ，。……～！？―」』）”、
        // 如果不是，就要像之前的check规则一样报错，显示出换行符前的第一个字符是什么
        String allowed = "，。……～！？―」』）”、";
        int startIdx = isDialogWithSpeaker ? 1 : 0; // 如果是带名字对话，跳过名字行
        for (int i = startIdx; i < segments.length; i++) {
            String s = segments[i];
            if (s == null || s.isEmpty()) continue;

            // 找到最后一个非空格字符的索引
            int idx = s.length() - 1;
            while (idx >= 0 && s.charAt(idx) == '　') idx--;
            if (idx < 0) continue; // 整行都是空格

            char innerLast = s.charAt(idx);

            // 检查是否为呐喊/长音的情况
            // 条件1: 当前行不能是整个文本块的最后一行
            // 条件2: 行尾至少有4个连续相同的字符 (例如: 哦哦哦哦)
            boolean isRepetitiveShout = false;
            if (i < segments.length - 1) { // 条件1
                // 确保有足够长度进行检查 (索引至少为3)
                if (idx >= 3) {
                    if (s.charAt(idx - 1) == innerLast &&
                            s.charAt(idx - 2) == innerLast &&
                            s.charAt(idx - 3) == innerLast) {
                        isRepetitiveShout = true;
                    }
                }
            }

            // 如果不满足豁免条件，并且行尾字符不在允许列表中，则报错
            if (!isRepetitiveShout && allowed.indexOf(innerLast) == -1) {
                errors.add("错误：第" + (i + 1) + "行换行符前应为标点（仅允许 " + allowed + "），当前为‘" + innerLast + "’");
            }
        }

        return errors;
    }

}