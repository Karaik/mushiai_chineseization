**背景**
这是一个“文本编辑器/校对”项目。我们在 `src/test/java/PublicTest.java` 里有一个 `checkerPipeline()` 测试，会遍历 `src/main/resources/spt` 下的所有 `*.spt.txt`，依据校验规则生成错误报告。原始蓝本在 `src/main/resources/sptBluePrint`。
目前存在痛点：

* 所有检查结果被写到一个文件里，不便于逐文件修复；
* 我需要在编辑器里重新找到原文件再改，效率很低；
* 我希望“把有问题的行导出为一个**可直接编辑的补丁文件**（每个源文件对应一个 patch 文件）”，我修改 patch 文件后，执行一个 **patch** 方法即可把修正回写到 `resources/spt` 对应的源文件中。

**目标**

1. 修复现有 `checkPipeline` 的不合理点（见后面的“需修复问题”）。
2. 改造检查流程：**每个源文件**检查完成后，导出一个 `patch.<源文件名>.spt.txt` 到 `src/main/resources/result/` 目录，内含“该文件所有需要修复的行（可直接改）+ 简要错误说明（注释行）”。
3. 新增一个可执行的 **patch 方法**（放在 `src/test/java` 下的 `PublicPatch.java` 或直接新增 `PublicTest#patch()` 测试方法均可），读取 `resources/result/patch.*.spt.txt`，把我在 patch 文件中改动过的行，**按锚点精确替换**回 `resources/spt` 下的对应文件。
4. 我运行 `checkerPipeline()` 后，会在 `resources/result` 下为每个源文件得到一个 patch 文件；我只需编辑这些 patch 文件，再运行 `patch()` 方法，就能把修正自动回写。
5. 允许在 `resources/spt` 中直接修改（可先自动做 `.bak` 备份）。

---

## 需修复问题（请按此逐条改造）

1. **单一结果文件不可用**

    * 现状：`checkerPipeline()` 把所有文件的检查输出汇总写入 `src/main/resources/result.txt`。
    * 期望：改为**逐文件导出**到 `src/main/resources/result/patch.<file>.spt.txt`。
    * 方案：遍历时对每个文件分别构建报告与“可编辑补丁内容”，分别落盘。

2. **Map 键值冲突风险**

    * 现状：`findAllFiles()`/`findAllOriginFiles()` 使用 `File::getName` 作为 Map 键，若子目录内出现同名文件会冲突。
    * 期望：键改为**相对路径**（相对 `SPT_PATH`/`SPT_ORIGIN_PATH` 的路径字符串），保证唯一。
    * 方案：`Files.walk` 时保留 `path`，用 `SPT_PATH.relativize(path).toString()` 作为 key；`sptOriginFiles` 同理。

3. **注释与取行规则易混淆**

    * 现状：`PublicTest#checkFile` 的注释写“原文段落对比（● 行）”，但 `getOriginalLines()` 实际取的是 `startsWith("○")`。
    * 期望：**统一约定**：

        * 原文行：以 `○` 开头；
        * 译文行：以 `●` 开头。
    * 方案：修正文档注释，方法命名与含义保持一致。

4. **蓝本对齐的完整性校验不足**

    * 现状：`validateOriginSpt()` 仅逐行对比不等即报，但**未检查行数不一致**，且对越界异常直接吞掉。
    * 期望：

        * 先比较两侧行数，不一致时单独报错（包含双方行数）。
        * 越界异常不应吞掉；应记录“蓝本缺少/译文缺少第 N 行”的明确错误。

5. **分隔符常量重复**

    * 现状：`FormatChecker` 与 `SymbolChecker` 对 `[\r][\n]` 的分割写法不同（一个用 `Pattern.quote("[\\r][\\n]")`，一个用 `split("\\[\\\\r]\\[\\\\n]")`）。
    * 期望：抽成**公共常量**（如 `SPLIT_TOKEN = "[\\r][\\n]"` + `SPLIT_REGEX = "\\[\\\\r]\\[\\\\n]"`），统一调用，避免偏差。

6. **错误收集与可编辑导出**

    * 现状：`CheckerPipeline.sptFormatCheck` 若有错返回 `"\n" + line + "\n" + result`，不可直接形成“可编辑补丁”。
    * 期望：为“导出补丁”提供**结构化信息**：至少包括

        * 锚点 ID（即 `●...●`/`○...○` 中间的键，例如 `00933|12D9C4|07A`），
        * 原行文本（源文件中的整行），
        * 错误说明（多条），
        * 是否译文行或原文行标识。
    * 方案 A（最小改动）：在 `PublicTest#checkFile` 内，调用检查后**解析返回串**，把“第一行的带 `●`/`○` 的整行当作候选补丁行”，错误说明作为注释写在其上方。
    * 方案 B（更干净）：给 `CheckerPipeline` 新增一个返回 `Violation` 列表的方法，包含上述字段；原有文本输出仅用于人读。

---

## Patch 文件格式（请按此生成与解析）

* 位置：`src/main/resources/result/patch.<相对路径用___替代斜杠>.spt.txt`

    * 例：源文件 `spt/event/scene1.spt.txt` → `result/patch.spt___event___scene1.spt.txt`
    * 也可以用 `patch.<basename>.spt.txt`（若你已保证 key 唯一）；但推荐包含相对路径避免重名。
* 内容（可编辑）：按**条目**列出所有需修正的行；每条包含：

  ```
  # FILE: spt/event/scene1.spt.txt
  # ID: 00933|12D9C4|07A
  # ERR: 错误：●标识后必须紧跟一个半角空格
  # ERR: 错误：第2行超过24个字符，当前为 26
  ●00933|12D9C4|07A● （这里是可编辑的译文正文，分行用 [\r][\n]）

  # ID: 00961|12E0F8|050
  # ERR: 错误：对话结尾符号前不能为 。、，或空格
  ●00961|12E0F8|050● （这里是可编辑的另一条译文）
  ```
* 规则：

    * 我只需要直接改动以 `●` 开头的**整行文本**；
    * 注释行以 `#` 开头，仅供参考；
    * `# FILE:` 只在文件开头出现一次即可；`# ID:` 必须紧跟每条实际替换项；
    * 允许只包含译文 `●` 项；若也要修 `○` 原文，格式一致。

---

## 打补丁逻辑（PublicPatch.apply / PublicTest#patch）

* 读取 `src/main/resources/result` 下的所有 `patch.*.spt.txt`；
* 解析出目标源文件相对路径（来自 `# FILE:`，或由 patch 文件名反推）；
* 逐条读取 `# ID:` 锚点与其后的 `●...●`（或 `○...○`）整行文本；
* 在目标源文件中，按 **ID 精确匹配**（即 `●`/`○` 头标识之间的键完全相同）找到对应行，**用 patch 中的整行替换之**；

    * 匹配失败要报“未找到 ID：xxx”；
    * 替换前先复制源文件到同目录 `*.bak` 备份；写完后要删除，保证原子操作，避免写坏
* 写回 `resources/spt` 源文件（覆盖保存，保持 UTF-8）；
* 控制台输出：替换成功条数/失败条数汇总。

---

## 代码改动建议（最小侵入）

1. **PublicTest.java**

    * `checkerPipeline()`

        * 改为：

            * 构造 `Map<相对路径, File>`；
            * 对每个文件调用 `checkFile(File, 相对路径)`，它返回一个 `CheckResult`（含：人读报告字符串、用于补丁导出的条目列表）；
            * 人读报告可选统一写入 `result/report.all.txt`；**关键是**：把每个文件的“可编辑补丁”写入 `result/patch.<相对路径>.spt.txt`。
    * `findAllFiles()/findAllOriginFiles()`

        * Map 的 key 改为相对路径（见上）。
    * `validateOriginSpt(...)`

        * 先比较 `originSpt.size()` 与 `bluePrint.size()`；不等则报行数差异；
        * 对逐行比较，越界情况分别报“蓝本缺少第 N 行/译文缺少第 N 行”。
    * `getOriginalLines()/getTranslateLines()`

        * 保持功能不变；在注释上**明确**“○=原文、●=译文”的约定。

2. **新增** `src/test/java/PublicPatch.java`（或 `PublicTest#patch()`）

    * 提供 `@Test void patch()` 或 `public static void applyPatches()`：

        * 读取并解析 `resources/result/patch.*.spt.txt`；
        * 依据 `# FILE:` 和 `# ID:` + 行文本进行替换；
        * 做 `.bak` 备份；
        * 控制台打印汇总。
    * 把分隔符常量（`SPLIT_TOKEN` 等）放一个公共常量类里，供导出与解析一致使用。

3. **（可选）CheckerPipeline 小优化**

    * 新增一个方法 `List<Violation> checkLine(String translateLine, String originalLine)`，返回结构化错误（含 `id`, `isTranslate`, `rawLine`, `messages`）。
    * `sptFormatCheck` 维持原签名，以人读串兼容现有输出；`PublicTest` 在生成补丁时优先走结构化结果。

---

## 执行与验收

**命令**

* 运行检查（生成 patch 文件）：

  ```
  mvn -q -Dtest=PublicTest#checkerPipeline test
  ```
* 编辑 `src/main/resources/result/patch.*.spt.txt` 中的 `●...●`/`○...○` 行（只改行文本）。
* 执行打补丁：

  ```
  mvn -q -Dtest=PublicPatch#patch test
  ```

  或（若写成 PublicTest 的另一个测试方法）：

  ```
  mvn -q -Dtest=PublicTest#patch test
  ```

**验收标准**

1. `resources/result/` 下为**每个源文件**生成一个 `patch.*.spt.txt`；
2. patch 文件内仅包含“需要修的行”的可编辑版本，且每条上方带有 `# ID:` 与 `# ERR:` 注释；
3. 我修改 patch 文件的 `●...●` 行后，运行 `patch()` 能把对应 ID 的行**精准替换**回 `resources/spt/<原文件>`；
4. 替换前有 `.bak` 备份；
5. 重新运行 `checkerPipeline()` 时，已修正的行不再报错；
6. 兼容存在子目录与重名文件的情况（以相对路径识别）；
7. 全流程 UTF-8 编码，保留行内的 `[\r][\n]` 分行标记。

**边界与错误处理**

* patch 文件里 ID 没在目标文件找到 → 控制台列出未命中清单；
* patch 文件缺少 `# FILE:` → 按 patch 文件名反推；反推失败则跳过该文件并报错；
* 同一 ID 多次出现 → 以首次出现的为准，其余警告；
* `sptBluePrint` 缺行/多行 → 在 `validateOriginSpt` 报明确错误。

