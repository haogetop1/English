# 验证报告

验证环境：2026-10-10，Linux x86_64 云环境，JDK 21 / Gradle 8.13 / SDK 36。无 /dev/kvm，模拟器使用软件加速。

## 当前执行结果

| 项目 | 结果 |
| --- | --- |
| core 单元测试 | 35/35 通过；调度、队列、时区额度、校验、备份、统计、前台计时 |
| app 单元测试 | 35/35 通过；真实 Room 持久化、评分幂等、SQLite 故障回滚、撤销、恢复与 ViewModel，含六项审查回归 |
| Android lint | 0 错误，1 条兼容性提示：API 26 应显式声明 fullBackupContent；allowBackup=false 已禁用备份 |
| debug 与 androidTest APK 构建 | 通过 |
| APK 签名 | apksigner 验证通过，v2，Android Debug 证书 |
| 包信息 | com.xunji.flashcards.debug，0.1.0，minSdk 26 / targetSdk 36 |
| 权限 | 无 INTERNET / READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE / MANAGE_EXTERNAL_STORAGE；仅 AndroidX 自有动态接收器权限 |
| 工具链安装重复运行 | 通过，javac 21.0.12.1；依赖与缓存留在 xunji |
| API 26 FSRS 运行 | AndroidJUnitRunner 真实设备测试 1/1 通过，Java record / java.time / 四档算法依赖可运行 |
| API 26 Compose 设备测试 | 最终完整执行 7/7 通过，225.081 秒；含动画时钟答案隔离、真实旋转、可见错误反馈与数据库故障重试导航 |
| API 35 | 软件模拟器 system_server 在慢启动中反复退出，未完成启动；后续探测端口关闭。设备验证未执行 |
| 独立整体审阅 | 一次独立整体审阅已完成：4 项 Important 均修复并通过失败→通过回归；完整主机 70/70 与设备 7/7 通过，无未处理 Important |

## 设备与渲染

API 26 AOSP x86_64 模拟器，480×800 / 160 dpi；通过项目内直接 ADB 客户端执行同一 AndroidJUnitRunner，SDK adb 因只读主目录无法使用。不是以仅有端口/PID 判定成功。

首轮设备已通过：显示答案前隐藏背面和评分、评分→完成→撤销→重新评分；必填验证、保留输入、放弃修改、牌组删除取消及确认；浅色、深色、320 dp、字体 1.5/2.0、最大字段文本渲染与评分按钮可用性。已检查实际截图并修复大字体下标题信息重叠。实际 Activity 重建保留答案检查已通过。

实际截图位于 `artifacts/screenshots/`，覆盖 review-light-320、review-dark-320、review-dark-font150、review-dark-font200、review-long-text-font200。测试场景的学习内容只存在模拟器，应用首次使用不插入演示数据。

正文颜色与背景按 sRGB 对比度计算，已验证关键配对 6.00～14.63:1，超过 4.5:1 目标；图标与关键按钮区域至少 48 dp。长文本在卡片内滚动，大字体和窄屏评分改为两列。

软件模拟器出现过一次 System UI 无响应提示，选择等待后恢复；未将其认作应用通过或失败证据。

## 系统文件与进程恢复

最终 APK 再次 force-stop 后重开，实测快照 `device-db/final-after-restart.json` 通过；之前的备份验收通过 SQLite 与今日页面确认保留 REVIEW、revision 3、原 dueAt、两条评分记录（其中一条撤销）与已学习数量。实际 Android DocumentsUI 导出 JSON，修改每日额度 20→0；恢复预览取消后数据库仍为 0，重新选择并确认替换后恢复为 20/SYSTEM。恢复前后逐项比较牌组、卡片 ID、调度和完整评分快照，一致。证据见 `artifacts/test-results/manual-saf-backup.json` 与 `device-db/{before-saf,after-cancel,after-restore}.json`。

## 尚未验证

- API 35 上的运行与交互。
- 物理真机、TalkBack 人工朗读、OEM 文件提供器行为。
- 配置发布后的新任务恢复。安装和启动脚本草稿已保存，尚未发布。

如未补足这些项目，应按 [真机步骤](device-checklist.md) 检查，不将 JVM/构建成功替代它们。

## 产物与原始证据

`bash scripts/verify.sh` 同时保存实际 JUnit XML、lint HTML/XML、签名、包信息与权限输出；路径 `artifacts/test-results/`。APK：`artifacts/xunji-0.1.0-debug.apk`，摘要文件 `artifacts/xunji-0.1.0-debug.apk.sha256`。生成物未提交 Git。

独立审查与决定说明见 [修复记录](review-and-decisions.md)。

核心与数据库测试均先在缺失实现上观察断言失败，再实现并通过；数据库回滚使用测试 SQLite trigger，没有生产故障后门。被撤销记录、非法备份关系、超限输入和读写异常均有独立验证。

