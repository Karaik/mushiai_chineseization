package checkSolution;

import java.util.regex.Pattern;

public class FormatChecker {

    /**
     * 格式校验
     * 例句对（普通文本）
     * ○00933|12D9C4|07A○ あちこちに新しい家が見られるようになり、夢美の[\r][\n]住むマンションの外観など、今では古びた雰囲気さ[\r][\n]え漂うようになっていた。[\r][\n]
     * ●00933|12D9C4|07A● 随处都盖起了新的房子，[\r][\n]梦美现在住的公寓从外观上看也带上了一种老旧的氛围感。[\r][\n]
     * 规则：（仅对●开头的译文行做校验）
     * 1.原文开头标识用一对○包裹，译文用一对●包裹，中间的内容一致
     * 2.开头标识过后，必定有一个且仅有一个半角空格分隔，后面的全是正文文本
     * 3.游戏内用[\r][\n]分行，一行的字符在不包括分隔符的情况下不超过24个字
     *
     * 例句对（对话框文本）
     * ○00961|12E0F8|050○ 優斗[\r][\n]「明日は休みだけど、どうする？[\r][\n]　久しぶりにどこか遊びにでも行くかい？」[\r][\n]
     * ●00961|12E0F8|050● 优斗[\r][\n]「明天是休息，你有什么打算吗？[\r][\n]　要不要久违地一起去哪玩玩吗？」[\r][\n]
     * 4.如果第二行的开始，与最后一行的结尾能组成一对「」或『』，则代表此文本为对话文本，进行对话文本的格式校验
     * 5.「只能和」组成一对，且必须能组成一对，否则错误，『』同理，一对对话框内的文本叫语音句子
     * 6.语音句子内，除了第一行，都必须带有且仅有一个全角空格
     * 7.最后一行的」或』前，可以有标点，但标点不能是。、，和全角或半角空格
     *
     */
    public static String sptCheckFormat(String line, String originalLine) {
        StringBuilder result = new StringBuilder();

        // 1. 校验头标记一致（规则1）
        if (!line.startsWith("●") || !originalLine.startsWith("○")) {
            CheckerPipeline.hasError = true;
            result.append("错误：开头标识不正确，应为●或○开头\n");
            return result.toString();
        }
        int headerEnd = line.indexOf('●', 1);
        if (headerEnd == -1 || !line.substring(1, headerEnd).equals(originalLine.substring(1, originalLine.indexOf('○', 1)))) {
            CheckerPipeline.hasError = true;
            result.append("错误：●...● 内的标识与原文不一致\n");
        }

        // 2. 校验●标识后必须有一个半角空格（规则2）
        if (line.length() <= headerEnd + 1 || line.charAt(headerEnd + 1) != ' ') {
            CheckerPipeline.hasError = true;
            result.append("错误：●标识后必须紧跟一个半角空格\n");
            return result.toString();
        }
        String content = line.substring(headerEnd + 2); // 跳过“●内容● 空格”
        String[] lines = content.split(Pattern.quote("[\\r][\\n]"));

        // 3. 校验普通文本每行不超过24字（规则3）
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].length() > 24) {
                CheckerPipeline.hasError = true;
                result.append("错误：第").append(i + 1).append("行超过24个字符，当前为").append(lines[i].length()).append("\n");
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
            // 5. 校验对话框符号配对（规则5）
            int open = 0, close = 0;
            for (String l : lines) {
                for (char c : l.toCharArray()) {
                    if (c == '「' || c == '『') open++;
                    if (c == '」' || c == '』') close++;
                }
            }
            if (open != close) {
                CheckerPipeline.hasError = true;
                result.append("错误：对话框符号数量不匹配\n");
            }

            // 6. 语音句子第二行开始必须包含且仅包含一个全角空格
            for (int i = 2; i < lines.length; i++) {
                long spaceCount = lines[i].chars().filter(c -> c == '　').count();
                if (spaceCount != 1) {
                    CheckerPipeline.hasError = true;
                    result.append("错误：第").append(i + 1).append("行开头应包含一个全角空格，当前为").append(spaceCount).append("个").append("\n");
                }
            }

            // 7. 最后一行结尾前符号检查
            String lastLine = lines[lines.length - 1];
            if (lastLine.endsWith("」") || lastLine.endsWith("』")) {
                if (lastLine.length() >= 2) {
                    char before = lastLine.charAt(lastLine.length() - 2);
                    if (before == '。' || before == '、' || before == '，' || before == ' ' || before == '　') {
                        CheckerPipeline.hasError = true;
                        result.append("错误：对话结尾符号前不能为 。、，或空格").append("\n");
                    }
                }
            }

        }

        return result.toString();
    }

}
