# Xunji Android Flashcards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付离线 Android 闪卡应用“循记”，支持制卡、FSRS 复习、统计和完整本地备份。

**Architecture:** `core` 是不依赖 Android 的 Kotlin/JVM 模块，负责模型、调度、队列、输入与备份验证。`app` 使用 Room、事务业务操作和 Compose，通过 ViewModel 展示可观察状态；调度器不直接写数据库。

**Tech Stack:** Kotlin 2.2.20、Compose、Material 3、Room 2.8.1、Coroutines、java-fsrs 1.0.0、Gradle 8.13、AGP 8.13.2。

**Spec:** [已确认的正式设计](../specs/2026-10-09-xunji-flashcards-design.md)。实现前同时阅读设计与本文。日期：2026-10-09；用户已确认设计与计划，选择当前代理直接执行。当前结果见 [验证报告](../../testing/verification-report.md)，API 35 与真机项目未验证。

## Global Constraints

- 工作目录 `/workspace/English/tools/xunji`；所有新增源码、下载、缓存、构建输出与文档留在该目录；不新建 worktree、不推送、不上架。
- Android 最低 API 26；compileSdk=36、targetSdk=36；源码目标 Java 17；应用名称“循记”。
- 正式 applicationId/namespace=`com.xunji.flashcards`；debug 后缀 `.debug`；versionCode=1、versionName=`0.1.0`。
- 不申请 INTERNET、MANAGE_EXTERNAL_STORAGE 或共享存储读写权限；文件选择使用 SAF。
- 本地 Room schemaVersion=1；禁止破坏性数据库迁移；业务 ID 为 UUID 字符串，时间为 UTC epoch milliseconds。
- 固定 FSRS `io.github.open-spaced-repetition:fsrs:1.0.0`；保留 MIT 声明；默认参数、retention=0.9、学习 1/10 分钟、重新学习 10 分钟、关闭 fuzzing。
- 每日新卡上限默认 20，范围 0～100，全牌组合计；学习中/重新学习优先，然后到期复习，再新卡；不提前复习。
- 牌组名称去首尾空白后 1～80 字符；正面 1～2,000、背面 1～10,000、例句 0～4,000 字符；字符数按 Unicode code point 计，必填文本不得为空白。
- 评分与日志同事务；操作 ID 幂等；revision 冲突不覆盖；只撤销当前会话最近一次评分，撤销后不能继续撤销更早记录。
- 备份 `format=xunji-backup`、schemaVersion=1；全量替换需用户确认；输入最多 50 MiB；失败保留原数据。
- 时间戳范围 0～253402300799999；用时 0～300,000 ms；非 NEW 记忆参数 stability>=0.001、difficulty 在 1～10。
- 暖白/墨绿/琥珀主题和深色模式，中文默认，大字体及长文本可用，触控区>=48 dp，正文对比度>=4.5:1。
- 不默认插入示例牌组；不加入 Anki 导入、同步、AI、媒体、账户或连续打卡要求。
- 实现业务行为先写有意义的失败测试，运行确认失败后实施，再运行相关测试。构建与文档配置不写镜像式测试。

## Review Focus

1. Emoji、组合字符和多行文本：输入长度不能按字节错误拒绝；前后面换行和内容应保存，牌组名才自动 trim。归属 Task 2/3。
2. 设备时钟倒退或跨夏令时：算法不能产生负复习用时或异常状态；日额度使用时区日界线而非固定 24 小时。归属 Task 2/6。
3. 导入看似合法但快照关联错误的备份：日志里的 cardId、deckId、前后快照需一致，拒绝并保留数据库。归属 Task 5。
4. SAF 取消或流中途读写失败：不触发替换、不声称导出成功，用户仍能继续使用原数据。归属 Task 5/7。
5. 恢复或删除发生在答案已显示期间：终止旧会话或刷新状态，不用旧 revision 再次评分。归属 Task 4/6/7。

## 固定版本与可信获取

| 项目 | 固定版本 / 校验 |
| --- | --- |
| JDK | Eclipse Temurin `21.0.12.1+1`，Linux x64，编译目标 17 |
| JDK 压缩包 | `OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz`；SHA-256 `ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94` |
| Gradle | 8.13；distributionSha256Sum=`20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78` |
| Android command-line tools | 19.0，归档 `commandlinetools-linux-13114758_latest.zip`；官方 repository2-3.xml 的 SHA-1 `5fdcc763663eefb86a5b8879697aa6088b041e70` |
| SDK | platforms;android-36、build-tools;36.0.0；用 sdkmanager 保留官方完整性校验 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin / Compose 编译插件 / serialization 编译插件 | 2.2.20 |
| KSP | 2.2.20-2.0.3 |
| Compose BOM | 2025.09.01，管理 UI、Material 3 和 Compose 测试版本 |
| Activity Compose | 1.11.0 |
| Lifecycle ViewModel/Runtime Compose | 2.9.4 |
| Navigation Compose | 2.9.5 |
| Room runtime/ktx/compiler | 2.8.1 |
| Coroutines core/android/test | 1.10.2 |
| kotlinx.serialization JSON | 1.9.0 |
| coreLibraryDesugaring | desugar_jdk_libs 2.1.5 |
| JVM 测试 | JUnit 4.13.2；Robolectric 4.16（Room JVM 测试使用 API 28） |
| Android 测试 | runner 1.6.2、androidx.test.ext:junit 1.2.1，Compose 测试使用 BOM |

固定版本组合已实际安装、构建并通过 JVM 与 API 26 验证。若实测发现工具兼容问题，只调整必要的固定版本，记录原因与验证结果，不关闭 TLS/校验或改写预期校验值。

JDK 从 `github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/` 获取；Gradle 从 `services.gradle.org/distributions/` 获取；SDK 从 `dl.google.com/android/repository/` 获取；依赖使用 Google Maven 与 Maven Central。优先复用已存在且验证有效的安装，不覆盖用户工具。

## 目录与共享接口

所有路径相对当前 xunji 目录。后文 `coreSrc` 指 `android/core/src/main/kotlin/com/xunji/core`；`coreTest` 指 `android/core/src/test/kotlin/com/xunji/core`；`appSrc` 指 `android/app/src/main/java/com/xunji/flashcards`；`appTest` 指 `android/app/src/test/java/com/xunji/flashcards`；`deviceTest` 指 `android/app/src/androidTest/java/com/xunji/flashcards`。这是路径展开规则，不是新增目录名。

模型统一定义于 core：Deck、Flashcard、CardSchedule、ReviewLog、AppSettings、CardCandidate、DeckSummary、CardItem。时间字段为 Long epoch milliseconds，算法参数接受 Instant；可序列化对象不直接包含 Instant。

- `CardSchedule(cardId, phase, step, stability, difficulty, dueAt, lastReviewAt, firstReviewedAt, revision)`：字段类型依次为 String、Phase、Int?、Double?、Double?、Long、Long?、Long?、Long。
- `Deck(id:String, name:String, createdAt:Long, updatedAt:Long)`。
- `Flashcard(id:String, deckId:String, front:String, back:String, example:String, createdAt:Long, updatedAt:Long)`。
- `ReviewLog(id:String, cardId:String, deckId:String, sessionId:String, rating:Rating, reviewedAt:Long, durationMs:Long, beforeSchedule:CardSchedule, afterSchedule:CardSchedule, undoneAt:Long?)`；id 同时是评分操作 ID。
- `AppSettings(newCardsPerDay:Int=20, themeMode:ThemeMode=SYSTEM, schemaVersion:Int=1)`。
- `CardCandidate(card:Flashcard, schedule:CardSchedule, deckName:String)`；`CardItem(candidate:CardCandidate)`；`DeckSummary(deck:Deck,total:Int,due:Int,newCards:Int)`。
- Phase=`NEW/LEARNING/REVIEW/RELEARNING`；Rating=`AGAIN/HARD/GOOD/EASY`，数据库数值 1/2/3/4；ThemeMode=`SYSTEM/LIGHT/DARK`。

模型以小文件按职责定义。业务逻辑只使用这些应用模型，FSRS 类限制在适配器文件内。

### Task 1: 可重复构建与 Android FSRS 兼容检查

**Files:**
- Create: `.gitignore`、`scripts/setup-toolchain.sh`、`scripts/env.sh`、`README.md`。
- Create: `android/settings.gradle.kts`、`android/build.gradle.kts`、`android/gradle.properties`、`android/gradle/libs.versions.toml`、`android/gradlew`、`android/gradlew.bat`、`android/gradle/wrapper/gradle-wrapper.properties`、`android/gradle/wrapper/gradle-wrapper.jar`。
- Create: `android/core/build.gradle.kts`、`android/app/build.gradle.kts`、`android/app/src/main/AndroidManifest.xml`、`android/app/src/main/res/values/strings.xml`、`appSrc/MainActivity.kt`（初始最小宿主）。
- Test: `coreTest/compat/FsrsLibraryTest.kt`、`deviceTest/compat/FsrsAndroidCompatibilityTest.kt`。

**Interfaces:** Consumes 固定版本表；Produces 可运行的 Gradle 工程和项目内环境初始化；本任务不定义产品业务接口。

- [x] **Step 1:** 编写并执行 setup-toolchain.sh：检查已安装版本→下载必要归档→验证 checksum→解压至 `.toolchain/`；安装 SDK 36/build-tools 36.0.0。脚本非交互，SDK 许可通过 sdkmanager 的标准接口接受，保留其命令退出状态。env.sh 根据自身绝对位置导出 JAVA_HOME、ANDROID_HOME、ANDROID_SDK_ROOT、GRADLE_USER_HOME、ANDROID_USER_HOME、TMPDIR，均指向 xunji 内；不改 HOME 或用户全局配置。
- [x] **Step 2:** 用经校验的 Gradle 生成 Wrapper，填写固定 checksum；建立 core/app 模块与依赖目录，Gradle workers 最大 4、堆最大 4 GiB；创建无联网权限的最小 Activity，配置 debug 签名文件位于项目缓存内。验证 `javac -version`、`sdkmanager --list_installed`、`./gradlew --version`。
- [x] **Step 3:** 为 raw java-fsrs 写测试 `newCardAgainUsesOneMinute` 和 `newCardGoodUsesTenMinutes`：关闭 fuzzing，固定 Instant，调用 reviewCard，断言 due 分别等于 now+60 秒和 now+600 秒、产生对应 rating 日志且原 Card 未被修改。添加设备版本相同断言，实际运行才能证明 record/java.time 在 Android 上可用。

```kotlin
assertEquals(at.plusSeconds(60), again.card().due)
assertEquals(at.plusSeconds(600), good.card().due)
assertEquals(Rating.GOOD, good.reviewLog().rating())
```

- [x] **Step 4:** Run `source scripts/env.sh` 后在 android 执行 `./gradlew :core:test :app:assembleDebug --console=plain`；期望有效测试计数>0、无失败并生成 APK。工具配置没有业务红绿循环；不能把首次成功构建描述为所有产品功能已通过。
- [ ] **Step 5:** 用 SDK 稳定 channel 的 emulator 和 API 35 AOSP x86_64 system image，在 `.toolchain/` 内建 AVD。没有 KVM 时尝试 `-no-accel -gpu swiftshader_indirect -no-window -no-audio -no-snapshot`；等待启动最多 10 分钟，按最长 30 秒输出检查循环、期间保持进度更新。启动后运行 `./gradlew :app:connectedDebugAndroidTest --console=plain`，记录具体版本和结果；失败需诊断，设备未运行必须标记 unrun，不用 JVM 结果替代。
- [x] **Step 6:** 更新 README 工具初始化与验证步骤，提交本任务的工程、脚本及兼容测试，不提交工具链、缓存、私有签名或 APK。

### Task 2: 领域模型、FSRS 适配、队列和输入规则

**Files:**
- Create: `coreSrc/model/Models.kt`、`coreSrc/model/ReviewLog.kt`、`coreSrc/scheduling/Scheduler.kt`、`coreSrc/scheduling/FsrsScheduler.kt`。
- Create: `coreSrc/study/QueuePlanner.kt`、`coreSrc/study/NewCardQuota.kt`、`coreSrc/time/LocalDay.kt`、`coreSrc/validation/InputValidator.kt`。
- Test: `coreTest/scheduling/FsrsSchedulerTest.kt`、`coreTest/study/QueuePlannerTest.kt`、`coreTest/study/NewCardQuotaTest.kt`、`coreTest/validation/InputValidatorTest.kt`。

**Interfaces:**
- Consumes Task 1 library and共享模型约定。
- Produces `Scheduler.review(schedule:CardSchedule,rating:Rating,at:Instant):CardSchedule`、`preview(schedule:CardSchedule,at:Instant):Map<Rating,CardSchedule>`；FsrsScheduler 实现。
- Produces `QueuePlanner.next(candidates:List<CardCandidate>,remainingNew:Int,at:Instant):QueueDecision`，结果 Ready(candidate)/Waiting(nextDueAt:Long)/Done。
- Produces `NewCardQuota.remaining(allSchedules:List<CardSchedule>,settings:AppSettings,at:Instant,zone:ZoneId):Int` 和 `LocalDay.bounds(at:Instant,zone:ZoneId):DayBounds(start:Long,endExclusive:Long)`。
- Produces `InputValidator.deckName(raw:String):String`、`cardContent(front:String,back:String,example:String):Unit`；失败抛 FieldValidationException(field:String,message:String)。

- [x] **Step 1:** 写失败测试：NEW/AGAIN due=at+60,000；NEW/GOOD due=at+600,000；REVIEW/AGAIN→RELEARNING；preview 四项等于同时间独立 review 结果且输入未变化。review 更新 firstReviewedAt、lastReviewAt 和 revision+1；早于 lastReviewAt 的评分拒绝，不制造负间隔。

```kotlin
assertEquals(at.toEpochMilli() + 600_000, good.dueAt)
assertEquals(at.toEpochMilli(), good.firstReviewedAt)
assertEquals(original.revision + 1, good.revision)
assertEquals(scheduler.review(original, Rating.GOOD, at), preview.getValue(Rating.GOOD))
```

- [x] **Step 2:** 补队列和额度测试：到期学习优先于 REVIEW，NEW 不计到期；额度=0 仍返回到期卡；无可学但有未来学习卡返回 Waiting；全牌组计数；Asia/Shanghai 午夜额度重置，America/New_York 夏令时日界线不按固定 24 小时计算。
- [x] **Step 3:** 补输入测试：空白字段拒绝；牌组名 trim；正面/背面内容保留换行；80/2,000/10,000/4,000 code point 边界通过，超出 1 拒绝；含 emoji 的正面长度按 code point 计算。
- [x] **Step 4:** Run `./gradlew :core:test --console=plain`，确认因缺失接口或行为而失败；实现上述精确接口，显式映射库 LEARNING/REVIEW/RELEARNING 与应用 NEW，不使用库默认 cardId 当 UUID；映射临时库 cardId=0，忽略库日志业务 ID。
- [x] **Step 5:** 再运行相同测试，期望所有上述测试执行且通过；记录计数，提交模型和核心逻辑。

### Task 3: Room 持久化和牌组/卡片 CRUD

**Files:**
- Create: `appSrc/data/Entities.kt`、`appSrc/data/XunjiDao.kt`、`appSrc/data/XunjiDatabase.kt`、`appSrc/data/EntityMappers.kt`、`appSrc/data/StudyRepository.kt`、`appSrc/data/RoomStudyRepository.kt`。
- Create: `coreSrc/serialization/ScheduleCodec.kt`；`android/app/schemas/com.xunji.flashcards.data.XunjiDatabase/1.json`（Room 生成）。
- Test: `appTest/data/RoomStudyRepositoryTest.kt`、`coreTest/serialization/ScheduleCodecTest.kt`。

**Interfaces:**
- Consumes Task 2 模型与 InputValidator。
- Produces `StudyRepository.observeDecks():Flow<List<Deck>>`、`observeCards(deckId:String):Flow<List<CardCandidate>>`、`observeSchedules():Flow<List<CardSchedule>>`、`observeLogs():Flow<List<ReviewLog>>`、`observeSettings():Flow<AppSettings>`。
- Produces suspend `createDeck(name:String):Deck`、`renameDeck(id:String,name:String):Unit`、`deleteDeck(id:String):Unit`、`saveCard(id:String?,deckId:String,front:String,back:String,example:String):Flashcard`、`deleteCard(id:String):Unit`、`candidates(deckId:String?):List<CardCandidate>`、`saveSettings(settings:AppSettings):Unit`。
- Produces `ScheduleCodec.encode(schedule:CardSchedule):String`、`decode(json:String):CardSchedule`，使用应用 schema，非第三方库 JSON。

- [x] **Step 1:** 写真实 Room Robolectric 测试：创建卡后 NEW 状态与内容一起保存；编辑保留 schedule/review logs；删除牌组级联删除 cards/schedules/logs；同名牌组允许；字段长度拒绝不写入；保存英文、emoji、多行文字后读取完全相等。

```kotlin
assertEquals(originalSchedule, repository.candidates(deck.id).single().schedule)
assertEquals("line one\nline two 😀", repository.candidates(deck.id).single().card.front)
assertTrue(repository.candidates(deletedDeck.id).isEmpty())
```

- [x] **Step 2:** 添加文件型数据库 close/reopen 持久化测试；默认 settings=20/SYSTEM/1；saveSettings(0) 和主题可持久化；ScheduleCodec 往返保持所有可空与非空字段。Run `./gradlew :app:testDebugUnitTest :core:test --console=plain`，确认缺失实现导致失败。
- [x] **Step 3:** 实现 Room entities/DAO、外键和索引、默认单行设置初始化、数据映射与仓库；新增 Card 与 CardSchedule 同事务；时间源为注入 Clock，ID 源为注入 UUID factory；导出 schema，不启用 destructive migration。
- [x] **Step 4:** 再运行上述测试，确认真实数据库约束、级联和重启测试通过；保持空数据库无示例数据。提交数据层和生成 schema。

### Task 4: 原子评分、幂等、撤销和冲突处理

**Files:**
- Create: `appSrc/study/ReviewService.kt`、`appSrc/study/ReviewCommands.kt`；Modify `appSrc/data/XunjiDao.kt`。
- Test: `appTest/study/ReviewServiceTest.kt`。

**Interfaces:**
- Consumes Room、Scheduler、InputValidator、NewCardQuota，Clock 注入。
- Produces `GradeCommand(operationId:String,sessionId:String,cardId:String,expectedRevision:Long,rating:Rating,durationMs:Long)`。
- Produces suspend `ReviewService.grade(command:GradeCommand):GradeResult`，Recorded(log)/Duplicate(log)/MissingCard/StaleSchedule/QuotaReached/InvalidCommand；`undo(sessionId:String,logId:String):UndoResult`，Restored(schedule)/Unavailable。

- [x] **Step 1:** 写测试：首次评分 firstReviewedAt 更新；同 operationId 重试只有一条日志与一次 revision 更新；重复 ID 但 command 内容不同必须拒绝，不能返回另一张卡的日志；过期 revision、已删除卡、额度已经耗尽均无写入。断言 repeated rating 不重复消耗新卡额度。
- [x] **Step 2:** 写原子性测试：在测试 SQLite 中用 trigger 拒绝 log insert，断言异常后 schedule 与 firstReviewedAt 未改变。撤销恢复完整 before 状态但 revision 单调递增、首次额度返还、undoneAt 非空；错误 session、已被修改状态和二次撤销均拒绝。

```kotlin
assertEquals(1, repository.observeLogs().first().size) // 同操作重试后
assertEquals(before, repository.candidates(deck.id).single().schedule) // log insert 失败后
assertNull(restored.firstReviewedAt) // 撤销首次评分后
assertTrue(restored.revision > after.revision)
```

- [x] **Step 3:** Run `./gradlew :app:testDebugUnitTest --tests '*ReviewServiceTest' --console=plain` 验证失败；实现 Room withTransaction。先查 operationId 验证幂等，再检查卡片/revision/额度，使用实际点击 Clock 时间评分；撤销检查最新有效记录和 after revision。
- [x] **Step 4:** 再运行同测试及数据层回归；所有有效测试通过后提交。不要添加生产环境故障注入后门，SQLite trigger 仅存在于测试数据库。

### Task 5: 备份格式、验证和原子恢复

**Files:**
- Create: `coreSrc/backup/BackupDocument.kt`、`coreSrc/backup/BackupCodec.kt`、`coreSrc/backup/BackupValidator.kt`。
- Create: `appSrc/backup/BackupService.kt`、`appSrc/backup/BackupFileGateway.kt`；Modify `appSrc/data/XunjiDao.kt`。
- Test: `coreTest/backup/BackupValidatorTest.kt`、`appTest/backup/BackupServiceTest.kt`。

**Interfaces:**
- Produces `BackupDocument(format:String,schemaVersion:Int,exportedAt:Long,decks:List<Deck>,cards:List<Flashcard>,schedules:List<CardSchedule>,reviewLogs:List<ReviewLog>,settings:AppSettings)`。
- Produces `BackupCodec.encode(document:BackupDocument):String`、`decode(json:String):BackupDocument`；`BackupValidator.validate(document:BackupDocument):Unit`。
- Produces suspend `BackupService.export():BackupDocument`、`prepare(input:InputStream):PreparedBackup`、`replace(prepared:PreparedBackup):Unit`；PreparedBackup 含已验证 document 及 count summary。`BackupFileGateway.write(uri:Uri,document:BackupDocument):Unit` 与 `read(uri:Uri):InputStream` 封装 ContentResolver。

- [x] **Step 1:** 写失败测试：导出/恢复保留 IDs、dueAt、firstReviewedAt、revision、有效/撤销日志和 settings；未来 schema、重复 ID、失效外键、NaN/Infinity、越界时间/用时/参数、NEW 状态不一致、过长字段拒绝。
- [x] **Step 2:** 覆盖 Review Focus：log.cardId/deckId 与前后快照不一致必须拒绝；非 NEW 必须有合法记忆参数；卡片必须恰好对应一个 schedule；UTF-8 输入超过 50*1024*1024 字节拒绝；中途 IOException 不调用 replace、原数据不变。测试 replace 发生异常时事务回滚。

```kotlin
assertEquals(before.cards, restoredBackup.cards)
assertEquals(before.schedules, restoredBackup.schedules)
assertEquals(before.reviewLogs, restoredBackup.reviewLogs)
assertEquals(before.settings, restoredBackup.settings)
assertEquals(before, service.export()) // 固定 Clock 下，失败恢复后原库不变
```

- [x] **Step 3:** Run `./gradlew :core:test :app:testDebugUnitTest --console=plain` 验证失败；实现固定 schema JSON（拒绝 unknown keys 与特殊浮点值），验证业务关系，流式读取有界输入，事务一致快照导出及全量替换；不得边读文件边删除数据库。
- [x] **Step 4:** 再运行相同测试，通过后提交；SAF UI 确认与取消在 Task 7 实现，BackupService 不能自行跳过用户确认。

### Task 6: 统计、会话状态与 ViewModel

**Files:**
- Create: `coreSrc/stats/StudyStatistics.kt`、`coreSrc/study/SessionTimer.kt`。
- Create: `appSrc/ui/MainViewModel.kt`、`appSrc/ui/EditorViewModel.kt`、`appSrc/ui/ReviewViewModel.kt`、`appSrc/ui/SettingsViewModel.kt`、`appSrc/ui/UiModels.kt`、`appSrc/AppContainer.kt`。
- Test: `coreTest/stats/StudyStatisticsTest.kt`、`coreTest/study/SessionTimerTest.kt`、`appTest/ui/ReviewViewModelTest.kt`、`appTest/ui/EditorViewModelTest.kt`、`appTest/ui/SettingsViewModelTest.kt`。

**Interfaces:**
- Produces `StudyStatistics.summarize(logs:List<ReviewLog>,at:Instant,zone:ZoneId):StatisticsSummary`；`StatisticsSummary(today:DaySummary,lastSevenDays:List<DaySummary>)`；`DaySummary(date:LocalDate,scoreCount:Int,distinctCards:Int,againRatio:Double?,durationMs:Long)`，零记录 ratio=null；排除 undone 日志。
- Produces `SessionTimer.resume(monotonicMs:Long)`、`pause(monotonicMs:Long)`、`reset()`、`elapsedMs(monotonicMs:Long):Long`；计算用时使用单调时钟，提交 duration clamp 0～300,000。
- Produces `MainViewModel.state:StateFlow<MainUiState>`、`refresh(at:Instant,zone:ZoneId):Unit`；`EditorViewModel.save():Unit`、`state:StateFlow<EditorUiState>`。
- Produces `ReviewViewModel.state:StateFlow<ReviewUiState>`、`start(deckId:String?):Unit`、`reveal():Unit`、`grade(rating:Rating):Unit`、`undo():Unit`、`setForeground(active:Boolean):Unit`、`finish():Unit`。ReviewUiState=Loading/Question/Answer/Submitting/Complete/Failure，保存快照与 sessionId。
- Produces `SettingsViewModel.state:StateFlow<SettingsUiState>`、`prepareRestore(uri:Uri):Unit`、`confirmRestore():Unit`、`cancelRestore():Unit`、`export(uri:Uri):Unit`；恢复成功事件通知当前 ReviewViewModel.finish()。

- [x] **Step 1:** 写统计/用时测试：0 记录无 NaN；四次评分两张卡 scoreCount=4/distinct=2；AGAIN 1 次 ratio=0.25；撤销记录不计；7 日含今天；前台 2 秒、后台 10 秒、前台 3 秒，elapsed=5,000 ms；墙钟倒退不影响单调用时。

```kotlin
assertEquals(4, summary.today.scoreCount)
assertEquals(2, summary.today.distinctCards)
assertEquals(0.25, summary.today.againRatio!!, 0.0)
assertEquals(5_000L, timer.elapsedMs(15_000L))
```

- [x] **Step 2:** 写 ViewModel 测试：未 reveal 禁止 grade；重复 grade 无重复提交；rotation/SavedStateHandle 保留当前问题/答案；grade 错误保留答案可重试；stale/missing 刷新队列；undo 后显示正面，第二次 undo unavailable；finish 不写未评分记录。
- [x] **Step 3:** 补 editor 必填与重复保存测试；设置保存上限/主题；取消恢复无写入，确认恢复成功终止旧会话，旧答案无法再次提交；onResume 刷新时区、额度与预览。Run core/app tests 验证失败。
- [x] **Step 4:** 实现手动 AppContainer DI，ViewModel 通过 repository/service 而非 UI 直接 DAO；StateFlow 驱动展示。统计使用本地日区间、保存后刷新、前台每分钟刷新到期数；后台停止刷新，不建后台常驻服务。
- [x] **Step 5:** 再运行 `./gradlew :core:test :app:testDebugUnitTest --console=plain` 并确认上述测试执行；提交统计与状态层。

### Task 7: 精美 Compose 界面与完整交互

**Files:**
- Create: `appSrc/ui/theme/Color.kt`、`Type.kt`、`Theme.kt`（均在 theme/）；`appSrc/ui/components/XunjiComponents.kt`。
- Create: `appSrc/ui/XunjiApp.kt`、`appSrc/ui/screens/TodayScreen.kt`、`DecksScreen.kt`、`DeckDetailScreen.kt`、`CardEditorScreen.kt`、`ReviewScreen.kt`、`StatisticsScreen.kt`、`SettingsScreen.kt`（后六文件也位于 screens/）。
- Modify: `appSrc/MainActivity.kt`、`android/app/src/main/res/values/strings.xml`；Create 本地矢量图标/启动图资源及屏幕文案。
- Test: `deviceTest/ui/LearningFlowTest.kt`、`EditorAndDeletionTest.kt`、`BackupAndSettingsTest.kt`、`AccessibilityLayoutTest.kt`。

**Interfaces:** Consumes Task 6 StateFlow/events；Produces 带可访问标签与稳定 testTag 的 Compose screens；navigation route=Today/Decks/DeckDetail(deckId)/Editor(deckId,cardId?)/Review(deckId?)/Statistics/Settings。复习关键 tags=`card-front/card-back/reveal-answer/rating-again/rating-hard/rating-good/rating-easy/undo-rating`。

- [x] **Step 1:** 写 Compose 行为测试：初始空页面→创建牌组→制卡→进入复习；正面阶段无答案/评分，reveal 后有四档标签与间隔；grade 后完成，退出/再入后 due 不被重置。设备执行条件成立时运行测试确认缺失 UI 失败；没有设备则保留测试并标记 unrun，不伪造红绿记录。

```kotlin
compose.onNodeWithTag("card-back").assertDoesNotExist()
compose.onNodeWithTag("rating-good").assertDoesNotExist()
compose.onNodeWithTag("reveal-answer").performClick()
compose.onNodeWithTag("card-back").assertIsDisplayed()
compose.onNodeWithTag("rating-good").assertIsEnabled()
```

- [x] **Step 2:** 补未保存内容退出确认、字段错误保留输入、删除确认与取消、已显示答案后删除/恢复刷新测试；SAF 返回 null 为取消，无错误也无替换；恢复数量预览和二次确认；导出失败不显示成功提示。实现时使用可注入 FileGateway 做 UI 失败情境测试，真实文件 API 另行设备验证。
- [x] **Step 3:** 实现固定颜色/字体/间距 token、Material 3 主题、底栏和设置入口；今日、牌组、详情、制卡、复习、记录、设置完整流程。背面例句只在答案面；评分提交时禁用按钮；复习完成保留最近评分撤销入口。
- [x] **Step 4:** 实现中文反馈、空/加载/错误/完成状态；theme SYSTEM/LIGHT/DARK；SAF CreateDocument/OpenDocument 与替换确认；不申请联网或存储权限。删除牌组提示“将删除全部卡片和学习记录”，错误反馈不显示内部堆栈。
- [ ] **Step 5:** 用实际渲染验证浅色/深色、最大字段长文本、字体缩放 1.5 和 2.0、320 dp 宽度、软键盘避让；检查正文对比度>=4.5:1、点击区>=48 dp、TalkBack 标签；评分按钮宽度不足改两列，动画 180～240 ms 并尊重系统动画设置。
- [x] **Step 6:** Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain`（connected 仅设备已就绪时）；截图存 `artifacts/screenshots/`，与正式设计逐项对照修正；提交 UI 和 tests，不提交截图缓存或 APK。

### Task 8: 最终验证、APK 交付和使用说明

**Files:**
- Create: `scripts/verify.sh`、`scripts/run-emulator.sh`、`docs/testing/device-checklist.md`、`docs/testing/verification-report.md`、`THIRD_PARTY_NOTICES.md`。
- Modify: `README.md`；Generate ignored `artifacts/xunji-0.1.0-debug.apk`、`artifacts/test-results/`、`artifacts/screenshots/`。

**Interfaces:** Consumes 完整项目；Produces 可重复的构建验证命令、实际 APK 与透明的执行结果；不新增产品功能。

- [x] **Step 1:** 完善 README：工具激活、首次安装、新卡上限、四档评分、撤销范围、漏学后复习、备份/全量替换、重新构建；列出 MIT FSRS、AndroidX 等实际依赖许可，保留适用声明。
- [x] **Step 2:** scripts/verify.sh 激活项目环境后执行 `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`；保存当前运行报告并保持底层退出码，检查测试数量，区分失败/跳过/未运行。一次通过后仅因新变更或未解决问题重复测试。
- [x] **Step 3:** 在可用设备上执行 connected tests；安装 APK→创建内容→评分→force-stop→重启核对持久化→导出→修改→恢复→核对 due/历史/settings；保存设备型号/API 与当前测试时间。除 API 35 主验证外，在 API 26 系统镜像可用时重复 FSRS 兼容与学习流程测试；任一目标未执行分别记录。软件模拟器始终失败时记录已尝试诊断与未运行项，提供真机检查步骤，不宣称设备就绪。
- [x] **Step 4:** 使用 build-tools 36.0.0 的 apksigner verify 和 aapt dump badging/permissions 检查 debug APK 的包名、API 26、签名及权限；复制实际构建产物至 artifacts/xunji-0.1.0-debug.apk，生成 SHA-256，明确是 debug 签名，不是商店发布版本。
- [x] **Step 5:** 完成代码审阅与问题修正，必要时只重跑受影响检查；写 verification-report 记录实际通过、失败和未执行项，尤其 Java-FSRS Android 运行结果及 UI 截图。检查 git diff、只提交 xunji 范围源码与文档，保持既有 tools/docs/research 文件不变。

## 执行与审阅建议

推荐 Native：由当前代理在此会话按任务实施，阶段提交；完成后一次独立整体代码审阅。原因是八个任务共享调度模型、Room 事务及 ViewModel 接口，连续执行便于保持一致，也能减少每个任务重建上下文的开销。

用户也可以选择 Subagent-driven：逐任务派发实现与审阅，再做整体审阅；并行仅适用于已明确接口且不共享修改的工作。当前未派发任何子代理。

正式设计和本文实施计划已经由用户确认，用户选择 Native，由当前代理直接执行。实际构建、测试和设备检查记录见 docs/testing/verification-report.md。

## 计划自查映射

| 设计部分 | 实现任务 |
| --- | --- |
| 平台、目录和依赖 | 1、8 |
| 页面导航、制卡与主题 | 6、7 |
| 数据模型、持久化、删除 | 2、3 |
| FSRS、队列、时间与额度 | 1、2、4、6 |
| 评分、幂等、撤销、统计 | 4、6 |
| 本地备份与原子恢复 | 5、7 |
| 错误、生命周期与可访问性 | 3～7 |
| 自动化、设备、构建与交付 | 各任务测试、8 |

## 已验证的元数据来源

- https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1
- https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256
- https://dl.google.com/android/repository/repository2-3.xml
- https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/8.13.2/gradle-8.13.2.pom
- https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.09.01/compose-bom-2025.09.01.pom
- https://repo.maven.apache.org/maven2/io/github/open-spaced-repetition/fsrs/1.0.0/fsrs-1.0.0.pom

## 最终执行状态（2026-10-10）

Tasks 1～8 已完成实现、必要诊断和交付验证：core 35/app 35、API 26 设备 7 项全部通过。Task 1 Step 5 的 API 35 运行、Task 7 Step 5 的 TalkBack 人工验证仍未完成，因此保留未勾选；真实字体/窄屏/长文本/软键盘/标签与对比度已验证。未执行项不作通过声明，详情见验证报告。
