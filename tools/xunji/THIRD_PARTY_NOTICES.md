# Third-party notices

循记使用以下开源组件，没有复制 AnkiDroid 的应用源码或界面资源。应用本身的许可尚未指定；这些声明不替代各依赖的许可。

| 组件 | 固定版本 | 许可 |
| --- | --- | --- |
| open-spaced-repetition/java-fsrs | 1.0.0 | MIT |
| AndroidX / Jetpack Compose / Material 3 | Compose BOM 2025.09.01 | Apache 2.0 |
| Room | 2.8.1 | Apache 2.0 |
| Activity / Lifecycle / Navigation | 1.11.0 / 2.9.4 / 2.9.5 | Apache 2.0 |
| Kotlin / kotlinx.coroutines / kotlinx.serialization | 2.2.20 / 1.10.2 / 1.9.0 | Apache 2.0 |
| Jackson（FSRS 的传递依赖） | 2.19.2（BOM 统一后的实际运行版本） | Apache 2.0 |
| desugar_jdk_libs（java.time 支持） | 2.1.5 | GPLv2 with Classpath Exception；部分源文件 Apache 2.0 |
| Gradle / Android Gradle Plugin | 8.13 / 8.13.2 | Apache 2.0 |
| Eclipse Temurin JDK（仅构建工具） | 21.0.12.1+1 | GPLv2 with Classpath Exception |
| JUnit / Robolectric（仅测试） | 4.13.2 / 4.16 | EPL 1.0 / MIT |
| adb-shell（仅开发设备脚本） | 0.4.4 | Apache 2.0 |

Java-FSRS 原始 MIT 声明、Jackson 随包 Apache 2.0 完整许可及 NOTICE、desugar_jdk_libs GPLv2/Classpath 许可作为 assets/licenses 打包进 APK，并保留在源码中。Java-FSRS 通过 Maven Central 引入，不修改其源码。Gradle/JDK/SDK 工具包保留自带许可文件。

参考：

- https://github.com/open-spaced-repetition/java-fsrs
- https://source.android.com/docs/setup/about/licenses
- https://github.com/google/desugar_jdk_libs
- https://github.com/adb-shell/adb-shell

发布前应重新核对实际发布依赖及完整许可清单；本次交付是供开发验证的 debug APK。
