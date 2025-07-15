package checkSolution;

import static checkSolution.FormatChecker.sptCheckFormat;
import static checkSolution.SymbolChecker.sptCheckSymbol;

public class CheckerPipeline {

    public static boolean hasError = false; // 全局标志

    // line为传进来的译文，originalLine为原文
    public static String sptFormatCheck(String line, String originalLine) {
        hasError = false; // 每次调用时初始化
        StringBuilder result = new StringBuilder();

        // 1. 格式检查
        String formatCheckResult = sptCheckFormat(line, originalLine);

        // 2. 符号使用检查
        String symbolCheckResult = sptCheckSymbol(line, originalLine);


        if (hasError) {
            result.append(formatCheckResult);
            result.append(symbolCheckResult);
            return "\n" + line + "\n" + result;
        }
        return result.toString();
    }

}
