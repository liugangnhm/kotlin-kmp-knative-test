# KMP + Kotlin/Native + C：跨平台演示与技术讨论存档

项目包含两个 Gradle 工程：生产库的根工程，以及独立消费工程 `consumer`。支持 Windows x64 原生程序、JVM 程序和 Android 应用。Windows 通过 cinterop 调 C；Android 通过 JNI 调用同一份 C 源码；普通 JVM 目标使用纯 Kotlin 实现。

存档日期：2026-10-15。本文整合本次会话中的实现、运行命令、验证结果和选型讨论。目标支持等级等外部信息以存档时查阅的官方资料为准，升级工具链时需要重新核对。

## 目录

- [项目范围与术语](#项目范围与术语)
- [环境](#环境)
- [一次性编译所有模块](#一次性编译所有模块)
- [同工程模块引用](#同工程模块引用)
- [同一个工程中的多个 exe 应用](#同一个工程中的多个-exe-应用)
- [Android 平台实验](#android-平台实验)
- [发布库供独立 KMP 工程使用](#发布库供独立-kmp-工程使用)
- [目录和职责](#目录和职责)
- [为什么 C 可以随库一起复用](#为什么-c-可以随库一起复用)
- [Android 必须使用 JNI 吗](#android-必须使用-jni-吗)
- [Kotlin/Native 支持哪些 target](#kotlinnative-支持哪些-target)
- [Kotlin/Native 的限制](#kotlinnative-的限制)
- [Kotlin/Native 与 Rust 对比](#kotlinnative-与-rust-对比)
- [已完成的验证与实验边界](#已完成的验证与实验边界)
- [官方资料](#官方资料)

## 项目范围与术语

最初目标是一个 Windows 可运行的 KMP + Kotlin/Native + C 混编演示，随后依次增加独立库发布、多个 exe 应用和 Android APK。

| 名称 | 含义 | 在本项目中的位置 |
| --- | --- | --- |
| KMP | Kotlin Multiplatform，共享源码与多目标构建方案 | `native-lib` 的 commonMain、平台 actual 和发布配置 |
| Kotlin/Native（K/Native） | 将 Kotlin 编译为原生机器码的后端；本次讨论中的 knative 指它 | Windows 的 `mingwX64` 目标，不是云原生项目 Knative |
| cinterop | 从 C 头文件生成 Kotlin/Native 绑定 | Windows 原生实现调用 C |
| Android/JVM | Kotlin 编译为 JVM 字节码，再由 Android 工具链转换为 DEX，在 Android Runtime 上运行 | 本项目实际采用的 Android 目标 |
| Android Native | `androidNativeArm64` 等 Kotlin/Native 目标 | 仅作原理讨论，本项目未添加 |
| JNI | JVM/Android Runtime 与原生代码的调用边界 | `android-cbridge` 负责加载并调用 C 动态库 |

共享逻辑通过 `expect/actual` 隔离平台差异。Windows 和 Android 调用同一份 C 源码，普通 JVM 端使用纯 Kotlin 实现；这三个实现都使用相同的 `Summary` 和 `summarize` API。

## 环境

- Windows x64、JDK 17。
- 构建库时需要 MinGW-w64 x64 的 `gcc`、`ar` 在 PATH 中，使用同一套工具链。
- 消费已发布库时无需 gcc/ar、C 源码或头文件，仍需要 JDK 和 Kotlin/Native 构建环境。
- 首次构建需要联网；Gradle Wrapper 会下载依赖，无需全局安装 Gradle/Kotlin。
- 固定 Kotlin 2.4.20、Gradle 9.2.0。
- Android 构建使用 AGP 9.0.1、SDK / Build Tools 36、NDK 28.2.13676358、CMake 3.22.1。配置 `ANDROID_HOME`，或在根目录的 `local.properties` 中设置 `sdk.dir=你的SDK路径`；该本机配置不会提交到 Git。

检查命令：

```powershell
java -version
gcc --version
ar --version
adb devices -l
```

`gradle.properties` 已启用 AndroidX，并为 Gradle 配置 2 GiB 最大堆和 1 GiB 最大 Metaspace，避免新增 Android 工具链后沿用默认内存设置导致频繁重启 Daemon。

## 一次性编译所有模块

在仓库根目录执行：

```powershell
# 组装根工程中所有模块，不运行测试
.\gradlew.bat assemble

# 组装所有模块，并执行常规检查和测试
.\gradlew.bat build

# 日常只生成根工程中所有 Windows 应用的 Debug exe
.\gradlew.bat linkDebugExecutableMingwX64
```

`assemble` 包含共享库、两个 Windows 应用的 Debug/Release exe、JVM 产物和 Android APK/AAR，因此需要相应的 Windows C 工具链与 Android SDK/NDK。Release 优化耗时较长，日常可选择上面的 Debug 任务。

`build` 不会自动运行需要设备的 `connectedDebugAndroidTest`。它们也不会自动发布本地 Maven 仓库，或包含独立的 `consumer` 工程。

如果还要编译独立消费工程，先发布依赖，再构建它：

```powershell
.\gradlew.bat :native-lib:publishAllPublicationsToDemoRepository :android-cbridge:publishAllPublicationsToDemoRepository
.\gradlew.bat -p consumer build
```

`-p consumer` 切换到另一个 Gradle 工程；并不是把它作为根工程的模块加入。

## 同工程模块引用

在仓库根目录执行：

```powershell
.\gradlew.bat :demo-app:runDebugExecutableMingwX64
.\gradlew.bat :demo-app:runJvm
.\gradlew.bat :native-lib:mingwX64Test :native-lib:jvmTest
```

`demo-app/build.gradle.kts` 的共享代码依赖：

```kotlin
commonMain.dependencies {
    implementation(project(":native-lib"))
}
```

原生端输出：

```text
Platform: Windows x64 / Kotlin Native
Backend: C static library (MinGW x64)
Shared input: 10, 20, 30, 40
Shared summary: count=4, total=100, average=25.0
Self-check: OK
```

直接运行 exe：

```powershell
.\demo-app\build\bin\mingwX64\debugExecutable\kmp-c-demo.exe
```

## 同一个工程中的多个 exe 应用

根工程同时包含 `demo-app` 和 `stats-app` 两个独立应用模块。它们共享 `native-lib`，各自配置入口和 exe 名称，分别输出到自己的 build 目录。

| 应用模块 | Kotlin 入口 | Windows exe | 功能 |
| --- | --- | --- | --- |
| `demo-app` | `demo.main` | `kmp-c-demo.exe` | 单组数据统计 |
| `stats-app` | `stats.main` | `stats-report.exe` | 上午、下午两组数据报表 |

一次构建两个 exe：

```powershell
.\gradlew.bat :demo-app:linkDebugExecutableMingwX64 :stats-app:linkDebugExecutableMingwX64
```

通过 Gradle 分别运行：

```powershell
.\gradlew.bat :demo-app:runDebugExecutableMingwX64 :stats-app:runDebugExecutableMingwX64
# 第二个应用也可以用 JVM 运行
.\gradlew.bat :stats-app:runJvm
```

直接启动各自的 exe：

```powershell
.\demo-app\build\bin\mingwX64\debugExecutable\kmp-c-demo.exe
.\stats-app\build\bin\mingwX64\debugExecutable\stats-report.exe
```

上述命令依次运行；如果需要两个程序同时启动，可以用独立进程：

```powershell
$demoProcess = Start-Process -FilePath .\demo-app\build\bin\mingwX64\debugExecutable\kmp-c-demo.exe -WindowStyle Hidden -PassThru
$statsProcess = Start-Process -FilePath .\stats-app\build\bin\mingwX64\debugExecutable\stats-report.exe -WindowStyle Hidden -PassThru
$demoProcess, $statsProcess | Wait-Process
```

这两个控制台示例执行完就退出。`stats-report` 输出中应包含：

```text
Application: stats-report
Backend: C static library (MinGW x64)
Morning: count=3, total=60, average=20.0
Afternoon: count=2, total=60, average=30.0
Report self-check: OK
```

增加第三个 exe 时，新增一个应用模块并在 `settings.gradle.kts` 中 include，像 `stats-app` 一样配置 `binaries.executable` 和库依赖即可。

关键配置是各应用自己的 `binaries.executable`。例如 `stats-app/build.gradle.kts`：

```kotlin
mingwX64 {
    binaries.executable {
        baseName = "stats-report"
        entryPoint = "stats.main"
    }
}
```

库模块不需要声明可执行程序。两个 exe 分别链接所需代码，可以独立运行；它们不是在运行时共享一个 Kotlin 进程。

单独构建并运行 Windows Release 版本：

```powershell
.\gradlew.bat :demo-app:linkReleaseExecutableMingwX64
.\demo-app\build\bin\mingwX64\releaseExecutable\kmp-c-demo.exe
```

## Android 平台实验

新增 `android-app` 应用模块，Android 最低版本为 7.0（API 24），支持 `arm64-v8a` 和 `x86_64`，目前不包含 32 位 ABI。

构建可直接安装的 Debug APK：

```powershell
.\gradlew.bat :android-app:assembleDebug
```

产物：`android-app/build/outputs/apk/debug/android-app-debug.apk`。

连接已授权 USB 调试的 Android 设备，或启动 Android Studio 模拟器后：

```powershell
adb install -r .\android-app\build\outputs\apk\debug\android-app-debug.apk
adb shell am start -n com.example.kmpdemo/.MainActivity
```

也可以通过 Gradle 安装：`.\gradlew.bat :android-app:installDebug`。连接多个设备时，在 adb 命令后加入 `-s 设备序列号` 选择设备，例如 `adb -s emulator-5580 install -r 路径.apk`。

应用启动即显示平台、C 后端、统计结果和 `Self-check: OK`，点击 `Run shared Kotlin + C` 可以重新执行。预期结果为 count=4、total=100、average=25.0，后端为 `C shared library (Android NDK / JNI)`。

Android 调用链：

```text
android-app MainActivity
  → native-lib commonMain summarize
  → native-lib androidMain PlatformCalculator
  → android-cbridge NativeBridge（JNI）
  → libdemo_android.so → 同一份 demo.c / demo_sum
```

`native-lib` 使用官方 Android-KMP 库插件添加 Android 目标。NDK/CMake 编译封装在标准 Android 库 `android-cbridge` 中，该库作为 Android 目标依赖传递给应用。Android 应用使用 AGP 9 内置 Kotlin 支持，不需要额外应用 kotlin-android 插件。

`androidMain` 是运行在 Android Runtime 上的 Kotlin/JVM 实现；这里没有新增 `androidNativeArm64` 等 Kotlin/Native 目标。它与 Windows cinterop 的实现方式不同，但复用了相同的 Kotlin 业务代码和 C 源码。

在设备上运行 JNI 仪器测试（正常统计、空数组、负数、大整数以及后端识别）：

```powershell
.\gradlew.bat :android-app:connectedDebugAndroidTest
```

构建 APK 和测试 APK，不要求连接设备：

```powershell
.\gradlew.bat :android-app:assembleDebug :android-app:assembleDebugAndroidTest

# Android 静态检查
.\gradlew.bat :android-app:lintDebug
```

Android JNI 测试需要设备，使用上面的 `connectedDebugAndroidTest` 单独运行。Windows/JVM 共享库测试仍可用 `:native-lib:mingwX64Test :native-lib:jvmTest` 运行。

## 发布库供独立 KMP 工程使用

`nativeInterop` 是目录约定，本身不是独立的可分发模块。可复用的是包含 Kotlin 公共 API、各平台实现、cinterop 绑定及所需 C 产物的 `native-lib` KMP 库。

| 使用方式 | 消费方依赖 |
| --- | --- |
| 同一 Gradle 工程 | `implementation(project(":native-lib"))` |
| 独立工程，通过 Maven | `implementation("com.example:native-lib:1.0.0")` |

公共 API 保持普通 Kotlin 类型，调用方不需要接触 C 指针、`usePinned` 或 JNI 方法。要让消费工程在 commonMain 使用 API，发布库必须为消费方所需的目标提供可解析的实现和产物。

先发布到仓库内的本地 Maven 目录，仅写入本机文件，不上传任何远程服务：

```powershell
.\gradlew.bat :native-lib:publishAllPublicationsToDemoRepository
.\gradlew.bat :android-cbridge:publishAllPublicationsToDemoRepository
```

发布位置是 `build/demo-repo/`，依赖坐标是 `com.example:native-lib:1.0.0`。

第二条命令发布 Android JNI 依赖库；其他 Android 工程通过 Maven 使用 `native-lib` 时，需要仓库中同时存在这个传递依赖。仅供 Windows/JVM 消费时不依赖该 Android AAR。Android 消费工程还需在 repositories 中配置 `google()`。

再启动独立消费工程：

```powershell
.\gradlew.bat -p consumer runDebugExecutableMingwX64 runJvm
```

`consumer/settings.gradle.kts` 独立定义工程，没有通过 `includeBuild` 或 `project(...)` 引入生产库。使用根目录的 Wrapper 只是复用 Gradle 启动器。

`consumer/build.gradle.kts` 中只有仓库和普通 KMP 依赖配置：

```kotlin
repositories {
    maven {
        url = layout.projectDirectory.dir("../build/demo-repo").asFile.toURI()
        content { includeGroup("com.example") }
    }
    mavenCentral()
}

kotlin {
    jvm()
    mingwX64 { /* 配置消费方自己的可执行程序 */ }
    sourceSets {
        commonMain.dependencies {
            implementation("com.example:native-lib:1.0.0")
        }
    }
}
```

消费方的 `commonMain` 直接调用 Kotlin API：

```kotlin
import demo.summarize

val summary = summarize(intArrayOf(7, 8, 9))
check(summary.total == 24L)
check(summary.average == 8.0)
```

原生消费方预期输出：

```text
Dependency: com.example:native-lib:1.0.0 (Maven artifact)
Platform: Windows x64 / Kotlin Native
Backend: C static library (MinGW x64)
Consumer summary: count=3, total=24, average=8.0
Consumer self-check: OK
```

JVM 消费方输出相同的统计结果，Backend 为 `Pure Kotlin (JVM implementation)`。

消费方没有 `.c`、`.h`、`.def` 文件，没有 cinterop、gcc 或 ar 构建任务。Windows 可执行程序链接发布产物里携带的 C 静态库，运行时无需 JVM 或自定义 C DLL。

独立消费方的 exe：

```powershell
.\consumer\build\bin\mingwX64\debugExecutable\consumer-demo.exe
```

如需部署到自己的 Maven 服务，将库的 publishing 仓库地址和消费方的 repositories 地址换成该服务地址即可；认证信息按服务要求配置，不要写死在源码中。

## 目录和职责

```text
build.gradle.kts                 固定插件版本
settings.gradle.kts              包含共享库、两个 exe 应用、Android JNI 库和应用
native-lib/
  build.gradle.kts               C 编译、cinterop、KMP 库、Maven 发布
  src/commonMain/                expect API、Summary、summarize
  src/commonTest/                两端复用的 4 项测试
  src/mingwX64Main/              actual：调用 C，处理指针
  src/jvmMain/                   actual：纯 Kotlin 实现
  src/androidMain/               actual：通过 NativeBridge 调 C
  src/nativeInterop/c/           demo.c、demo.h
  src/nativeInterop/cinterop/    demo.def
demo-app/
  build.gradle.kts               project(":native-lib") 依赖
  src/commonMain/                共享演示逻辑
  src/mingwX64Main/              Windows 入口
  src/jvmMain/                   JVM 入口
stats-app/
  build.gradle.kts               第二个 exe，依赖 native-lib
  src/commonMain/                共享报表逻辑
  src/mingwX64Main/              Windows 入口 stats.main
  src/jvmMain/                   JVM 入口
android-cbridge/
  build.gradle.kts               NDK/CMake 构建、AAR 发布
  CMakeLists.txt                 复用 native-lib 中的 demo.c
  src/main/java/                JNI Java 声明与 so 加载
  src/main/cpp/                 JNI C 适配层
android-app/
  build.gradle.kts               Android APK 构建
  src/main/                     演示 Activity 和 Manifest
  src/androidTest/              设备上的 JNI 集成测试
consumer/
  settings.gradle.kts            独立工程
  build.gradle.kts               Maven 坐标依赖
  src/commonMain/                共享消费逻辑和结果校验
  src/mingwX64Main/              Windows 入口
  src/jvmMain/                   JVM 入口
build/demo-repo/                 生成的本地 Maven 仓库（Git 忽略）
```

## 为什么 C 可以随库一起复用

1. `native-lib:compileC` 用 gcc 编译 C 文件，`archiveC` 用 ar 生成 `native-lib/build/native/c/libdemo.a`。
2. cinterop 根据 `demo.h` 生成 Kotlin 绑定。`.def` 的 `staticLibraries = libdemo.a` 将静态库嵌入 cinterop `.klib`。
3. `maven-publish` 发布公共 KMP 元数据、JVM 产物、Windows Native 产物，以及 Windows cinterop 产物。
4. 消费工程声明根坐标，Gradle 按目标解析对应产物；Kotlin/Native 将 C 库自动链接进 exe。

Windows 的 `native-lib/src/nativeInterop/cinterop/demo.def`：

```properties
headers = demo.h
headerFilter = demo.h
package = demo.cinterop
staticLibraries = libdemo.a
```

静态库搜索目录由 Gradle 中的 `extraOpts("-libraryPath", ...)` 指定。实际发布的 `native-lib-mingwx64-1.0.0-cinterop-demo.klib` 已检查包含 `default/targets/mingw_x64/included/libdemo.a`。消费方使用发布产物时，不需要重新用 gcc 编译这份 C 代码。

Android 的发布链不同：`native-lib` 发布 Android AAR，并依赖 `android-cbridge` 的 AAR，后者包含两个 ABI 的 `libdemo_android.so`。这些产物也需要发布到消费方可访问的仓库。

库的公共 API 使用 `IntArray`、`Long` 等普通 Kotlin 类型。原生实现内部用 `usePinned` 在同步 C 调用期间固定数组地址；空数组传空指针和长度 0。C 以 `int64_t` 累加，避免 Int 溢出。

```kotlin
// native-lib commonMain：共享层不导入 Windows、Android 或 C API。
expect object PlatformCalculator {
    val platform: String
    val backend: String
    fun sum(values: IntArray): Long
}

fun summarize(values: IntArray): Summary =
    Summary(values.size, PlatformCalculator.sum(values))
```

Windows 实现里的关键调用：

```kotlin
// 需要 ExperimentalForeignApi opt-in 和相应 cinterop imports。
if (values.isEmpty()) return demo_sum(null, 0)
return values.usePinned { pinned ->
    demo_sum(pinned.addressOf(0), values.size)
}
```

C 函数只在同步调用期间读取数组，不保存指针；返回的后端名称指向 C 字符串字面量，Kotlin 读取字符串但不释放它。Android JNI 包装则通过 GetIntArrayElements 获取数据，并在调用后用 ReleaseIntArrayElements 释放访问。

Windows 的原生产物只能用于匹配的 `mingwX64` 目标。增加 Linux/macOS 需要分别配置目标、C 编译及 actual 实现并发布对应产物。本例 Android 已通过 JNI 调 C，普通 JVM 目标仍使用纯 Kotlin 实现。

修改库源码后，重新发布并在消费工程使用 `--refresh-dependencies`，或发布一个新版本。正式使用应通过新版本分发变更。

## Android 必须使用 JNI 吗

不一定，取决于 Android 端运行的是哪一种 Kotlin：

| 形态 | Kotlin 调 C 的方式 | 对普通 Android UI 的影响 |
| --- | --- | --- |
| 普通 Android/JVM 应用 | 通常通过 JNI，或基于 JNI 的生成绑定 | 可直接使用 Activity、View、Compose Android 等 API |
| Kotlin/Native Android 目标 | 可以像 Windows 一样通过 cinterop 调 C | 不会自动获得对 Android/JVM API 的直接访问能力 |
| NativeActivity 等原生入口 | 可从原生代码访问 NDK 提供的 C API | 访问其他 Android Java Framework API 时仍可能需要 JNI |

即使把核心改为 Kotlin/Native，只要外部入口仍是普通 Kotlin/Java Activity，调用这个原生核心时仍有 JVM 与原生代码之间的边界。cinterop 解决的是 Kotlin/Native 与 C 的绑定，并不会自动消除 Android/JVM 到原生库的桥接。

本次明确选择保留普通 Android APK，通过 JNI 调用 C；没有实现 Android Native 命令行程序、NativeActivity 应用或 Android Native 版共享库。[Android NDK 概念](https://developer.android.com/ndk/guides/concepts)

## Kotlin/Native 支持哪些 target

以下为 Kotlin 官方当前列出的目标；本项目实际配置的是 Windows `mingwX64`，Android APK 使用 Android/JVM + JNI，并未配置 Android Native 目标。

| 平台 | Gradle target | 官方支持层级 |
| --- | --- | --- |
| macOS Apple Silicon | `macosArm64` | Tier 1 |
| iOS ARM64 真机、Apple Silicon 模拟器 | `iosArm64`、`iosSimulatorArm64` | Tier 1 |
| Linux x64、ARM64 | `linuxX64`、`linuxArm64` | Tier 2 |
| watchOS ARM64、Apple Silicon 模拟器 | `watchosArm64`、`watchosSimulatorArm64` | Tier 2 |
| tvOS ARM64、Apple Silicon 模拟器 | `tvosArm64`、`tvosSimulatorArm64` | Tier 2 |
| Android ARM32、ARM64、x86、x64 | `androidNativeArm32`、`androidNativeArm64`、`androidNativeX86`、`androidNativeX64` | Tier 3 |
| Windows x64 | `mingwX64` | Tier 3 |
| watchOS ARM64 设备目标 | `watchosDeviceArm64` | Tier 3 |
| iOS Intel 模拟器 | `iosX64` | Tier 3 |

已弃用、计划移除的目标单独列为：`watchosArm32`、`macosX64`、`watchosX64`、`tvosX64`、`linuxArm32Hfp`。支持状态可能随 Kotlin 版本变化，升级时以官方表格为准。

Apple 平台最终二进制需要在 macOS 构建。Windows x64、Linux x64 主机可以构建受支持的非 Apple 目标；本项目不会因为目标受 Kotlin 支持就自动生成该平台产物，还需要添加对应 target、C 工具链和平台实现。

Tier 1/2/3 是官方测试和兼容性保障等级，不代表是否需要 JNI。Android Native 的 Kotlin 代码可以直接通过 cinterop 调 C；但 Activity/Compose 所在的 Android/JVM 与原生库交互仍需要桥接。

## Kotlin/Native 的限制

以下说明针对 Kotlin/Native 原生后端；不能把它们全部套用到 KMP 的普通 JVM 或 Android/JVM 目标。

### 库生态和语言运行时

- Native 代码不能直接使用 JVM 专用的 `java.*` API、JAR 或 Android Framework。即使依赖库用 Kotlin 编写，也需要为对应 Native 目标提供产物，或者换用支持该目标的 KMP 库。
- 有类引用、函数引用等基础能力，但不能照搬 JVM 的完整 `kotlin-reflect`、Java 反射、类扫描或动态类加载方案。依赖运行时反射的框架需要另找支持 Native 的实现。
- 可以生成不依赖 JVM 的 exe，但仍包含 Kotlin/Native 运行时、内存分配器和 GC。无需 JVM 不等于没有运行时或内存管理开销。

参考：[KMP 依赖](https://kotlinlang.org/docs/multiplatform/multiplatform-add-dependencies.html)、[反射](https://kotlinlang.org/docs/reflection.html)、[Native 内存管理](https://kotlinlang.org/docs/native-memory-manager.html)。

### 跨平台构建和发布

- 共享的是源码及公共 API，不是一个能在所有系统运行的原生二进制。目标 OS、CPU 架构、ABI 和原生依赖需要分别适配。
- C 库也要针对每个目标编译，Windows `.a` 不可直接放到 Android；添加 target 后仍需要实现平台代码和构建链。
- Apple 最终二进制需要 macOS/Xcode；支持交叉编译不意味着任意主机都能构建任意目标。
- Windows `mingwX64` 和全部 `androidNative*` 当前为 Tier 3，官方测试及跨版本兼容性保障较弱；目标可能弃用，升级前要核对支持表。

参考：[目标与主机支持](https://kotlinlang.org/docs/native-target-support.html)。

### C/C++ 互操作边界

- cinterop 自动生成绑定，不会自动验证 C 指针、数组长度、内存所有权、线程要求或回调生命周期。Kotlin 的类型安全与 GC 无法消除 C 中的越界、悬空指针和泄漏。
- C 分配的资源仍要按 C 库约定释放；Kotlin 数组交给 C 时要确保地址在整个使用期间有效。本例的 usePinned 只覆盖同步调用，不能让 C 保存该地址供之后使用。
- 复杂 C++ 类、模板等通常需要先包装成 C ABI，再供 cinterop 调用。C 互操作不是完整的 C++ 语言互操作。
- 公共库 API 尽量保持普通 Kotlin 类型，减少消费方对生成绑定类型和原生 ABI 的直接依赖。

参考：[C 互操作与内存处理](https://kotlinlang.org/docs/native-c-interop.html)、[原生库导入稳定性](https://kotlinlang.org/docs/native-lib-import-stability.html)。

### 内存、性能和构建成本

- Kotlin/Native 使用共享堆和追踪式 GC，可以跨线程共享对象；共享可变状态仍需要同步，不能把“可跨线程访问”理解为“自动线程安全”。
- GC 会带来额外内存和潜在暂停，不能直接按 C/C++ 的确定性析构或硬实时模型设计。
- AOT 生成机器码并不保证一定比 JVM 更快或二进制更小，需要用实际工作负载测量。
- 首次构建要下载工具链，多个 target 和 Debug/Release 会增加构建成本；Release 的 LLVM 优化和链接尤其耗时。日常开发优先选择必要目标的 Debug 任务。

参考：[内存管理](https://kotlinlang.org/docs/native-memory-manager.html)、[编译性能](https://kotlinlang.org/docs/native-improving-compilation-time.html)。

对本项目最重要的三个检查点是：依赖是否支持所需目标、C API 如何封装，以及各目标的构建与发布是否完整。

## Kotlin/Native 与 Rust 对比

KMP 是共享源码与多目标构建方案，Kotlin/Native 是其中的原生编译后端；Rust 是独立语言。这里比较的是实现跨平台原生核心的选择。

| 维度 | Kotlin/Native | Rust |
| --- | --- | --- |
| 内存管理 | 自动 GC，较少手动处理 Kotlin 对象生命周期 | 所有权、借用和析构，没有必需的追踪式 GC |
| 内存与资源控制 | 有 Native 运行时、分配器和 GC 开销 | 更便于精细控制分配、释放和资源生命周期 |
| 学习成本 | Kotlin 开发者更容易延续已有经验 | 需要学习所有权、生命周期和并发约束 |
| Kotlin 业务复用 | 配合 KMP，共享业务源码可分别编译到 Android/JVM、JVM 和 Native | Rust 核心要通过平台绑定给 Kotlin 调用 |
| 调用 C | cinterop 根据头文件生成绑定 | C FFI，边界通常需要 unsafe 和安全封装 |
| Windows | 当前 Native 目标走 MinGW | 提供 MSVC 和 GNU 目标 |
| 平台范围 | Windows、Linux、Android NDK 和 Apple 平台，范围取决于官方 target 表 | 覆盖更多系统和架构，也提供多种嵌入式目标；各目标支持等级不同 |
| 二进制分发 | 按 OS、CPU、ABI 分别构建 | 同样需要按目标分别构建 |

内存机制依据：[Kotlin/Native 内存管理](https://kotlinlang.org/docs/native-memory-manager.html)、[Rust 所有权](https://doc.rust-lang.org/book/ch04-01-what-is-ownership.html)。互操作和平台依据：[Rust FFI](https://doc.rust-lang.org/nomicon/ffi.html)、[Rust 平台支持](https://doc.rust-lang.org/rustc/platform-support.html)。

比较时需要保留这些边界：

- Rust 的安全保证不会自动覆盖外部 C 实现，FFI 的指针、长度、线程和所有权约定仍需正确封装。
- Rust 没有必需的 GC，不代表所有操作零开销，也不自动满足硬实时要求；分配、锁、引用计数和系统调用仍会影响延迟。
- 把 Android 原生核心换成 Rust，不会自动消除 JNI。普通 Android/JVM UI 调用原生代码仍需桥接；原生入口是另一种架构。
- 性能、启动速度、内存和包体积没有在本项目中进行 Kotlin/Native 与 Rust 对照测试，不能据此给出速度或体积排名。

本次讨论形成的选型判断如下，属于工程建议，而非基准测试结论：

| 主要目标 | 倾向选择 |
| --- | --- |
| 已有大量 Kotlin 业务，需要 Android、Windows 等平台复用 | 延续 KMP / Kotlin/Native 结构 |
| 命令行工具、协议引擎、计算核心、强调资源控制的底层 SDK | 考虑 Rust |
| 核心库需要面向 Kotlin、C、Python 等多语言分发 | 倾向 Rust 加明确的 C ABI，同时评估绑定和维护成本 |
| Kotlin 业务复用，同时存在已测量确认的计算瓶颈 | KMP 负责业务，Rust 负责热点核心 |

当前演示已经满足原生 exe、共享逻辑与 C 混编，没有仅为“生成原生程序”而重写成 Rust 的必要。Rust 示例和 Rust 绑定没有在本仓库实现。

## 已完成的验证与实验边界

下面是本次会话已实际完成的验证记录，记录日期为 2026-10-15；文档后续更新不代表每次都重新执行了全部命令。

| 项目 | 已验证结果 |
| --- | --- |
| Windows 原生演示 | 输出 C 静态库后端、total=100、average=25.0、自检通过；exe 可脱离 Gradle 直接启动 |
| JVM 演示 | 使用纯 Kotlin 实现，输出与共享业务预期一致 |
| 多 exe 应用 | demo-app 和 stats-app 均构建并启动成功；两个独立进程退出码均为 0 |
| Windows/JVM 测试 | commonTest 的 4 项测试在两个目标各执行一次，共 8 项通过 |
| 独立 Maven 消费 | consumer 仅使用发布坐标，Windows/JVM 均运行成功；消费方没有 C 源码或 cinterop 配置 |
| Windows 发布包 | cinterop klib 中确认包含 libdemo.a |
| Android 构建 | Debug APK 和仪器测试 APK 构建成功；APK 中确认包含 ARM64 和 x86_64 的 libdemo_android.so |
| Android 运行 | Android 15 / API 35 x86_64 模拟器中安装、启动成功，页面展示 C 后端及 Self-check: OK，并验证重新运行按钮 |
| Android 设备测试 | 正常统计、空数组、负数、大整数、后端识别，共 5 项通过 |
| Android 检查与发布 | lintDebug 通过；native-lib Android AAR 和 android-cbridge AAR 已发布到本地演示仓库 |

验证范围说明：Android ARM64 只验证了编译和 APK 打包，未在 ARM64 真机运行；没有实现 Linux/macOS/iOS、Android Native 或 Rust 演示，也未记录完整的跨平台性能对比。Windows Release 运行命令已提供，但不将其列为本次已验证运行项。

生成的测试报告和 APK 位于各模块 build 目录，构建清理后可重新生成；它们不作为源码提交。需要复现时，先执行本文的环境检查，再按对应平台运行命令。

## 官方资料

- [KMP 库发布](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html)
- [C 静态库包含到 klib](https://kotlinlang.org/docs/native-definition-file.html#include-a-static-library)
- [C 互操作](https://kotlinlang.org/docs/native-c-interop.html)
- [expect/actual](https://kotlinlang.org/docs/multiplatform/multiplatform-expect-actual.html)
- [官方 Android-KMP 库插件与原生库接入方式](https://developer.android.com/kotlin/multiplatform/plugin)
- [Kotlin/Native 目标与构建主机支持表](https://kotlinlang.org/docs/native-target-support.html)
- [Kotlin/Native 概览](https://kotlinlang.org/docs/native-overview.html)
- [Kotlin/Native 内存管理](https://kotlinlang.org/docs/native-memory-manager.html)
- [Kotlin/Native 编译性能](https://kotlinlang.org/docs/native-improving-compilation-time.html)
- [KMP 库依赖](https://kotlinlang.org/docs/multiplatform/multiplatform-add-dependencies.html)
- [Kotlin 反射与 JVM 差异](https://kotlinlang.org/docs/reflection.html)
- [Android NDK 与 JNI](https://developer.android.com/ndk/guides/concepts)
- [Rust 所有权](https://doc.rust-lang.org/book/ch04-01-what-is-ownership.html)
- [Rust FFI](https://doc.rust-lang.org/nomicon/ffi.html)
- [Rust 平台支持](https://doc.rust-lang.org/rustc/platform-support.html)
