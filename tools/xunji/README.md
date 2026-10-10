# 寻记 · Xunji

一款离线 Android 闪卡应用：自己制卡，用 FSRS 安排复习，将进度保存在手机本地。中文原生 Compose 界面，暖白与森林绿配色，支持深色主题。

## 安装与第一次学习

最低 Android 8.0 / API 26。开发验证包在 `artifacts/xunji-0.1.0-debug.apk`；它使用独立包名 `com.xunji.flashcards.debug` 和 debug 签名，未发布到应用商店。

允许手机安装该来源的 APK 后打开寻记：

1. 在「牌组」新建一个牌组，例如「雅思词汇」。
2. 点新增卡片，正面填词汇或问题，背面填释义或答案，例句可选。
3. 开始复习，先回忆，再显示答案，根据实际感受评分。

| 评分 | 含义 |
| --- | --- |
| 忘记 | 没能回忆 |
| 困难 | 能回忆，但很费力 |
| 记得 | 正常回忆 |
| 轻松 | 很容易回忆 |

按钮下方显示预计间隔；评分时按真实时间重新计算。学习步骤未到期时不会立刻重复。可以撤销当前会话最后一次评分；撤销后入口关闭，重新评分后再次开放。离开复习页面结束会话。

默认每日新卡上限 20 张，所有牌组合计，设置范围 0～100。上限 0 不阻止到期复习；重复评分同一新卡不重复扣额度。偶尔漏学会保留原到期时间，可以在缓冲日补学。

「记录」分别显示评分次数、学习卡片数、忘记比例和有效用时。只计算前台用时，单次卡片最多 5 分钟；被撤销的评分不计。

## 本地备份

「设置 → 导出备份」使用系统文件选择器保存 JSON，包含牌组、卡片、调度状态、全部记录和设置。请自行保管重要备份。

恢复前先读取并校验文件，再显示数量与替换确认。**确认后替换当前全部数据，不合并，无法撤回。** 建议先导出当前数据。读取或写入失败不会清空已有数据；文件上限 50 MiB。

第一版独立使用，只支持寻记自己的格式，暂不支持 Anki `.apkg` 或 AnkiWeb。应用无联网或广泛存储权限。

## 构建与验证

在本目录执行：

```bash
bash scripts/setup-toolchain.sh
bash scripts/verify.sh
```

脚本固定安装 Temurin JDK 21.0.12.1+1、Gradle 8.13、Android SDK 36 / build-tools 36.0.0，执行 core/app 测试、lint、APK 构建和包信息检查，并复制 APK、SHA-256、报告到 `artifacts/`。

需要 Linux x86_64、Python 3.12+、pip、网络访问和约 8 GB 可用磁盘（模拟器需更多）。下载保留 TLS 与归档校验，JDK、Gradle 和 SDK 留在本项目。首次安装通过 SDK 标准接口接受开发工具许可。不要重设 HOME。

每个新 shell 激活工具后可直接开发：

```bash
source scripts/env.sh
cd android
./gradlew :core:test :app:testDebugUnitTest
```

工具与依赖缓存可保留；模拟器进程需要在新任务重新启动。没有 KVM 时软件模拟器会很慢。启动及真机步骤见 [设备验收](docs/testing/device-checklist.md)，实际通过与未执行项目见 [验证报告](docs/testing/verification-report.md)。不需要额外 worktree。

## 源码与文档

- `android/core`：模型、FSRS 适配、队列、统计、备份校验。
- `android/app`：Room、事务、ViewModel、Compose 与 SAF 文件接口。
- `scripts`：工具链与构建验证。
- `artifacts`：生成的 APK、报告和截图，忽略 Git。
- [正式设计](docs/superpowers/specs/2026-10-09-xunji-flashcards-design.md) / [实施计划](docs/superpowers/plans/2026-10-09-xunji-flashcards.md) / [第三方声明](THIRD_PARTY_NOTICES.md)。

所有实现留在 `tools/xunji`；没有推送或发布。空数据库不插入示例，测试在独立测试环境执行。
