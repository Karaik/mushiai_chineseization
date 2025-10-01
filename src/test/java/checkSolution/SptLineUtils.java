package checkSolution;

import java.util.Optional;

import static checkSolution.SptConstants.MARK_ORIGINAL;
import static checkSolution.SptConstants.MARK_ORIGINAL_CHAR;
import static checkSolution.SptConstants.MARK_TRANSLATE;
import static checkSolution.SptConstants.MARK_TRANSLATE_CHAR;

/**
 * 行工具方法集中在这里，方便通过注释掉调用方的工具引用来关闭 ID 判定能力。
 */
public final class SptLineUtils {

    private SptLineUtils() {
    }

    /**
     * 提取行首锚点 ID，例如『●00933|12D9C4|07A●』会返回中间部分。
     * 若想短路 ID 判断，可在调用方注释掉对本方法的调用。
     */
    public static Optional<String> extractId(String line) {
        if (line == null || line.isEmpty()) {
            return Optional.empty();
        }
        char marker = line.charAt(0);
        if (marker != MARK_TRANSLATE_CHAR && marker != MARK_ORIGINAL_CHAR) {
            return Optional.empty();
        }
        int end = line.indexOf(marker, 1);
        if (end <= 1) {
            return Optional.empty();
        }
        return Optional.of(line.substring(1, end));
    }

    /**
     * 判断是否为译文行（● 开头）。
     */
    public static boolean isTranslateLine(String line) {
        return line != null && line.startsWith(MARK_TRANSLATE);
    }

    /**
     * 判断是否为原文行（○ 开头）。
     */
    public static boolean isOriginalLine(String line) {
        return line != null && line.startsWith(MARK_ORIGINAL);
    }
}
