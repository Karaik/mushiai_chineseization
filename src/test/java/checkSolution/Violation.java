package checkSolution;

import java.util.List;

/**
 * 校验得到的结构化信息；若想跳过补丁导出，可在调用方直接忽略这些记录。
 */
public record Violation(
        String id,
        boolean translateLine,
        String rawLine,
        int lineIndex,
        List<String> messages
) {

    public Violation {
        rawLine = rawLine == null ? "" : rawLine;
        messages = List.copyOf(messages);
    }

    public boolean hasId() {
        return id != null && !id.isBlank();
    }
}
