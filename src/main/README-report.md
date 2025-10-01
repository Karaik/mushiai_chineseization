# 保存策略风险分析报告

## 1. 样本损坏观察
- 受损文件: src/main/resources/error/01B.spt_error.txt，大小 683,197 B。
- 统计：首段 8,192 B 全为 NUL，接着约 80 KiB 正常 UTF-8 文本，之后 593,085 B 再次变为 NUL。
- 非零数据集中在偏移 0x2000–0x15FFF，推测实际写入被中断，尾部文件空间被零填充。
- 内容中可见 "[\\r][\\n]" 等逃逸符号，说明原文件为 UTF-8 文本并包含控制符字面量。

## 2. 事故成因推测
- 保存流程为“truncate → 顺序写入 → close”，崩溃发生在写入批次期间。
- 写入流程使用缓冲（典型 8 KiB），首个缓冲尚未从用户态写入磁盘即发生断电，导致文件头部被零填。
- Windows 上如果 FileChannel 触达更高偏移（或调用 setLength），NTFS 会对未写入区间返回零，形成大片 NUL。
- 没有 fsync/force，crash 时元数据与数据页都可能处于未落盘状态，增加损坏概率。

## 3. 原子保存与 .bak MVP 思路
1. 生成同目录临时文件（遵循目标文件权限），完整写入并 FileChannel.force(true)。
2. 原文件存在时先生成/更新 *.bak（优先尝试硬链接，不支持则复制）。
3. 调用 Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)；若不支持 ATOMIC_MOVE，退回 StandardCopyOption.REPLACE_EXISTING 后再强退写入目录 fsync。
4. Windows 可选：JNA 引入 ReplaceFileW，原子替换同时写出 *.bak，并使用 REPLACEFILE_WRITE_THROUGH 保证写穿。

## 4. 快速损坏检测建议
- 检查前 4 KiB 与尾 4 KiB 是否出现连续 ≥1 KiB 的 NUL；超过阈值直接判定异常。
- 统计文件内 NUL 比例与 UTF-8 解码错误率，超过 1% 维度触发恢复提示。
- 比对目标文件与 *.bak/历史版本的大小和 SHA-256，如出现倒回或突增也提示用户。
- 将检测策略封装在读取服务，返回“可能损坏 + 原因 + 可恢复路径”给 UI。

## 5. 后续验证重点
- 故障注入：在 flush 前/force 前模拟进程终止或电源故障，确认最终磁盘状态只出现新旧两个版本。
- 覆盖磁盘满、权限拒绝、杀毒/索引器共享冲突，验证重试机制与用户提示。
- 跨平台：验证 NTFS、APFS、ext4、FAT32 对 ATOMIC_MOVE、硬链接、写穿的行为差异。
- 自动化：建立保存过程 metrics（耗时、重试次数、回退路径）以便回归监控。

## 6. 待补充源码
- SaveService / SafeSaver（当前保存实现）
- FileDialogs / PathUtils（路径策略、临时文件命名）
- BackupManager / VersionStore / AutosaveTask（若已存在）
- ShutdownHook / SessionManager（用于历史版本落盘）

## 7. 里程碑
- MVP（本周）：实现临时文件写入 + force + 原子替换 + .bak 同步策略；加入基本损坏检测与 UI 提示。
- 增强阶段：历史版本目录化管理、自动保存任务、孤儿 tmp/autosave 启动时扫描、跨进程锁与重试机制、写入性能监测。
