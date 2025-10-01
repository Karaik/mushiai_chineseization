package checkSolution;

import java.util.regex.Pattern;

/**
 * SPT 校验公共常量集中在此处；若要临时停用相关能力，可以在调用处整体注释对该类的依赖。
 */
public final class SptConstants {

    private SptConstants() {
    }

    public static final String SPLIT_TOKEN = "[\\r][\\n]";
    public static final String SPLIT_REGEX = Pattern.quote(SPLIT_TOKEN);

    public static final String MARK_TRANSLATE = "\u25CF"; // ●
    public static final String MARK_ORIGINAL = "\u25CB";  // ○
    public static final char MARK_TRANSLATE_CHAR = '\u25CF';
    public static final char MARK_ORIGINAL_CHAR = '\u25CB';

    public static final String RESULT_DIRECTORY = "result";
    public static final String PATCH_FILE_PREFIX = "patch.";
    public static final String PATCH_FILE_SUFFIX = ".spt.txt";
    public static final String PATCH_PATH_SEPARATOR = "___";
    public static final String REPORT_FILE_NAME = "report.all.txt";
}
