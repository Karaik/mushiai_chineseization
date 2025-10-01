package com.karaik.scripteditor.helper;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * 最小改动版：仅做“备份 + 原子替换 + 成功后删除 .bak”。
 * 语义：
 *  - 保存前：若 target 存在，且不存在旧 .bak，则生成 target.bak（硬链优先，不行再复制）。
 *  - 将内容写入同目录的临时文件，并 FileChannel.force(true) 落盘。
 *  - 用 ATOMIC_MOVE 原子替换到 target（不支持时退化为普通替换）。
 *  - 若整个保存流程成功，最后删除 .bak（若存在）。
 *  - 若流程中途崩溃/异常，则 .bak 会保留，作为“可恢复点”。
 */
public final class CrashSafeFileSaver {

    private CrashSafeFileSaver() {}

    public static void saveWithBak(List<SptEntry> entries, File targetFile) throws IOException {
        Path target = targetFile.toPath();
        Path dir = target.getParent() != null ? target.getParent() : Paths.get(".");
        String base = target.getFileName().toString();

        Path tmp = dir.resolve(base + ".tmp-" + UUID.randomUUID());
        Path bak = dir.resolve(base + ".bak");

        boolean createdBakThisTime = false;

        // 1) 如 target 存在，且当前没有 .bak（说明上次没有异常中断的标记），先做备份
        if (Files.exists(target) && !Files.exists(bak)) {
            try {
                Files.createLink(bak, target);      // 同卷/支持硬链时最快 & 原子
                createdBakThisTime = true;
            } catch (UnsupportedOperationException | IOException e) {
                // 跨卷 / FAT/exFAT 等不支持硬链，回退到复制
                try {
                    Files.copy(target, bak, REPLACE_EXISTING, COPY_ATTRIBUTES);
                    createdBakThisTime = true;
                } catch (IOException ex) {
                    // 备份失败不阻断保存（尽量不扩大变更面），但建议记录日志
                    // System.err.println("Backup .bak failed: " + ex.getMessage());
                }
            }
        }
        // 如果 .bak 已经存在，我们保留它（不要覆盖），这样若是“上次异常留下的”，这次失败也不影响恢复点。

        // 2) 写临时文件 & 落盘
        try {
            // 你已有的写出实现：复用 SptWriter，路径指向 tmp
            SptWriter.saveToFile(entries, tmp.toFile());
            try (FileChannel ch = FileChannel.open(tmp, WRITE)) {
                ch.force(true); // 强制把数据与元数据刷盘
            }
        } catch (Exception e) {
            // 写 tmp 失败则清理 tmp，并把异常抛上去
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            if (e instanceof IOException) throw (IOException)e;
            throw new IOException(e);
        }

        // 3) 原子替换 tmp -> target
        try {
            Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // 某些 FS/跨卷不支持 ATOMIC_MOVE，退化为普通替换（此时仍有 .bak 兜底）
            Files.move(tmp, target, REPLACE_EXISTING);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }

        // 4) 整个保存流程成功后：删除 .bak（不管这次是不是我们创建的）
        //    —— 满足“成功后 .bak 不留，只有异常/崩溃时 .bak 会遗留”的需求
        try { Files.deleteIfExists(bak); } catch (IOException ignore) {}
    }

    /** 检查是否有同名 .bak（供打开时提示用） */
    public static boolean hasBak(File targetFile) {
        Path bak = bakOf(targetFile.toPath());
        return Files.exists(bak);
    }

    /** 恢复：用 .bak 覆盖回 target，然后删除 .bak */
    public static void restoreFromBak(File targetFile) throws IOException {
        Path target = targetFile.toPath();
        Path bak = bakOf(target);
        if (!Files.exists(bak)) return;
        Files.copy(bak, target, REPLACE_EXISTING, COPY_ATTRIBUTES);
        try { Files.deleteIfExists(bak); } catch (IOException ignore) {}
    }

    /** 仅用于控制器侧做路径判断 */
    public static Path bakOf(Path target) {
        Path dir = target.getParent() != null ? target.getParent() : Paths.get(".");
        return dir.resolve(target.getFileName().toString() + ".bak");
    }
}
