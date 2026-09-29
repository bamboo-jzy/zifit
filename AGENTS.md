# AGENTS.md

> 面向 AI 编码代理的项目约定。**开始任何任务前先读完本文件。**
> 人类贡献者同样适用;其中「计划模式 / 子代理 / 教训沉淀」三节是为 AI 协作设计的。

---

## 1. 项目概述与技术栈

### 1.1 项目简介

ZiFit 是一个由 **AI 驱动**的个人健身管理 Android 应用。以三个数据底座 ——
**健身动作库**、**食材库**、**身体数据管理** —— 为输入,自动生成**训练计划**与
**饮食计划**,且两份计划**均可手动修改**。

- 唯一用户是开发者本人,产物为**侧载 APK**,不上架任何应用市场。
- 差异化重点:① 计划由 AI 生成,而非套用固定模板;② 训练与饮食双计划合一。

### 1.2 技术栈

| 层 | 技术 |
|---|---|
| 语言 | **Kotlin 2.3.20** |
| UI | **Jetpack Compose**(BOM `2026.03.01`)+ Material 3,声明式,**无 XML 布局** |
| 导航 | **Navigation 3**(`androidx.navigation3` `1.0.1`) |
| 异步 | Kotlin Coroutines / Flow(`1.10.2`) |
| 状态 | ViewModel + `StateFlow` + `sealed interface` 状态建模 |
| 持久化 | **SQLite**(计划经 Room 访问,尚未引入;当前数据层为占位实现) |
| 构建 | Gradle `9.1.0`(项目 wrapper)+ **AGP 9.0.1** |
| 测试 | JUnit 4(单元)/ Compose UI Test + Espresso(仪器) |
| SDK | compileSdk `36` / targetSdk `36` / minSdk `26` |

**不使用**:Android Studio、任何前端框架、任何额外打包器。
业务代码**一律 Kotlin**,不写 Java。

### 1.3 目标运行环境

| 项 | 值 |
|---|---|
| 真机 | 荣耀 Magic5(`PGT-AN00`),Android 16 / SDK 36,MagicOS |
| ABI | `arm64-v8a` |
| GMS | **无** —— 国内 MagicOS 不预置谷歌服务 |

---

## 2. 硬约束(Project Invariants)

**以下条目违反即任务失败,不接受"更省事"的替代方案。**

| # | 约束 | 原因 |
|---|---|---|
| C1 | 项目路径必须**纯 ASCII**,不得含中文或空格 | AGP 9 直接拒绝构建:`Your project path contains non-ASCII characters`。`android.overridePathCheck=true` 只屏蔽检查、底层仍会失败,**不可采用** |
| C2 | **禁止**引入任何 Google 服务依赖:`play-services-*`、Firebase / FCM、Google Maps、Google 登录、Play Core / Integrity | 目标机型无 GMS,引入即运行时崩溃 |
| C3 | **禁止**引入闭源第三方 SDK | GPL-3.0 §5(c)/§10 义务;当前全部依赖均为 Apache-2.0 |
| C4 | 每个新增 `.kt` 源文件**必须**带 GPL-3.0 版权头(模板见 §5.2) | GPL-3.0 §5(a) 要求显著标注修改 |
| C5 | **不得删除或改写 `LICENSE`** | 该文件是 FSF 官方 GPL-3.0 原文,GPL 禁止改动许可文本 |
| C6 | `.workbuddy/` 必须保持在 `.gitignore` 中 | 内含本地记忆与工作区数据,不得入库 |
| C7 | 源码换行符为 **LF**,`gradlew` 尤其不得被改成 CRLF | 本机 `core.autocrlf=true`,已由 `.gitattributes` 显式锁定;一旦被改成 CRLF,Git Bash 下 `./gradlew` 会报 `bad interpreter` |
| C8 | 提交作者邮箱必须为 `bamboomail_j@163.com`(仓库局部配置),**不得**使用公司邮箱 | 仓库公开,避免泄露工作身份 |
| C9 | 提醒 / 定时类功能只能依赖系统本地通知(`AlarmManager` / `WorkManager` + `NotificationManager`) | 无 GMS 推送可用;荣耀推送需先上架其应用市场,不适用 |
| C10 | 在 Git Bash 中路径必须写 POSIX 风格(`/d/projects/...`),写 `D:/...` 无效 | Git Bash 的 PATH 只认 POSIX 路径 |

---

## 3. 环境与常用命令

### 3.1 环境准备

项目**全程命令行构建,不依赖 Android Studio**。

```bash
export JAVA_HOME="<path-to-jdk-21>"          # 构建 JVM 用 JDK 21
export ANDROID_HOME="<path-to-Android-Sdk>"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

- 构建 JVM = **JDK 21**;Kotlin 工具链固定为 **JDK 17**(`jvmToolchain(17)`),两者都要装。
- SDK 需含 `platforms/android-36` 与 `build-tools/36.0.0`。
- SDK 管理命令是 **`android`**(新版 CLI);`sdkmanager` **已废弃**,不要再调用。
- ⚠️ `android` 命令必须**串行执行** —— 并发会争抢 `.sdk/refs/remote` 锁导致失败。
- 依赖仓库已在 `settings.gradle.kts` 前置国内镜像(阿里云),官方源兜底;
  Gradle 发行包走腾讯云镜像(见 `gradle/wrapper/gradle-wrapper.properties`)。
- `local.properties` 保存本机 SDK 路径,**已被 gitignore**,不要提交、不要手改远程值。

### 3.2 构建 / 安装 / 运行

| 目的 | 命令 |
|---|---|
| 构建 debug APK | `./gradlew assembleDebug` |
| 构建并安装到已连接真机 | `./gradlew installDebug` |
| 手动安装已有 APK | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| 清理构建产物 | `./gradlew clean` |
| 启动 App | `adb shell am start -n com.zifit.app/.MainActivity` |
| 强制停止 | `adb shell am force-stop com.zifit.app` |
| 卸载 | `adb uninstall com.zifit.app` |

产物路径:`app/build/outputs/apk/debug/app-debug.apk`

### 3.3 质量检查与验证

| 目的 | 命令 |
|---|---|
| 静态检查 | `./gradlew lint` |
| JVM 单元测试 | `./gradlew test` |
| 真机仪器测试(需连接设备) | `./gradlew connectedAndroidTest` |
| **提交前组合检查(推荐)** | `./gradlew lint test assembleDebug` |
| 列出已连接设备 | `adb devices` |
| 查看 App 日志 | `adb logcat --pid=$(adb shell pidof -s com.zifit.app)` |
| 确认 App 是否在前台 | `adb shell dumpsys activity activities \| grep zifit` |

**本项目没有 HTTP 后端**,不存在 `curl /api/health` 之类的健康检查。
在这里,"服务是否可用"等价于 **"APK 能装、能起、不崩"** —— 判定标准见 §6.4。

### 3.4 真机连接:必须走无线调试

本机连荣耀真机时 **USB 调试不可用**(荣耀 HDB 抢占 USB 接口,Windows 侧从未
暴露 ADB 接口)。这是环境事实,**不要反复重试 USB 排障**,直接用无线调试:

```bash
# 1) 手机:设置 → 关于手机 → 连点「版本号」7 次 → 开发者选项 → 打开「无线调试」
# 2) 确保手机与电脑处于同一局域网
# 3) 手机「无线调试 → 使用配对码配对设备」,记下 IP:配对端口 与 6 位配对码
adb pair <手机IP>:<配对端口> <配对码>
adb connect <手机IP>:<连接端口>
adb devices    # 期望输出 <手机IP>:<端口>  device
```

注意:

- 配对端口与连接端口**通常不同**,且每次重开无线调试都会变;
  可用 mDNS(`_adb-tls-pairing._tcp`)自动发现配对端口。
- adb daemon **不跨命令存活**:换一条命令后如需重新连接,先 `adb connect`。
- 手机弹「是否允许 USB 调试」时勾选**始终允许**。
- 安装被 MagicOS 拦截:退出「纯净模式」,或安装时点「继续安装」并验证荣耀账号。
- `adb devices` 为空 **≠** 线材坏。若 Windows 已枚举出手机的 MTP 接口,
  说明线材与驱动正常,问题只在手机侧是否开放了调试。

---

## 4. 架构说明

本项目遵循**分层架构**,依赖方向**单向向下**,下层不得反向依赖上层:

| 层 | 职责 | 位置 |
|---|---|---|
| **UI 层**(Compose) | 渲染与交互。**无业务逻辑、无数据获取** | `ui/**/*Screen.kt` |
| **ViewModel 层** | 持有 UI 状态(`StateFlow<XxxUiState>`),编排用例 | `ui/**/*ViewModel.kt` |
| **Repository 层** | 数据访问的**唯一出口**(DB / 文件 / 网络) | `data/**/*Repository.kt` |
| **导航层** | 路由 key 与返回栈 | `Navigation.kt` / `NavigationKeys.kt` |
| **主题层** | 颜色 / 字体 / 形状 | `theme/*.kt` |

按 Web 项目的类比:`Screen` ≈ Controller,`ViewModel` ≈ Service,
`Repository` ≈ Repository/DAO。领域用例变复杂时,再引入 `domain/` 层,
不要提前造层。

### 4.1 现有目录结构

```
app/src/main/java/com/zifit/app/
├── MainActivity.kt          # 唯一 Activity,承载 Compose 内容
├── Navigation.kt            # Navigation 3 路由表(NavDisplay + entryProvider)
├── NavigationKeys.kt        # @Serializable NavKey 路由定义
├── data/                    # Repository 层
│   └── DataRepository.kt
├── theme/                   # Color.kt / Theme.kt / Type.kt
└── ui/
    └── main/                # MainScreen.kt + MainScreenViewModel.kt
app/src/main/res/            # strings / themes / 图标 / backup 与 data-extraction 规则
app/src/test/                # JVM 单元测试(无设备可跑)
app/src/androidTest/         # 仪器测试(需真机)
```

构建配置:`gradle/libs.versions.toml` 是**版本唯一来源**,任何依赖版本只在此处声明。

> 架构文档:`docs/architecture.md`(数据层)、`docs/frontend-architecture.md`(UI 层)。
> 二者尚不存在 —— 首次改动对应层时一并建立,**别把架构知识只留在对话里**。

### 4.2 新增功能时的固定动作

1. 先在 `data/` 扩展 Repository 接口与实现(含表结构设计)。
2. 新增 `ui/<feature>/XxxViewModel.kt`(`StateFlow<XxxUiState>`)与
   `XxxScreen.kt`(纯渲染 `@Composable`)。
3. 在 `NavigationKeys.kt` 定义路由 key,在 `Navigation.kt` 注册 `entry<T>`。
4. **禁止在 `@Composable` 中直接访问 Repository**,一律经 ViewModel。
5. 同步补测试:`app/src/test/**/XxxViewModelTest.kt`(用 Fake Repository)。

---

## 5. 编码规范

### 5.1 风格

- Kotlin 官方代码风格(`kotlin.code.style=official`),缩进 **2 空格**。
- 注释与 UI 文案统一**简体中文**;标识符用英文。
- 用户可见字符串**必须**放在 `res/values/strings.xml`,不得硬编码进 Composable。
- UI 状态用 `sealed interface` + `object` / `data class` 建模(参见 `MainScreenUiState`)。
- 不新增依赖,除非确有必要;引入前必须先核对 §2 的 C2 / C3。

### 5.2 GPL 头注释(每个 `.kt` 文件必备)

新增 Kotlin 源文件必须以如下头注释开头,**一字不改**(年份可变):

```kotlin
/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
```

`*.kts` 构建脚本与资源文件**暂不加**该头。

---

## 6. 工作流编排 (Workflow Orchestration)

### 6.1 计划模式 (Plan Mode)

- **任何非琐碎任务**(3+ 步或涉及架构决策)**必须**先进入计划模式。
- 在 `tasks/todo.md` 中撰写详细计划,并使用可勾选清单。
- 一旦执行出现偏差,**立即停止**并重新规划,不要继续硬推。
- **验证步骤也要使用计划模式**,而非仅用于构建。

### 6.2 子代理策略 (Subagent Strategy)

- **大量使用子代理**,将研究、探索、并行分析等任务外包,保持主上下文窗口干净。
- 每个子代理只专注一个方向;解决复杂问题时,通过子代理投入更多算力。

### 6.3 自我改进循环 (Self-Improvement Loop)

- 用户**任何一次纠正**后,**立即**更新 `tasks/lessons.md` 并记录错误模式。
- 每次会话开始时,**必须先复习**项目相关的 lessons。
- 无情迭代 lessons,直到错误率下降。

> 本项目已有可沉淀的教训种子,首次创建 `tasks/lessons.md` 时请先录入:
> AGP 拒绝非 ASCII 路径;Git Bash 的 POSIX 路径要求;`core.autocrlf` 改坏 `gradlew`;
> `android` CLI 并发争锁;荣耀 HDB 占用 USB 导致必须走无线调试;`.workbuddy` 必须 gitignore。
> 新踩的坑请**追加**,不要只留在对话里。

### 6.4 完成前验证 (Verification Before Done)

- **绝不在证明它能工作前**标记任务完成。
- 本项目判定"能工作"的最低标准(按改动范围取用):
  1. `./gradlew lint test assembleDebug` 全绿;
  2. `adb install -r` 成功,**App 能启动且不崩溃**;
  3. `adb logcat` 中无 `FATAL EXCEPTION`、无新增 error 级日志;
  4. 涉及 UI 的改动:真机截屏取证,或在 `connectedAndroidTest` 中有对应断言。
- 必要时对比主分支与修改后的行为差异。
- 自问:"资深工程师会批准吗?"

### 6.5 要求优雅但平衡 (Demand Elegance)

- 非琐碎改动时暂停,思考:"有没有更优雅的方式?"
- 若修复感觉 hacky,基于现有知识实现优雅方案。
- **简单问题不要过度工程化**,每次呈现前先挑战自己的工作。

### 6.6 自主 Bug 修复 (Autonomous Bug Fixing)

- 收到 bug 报告后**直接修复**,无需用户手把手。
- 先指向证据:`adb logcat` 输出、Gradle 失败任务、`lint` 报告,然后解决。
- 修完按 §6.4 给出验证结果,用户无需上下文切换。
- 自动修复失败的 CI 测试。

---

## 7. 任务管理 (Task Management)

1. **先规划**:将计划写入 `tasks/todo.md`。
2. **验证计划**:实现前先 check-in。
3. **跟踪进度**:每完成一项即标记。
4. **解释变更**:每步提供高层总结。
5. **记录结果**:在 `tasks/todo.md` 末尾添加 review 部分。
6. **捕捉教训**:纠正后更新 `tasks/lessons.md`。

---

## 8. Git 与提交约定

| 项 | 值 |
|---|---|
| 远程 | `git@github.com:bamboo-jzy/zifit.git`(SSH) |
| 主分支 | `main` |
| 可见性 | **public** —— 任何提交即刻公开 |
| 提交语言 | 中文 |
| 提交前缀 | Conventional Commits:`feat:` / `fix:` / `chore:` / `docs:` / `refactor:` / `test:` |

提交前自查:

- 已过 §6.4 验证。
- 未提交 `local.properties`、`build/`、`.gradle/`、`.kotlin/`、`.workbuddy/`。
- 无密钥 / 令牌,**无本机绝对路径**,无公司邮箱。
- 新增 `.kt` 文件均已带 GPL 头(§5.2)。

---

## 9. 核心原则 (Core Principles)

- **简洁优先**:每次变更尽量简单,只影响最小代码。
- **绝不偷懒**:找到根因,不用临时修复,坚持资深开发者标准。
- **最小影响**:只修改必要部分,避免引入新 bug。
- **约束优先**:§2 的硬约束优先于任何"更省事"的实现方案。
