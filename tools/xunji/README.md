# 寻记 · Xunji

离线 Android 闪卡应用。自行创建文本卡片，通过 FSRS 安排复习，支持本地备份。

当前处于实现阶段。正式范围见 [设计](docs/superpowers/specs/2026-10-09-xunji-flashcards-design.md)，开发步骤见 [计划](docs/superpowers/plans/2026-10-09-xunji-flashcards.md)。

## 构建环境

在本目录运行：

```bash
bash scripts/setup-toolchain.sh
source scripts/env.sh
cd android
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

需要 Linux x86_64、Python 3.12+、网络访问和约 8 GB 可用磁盘空间（包含模拟器时更多）。脚本保留 TLS 与归档校验，工具链、缓存和构建输出留在本项目内。它使用固定的 Temurin JDK、Gradle 与 Android SDK；首次运行会通过 Android SDK 标准接口接受开发工具许可。

每个新 shell 先 `source scripts/env.sh`。不需要创建新 worktree；继续使用当前云任务的隔离工作区。

Android 设备测试需要启动模拟器或连接设备后运行 `./gradlew :app:connectedDebugAndroidTest`。设备测试结果与 JVM 测试分开报告。

## 项目结构

- `android/core`：独立业务模型、FSRS 适配和纯逻辑。
- `android/app`：Room 数据、事务操作和 Compose 界面。
- `scripts`：项目内工具链、构建和验证命令。
- `artifacts`：生成的 APK、测试报告和截图（不提交到 Git）。
