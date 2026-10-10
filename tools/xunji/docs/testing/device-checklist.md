# 真机验收

APK：`artifacts/xunji-0.1.0-debug.apk`。最低 Android 8 / API 26；debug 签名。建议先在测试设备安装，备份已有寻记数据。

1. 安装并打开，首次无演示数据；「牌组」创建「雅思词汇」。
2. 新建卡：正面 resilient，背面「有韧性的」，例句 Be resilient.；必填为空应保留输入并提示。
3. 进入复习，答案面和评分按钮应在「显示答案」之后出现；点「轻松」，完成页显示一次评分。
4. 点撤销，应回到正面并返还新卡额度；再次评分，然后退出。
5. 强制停止应用后重开：卡片、下一次到期时间及有效评分记录仍存在；不得重置为新卡。
6. 设置新卡上限 0：已到期复习仍可学习；切换浅色、深色和跟随系统。
7. 导出备份：通过系统文件选择器保存 `.xunji.json`，确认成功提示且文件可读取。
8. 修改卡片内容、主题和上限，再选择刚才的备份。先取消：当前数据不应改变。再次恢复并确认替换：内容、dueAt、记录和设置应恢复。
9. 选择错误文件/未来版本/超过 50 MiB 文件：显示失败，现有数据保留。取消文件选择：无失败提示、无数据变动。
10. 复习中切到后台或锁屏再回来：暂停计时、刷新间隔；旋转应保留当前卡片与答案。退出未评分页面不添加记录。
11. 检查长文本可滚动，字体 1.5 和 2 倍、320 dp 宽度下评分按钮全部可点；检查软键盘下保存/返回；TalkBack 能读出显示答案、四档评分和图标用途。
12. 删除卡片/牌组需确认，并明确同时删除学习记录；取消不删除。

自动设备测试：

```bash
source scripts/env.sh
bash scripts/run-emulator.sh 26 5556
# 等 sys.boot_completed 输出 1 后：
python3 scripts/device.py --port 5557 install android/app/build/outputs/apk/debug/app-debug.apk
python3 scripts/device.py --port 5557 install android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
python3 scripts/device.py --port 5557 instrument
```

有标准 adb / 真机的环境可以运行 `cd android && ./gradlew :app:connectedDebugAndroidTest`。云环境的 SDK adb 写入只读主目录，因此使用项目内直接 ADB 客户端；它仅连接本机已授权模拟器。软件模拟器安装可能慢于正常真机。
