package checkSolution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymbolChecker {

    /**
     * 符号校验（仅针对译文）
     * 1.不允许出现！？这样的配对，问号只能出现在感叹号前面，且为全角，不允许半角标点
     * 2.所有的波浪线全都为～ 其它种类的波浪线不允许出现
     * 3.如果一个逗号的前后两个字一样（如：我，我才没有），则应该为顿号（正确示例：我、我才没有）
     * 4.如果顿号的前后两个字不一样，也要提示错误
     * 5.所有的破折号全都为―（U+2015），且必须两两出现
     * 6.所有的半角符号全部都不支持
     * 7.译文中的任何引号（【】 「」 『』 ”” "" 等等），都应该为中文的引号（“”），且必须成对出现
     * 8.同一行中除了对话文本的开头，不允许出现半角或全角空格
     */
    public static String sptCheckSymbol(String line, String originalLine) {
        StringBuilder result = new StringBuilder();

        // 提取正文内容（跳过 ●xxx● 空格）
        int headerEnd = line.indexOf('●', 1);
        if (headerEnd == -1 || line.length() <= headerEnd + 2) return "";

        String content = line.substring(headerEnd + 2);
        String[] lines = content.split("\\[\\\\r]\\[\\\\n]");

        boolean isDialog = false;
        if (lines.length >= 2) {
            String first = lines[1].trim();
            String last = lines[lines.length - 1].trim();
            if ((first.startsWith("「") && last.endsWith("」")) ||
                    (first.startsWith("『") && last.endsWith("』"))) {
                isDialog = true;
            }
        }

        // 拼接去除换行的内容，供统一校验使用
        StringBuilder contentNoTag = new StringBuilder();
        for (String linePart : lines) {
            contentNoTag.append(linePart);
        }
        String fullText = contentNoTag.toString();

        // 1. 禁止“！？”或“?!”配对（全角或半角）
        if (fullText.contains("！？") || fullText.contains("!?") || fullText.contains("?!")) {
            CheckerPipeline.hasError = true;
            result.append("错误：问号应该在感叹号前且应该都为全角\n");
        }

        // 2. 禁止非法波浪线（仅允许全角 ～）
        char[] invalidTildes = {'~', '∼', '˜', '﹏', '〰'};
        for (char c : fullText.toCharArray()) {
            for (char bad : invalidTildes) {
                if (c == bad) {
                    CheckerPipeline.hasError = true;
                    result.append("错误：禁止使用非法波浪线 ‘").append(c).append("’，仅允许使用全角 ～\n");
                    break;
                }
            }
        }

        // 3. 顿号必须两字相同；逗号若两字相同应为顿号
        for (int i = 1; i < fullText.length() - 1; i++) {
            char prev = fullText.charAt(i - 1);
            char curr = fullText.charAt(i);
            char next = fullText.charAt(i + 1);

            if (curr == '、' && prev != next) {
                CheckerPipeline.hasError = true;
                result.append("错误：顿号 ‘、’ 前后两个字不一致，应为相同字\n");
            } else if (curr == '，' && prev == next) {
                CheckerPipeline.hasError = true;
                result.append("错误：逗号 ‘，’ 前后两个字相同，应为顿号 ‘、’\n");
            }
        }

        // 4. 破折号：仅允许使用 ―（U+2015），其它破折号全不允许，且必须成对出现
        long countValidDash = content.chars().filter(c -> c == '―').count();
        if (countValidDash != 0 && countValidDash != 2) {
            CheckerPipeline.hasError = true;
            result.append("错误：破折号 ‘―’ 必须成对出现，且应为两个\n");
        }
        for (char ch : new char[]{'‐', '-', '–', '—', 'ー'}) { // 包含 U+2010 到 U+2014
            if (content.indexOf(ch) != -1) {
                CheckerPipeline.hasError = true;
                result.append("错误：出现非法破折号 ‘").append(ch).append("’，仅允许使用 ―（U+2015）\n");
            }
        }

        // 5. 禁止所有半角符号（ASCII 符号）
        for (char c : fullText.toCharArray()) {
            if (c >= 0x21 && c <= 0x7E && "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(c) != -1) {
                CheckerPipeline.hasError = true;
                result.append("错误：出现非法半角符号 ‘").append(c).append("’\n");
            }
        }

        // 6. 禁止非法引号，仅允许 “”
        boolean skippedQuotes = false;
        for (int i = 0; i < fullText.length(); i++) {
            char c = fullText.charAt(i);

            // 忽略对话框起始引号「」或『』（如果是对话）
            if (isDialog && !skippedQuotes) {
                if ((c == '「' || c == '『') && i < fullText.length() - 1) {
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
                CheckerPipeline.hasError = true;
                result.append("错误：禁止使用引号 ‘").append(c).append("’，仅允许使用中文引号 “ ”\n");
            }
        }
        long quoteOpen = fullText.chars().filter(c -> c == '“').count();
        long quoteClose = fullText.chars().filter(c -> c == '”').count();
        if (quoteOpen != quoteClose) {
            CheckerPipeline.hasError = true;
            result.append("错误：中文引号 ‘“’ 与 ‘”’ 不成对\n");
        }

        // 7. 非首行不得包含空格（全角或半角），空格在开头如果是全角空格也可以
        for (int i = 1; i < lines.length; i++) {
            String lineText = lines[i];
            boolean hasHalfWidthSpace = lineText.contains(" ");
            boolean hasIllegalFullWidthSpace = lineText.contains("　") && !lineText.startsWith("　");

            if (hasHalfWidthSpace || hasIllegalFullWidthSpace) {
                CheckerPipeline.hasError = true;
                result.append("错误：第").append(i + 1).append("行包含非法空格（半角或非开头的全角空格）\n");
            }
        }

        // 8. 省略号应该是两个两个一组出现……
        Matcher matcher = Pattern.compile("…+").matcher(content);
        while (matcher.find()) {
            int len = matcher.group().length();
            if (len % 2 != 0) {
                CheckerPipeline.hasError = true;
                result.append("错误：省略号连续出现 ‘…’ 的数量应该为2的倍数，当前为 ")
                        .append(len).append("个\n");
            }
        }

        return result.toString();
    }

}
