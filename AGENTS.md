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
| C11 | **RepDB 数据集与其中文译文一律不得提交进仓库**(已由 `.gitignore` 忽略 `/app/src/main/assets/repdb/`) | 该数据集受 RepDB Free Tier License v1.0 Term 3 约束:**禁止**作为数据集再分发。本仓库是 public,提交即构成再分发。数据只随 APK 分发(Term 1 允许的应用内使用) |
| C12 | **不得把 RepDB 的图片送入任何生成式模型**(img2img、风格迁移、微调、多模态识别等) | 许可 Term 5 明确禁止;其输出还会被认定为"派生数据集",连带在应用里都不允许。**文本不受此限**,但译文同样受 C11 约束 |
| C13 | **`.env` 一律不得提交**(已由 `.gitignore` 忽略),且**只有 debug 包**可以注入其中的密钥;可提交的只有 `.env.example`,且里面**只能有占位值** | 仓库 public;`.env` 里是真密钥,提交即泄露且永久留在 Git 历史。release 包内嵌密钥等于把密钥随 APK 分发出去 |
| C14 | **签名密钥库(`.jks` / `.p12` / `.keystore`)与其口令文件 `keystore.properties` 一律不得入库**,且密钥库本体**存放在仓库目录之外** | 仓库 public,私钥进了 Git 历史就等于把签名权交出去,且拿不回来。放仓库之外是为了防 `git add -A` 之类的误操作。密钥库丢失 = 再也发不出同签名的更新包,已装的人只能卸载重装 |
| C15 | **发布前必须验证 release 包内不含任何密钥** —— `AI_ENV_*` 三项在 release 下恒为空串 | 发布包全世界可下载,密钥进包即泄露(C13 在发布侧的延伸)。核对方式见 §3.5 |

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

> 💡 调试用的 AI 密钥放在仓库根 `.env`(不入库),构建时自动注入 debug 包,装完即用,
> 见 §5.6。`.env` 改动**不影响已安装的包** —— 必须重新 `assembleDebug` 再装。

> ⚠️ **真机验证结束后必须留装 App,禁止卸载。** 用户需要在手机上手点按体验。
> `./gradlew connectedAndroidTest` 跑完**默认会卸载** app 与 test APK(AGP 行为),
> 会出现"刚验证完、手机上却找不到 App"的情况。本项目已在 `gradle.properties` 固定:
> ```
> android.injected.androidTest.leaveApksInstalledAfterRun=true
> ```
> 另:验证流程的**最后一步恒为安装 + 启动**(`adb install -r` → `am start`),
> 无论前面跑过什么测试。`adb uninstall` 与 `./gradlew clean` 之后**必须**重装。

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

### 3.4 真机连接:USB 优先,无线调试兜底

> **2026-09-30 更正**:此前记录「USB 调试在本机不可用(荣耀 HDB 抢占接口)」**不成立**。
> 实测开启开发者选项里的「USB 调试」后,Windows **可以**枚举出手机的 ADB 接口
> (`USB\VID_339B&PID_107D&MI_02`,驱动 `winusb.inf`),`adb devices` / `install -r` /
> `am start` 全链路实测可用(序列号 `A4YQVB3325001966`,机型 `PGT-AN00`)。

**USB 连接(首选)**

```bash
# 1) 手机:设置 → 关于手机 → 连点「版本号」7 次 → 开发者选项 → 打开「USB 调试」
# 2) 数据线连电脑(荣耀 HDB 会同时占用接口,但不影响 ADB)
adb devices -l
# 期望:A4YQVB3325001966  device product:PGT-AN00 model:PGT_AN00 device:HNPGT
```

⚠️ **关键坑:开启 USB 调试后若 `adb devices` 仍为空,拔插一次数据线。**
adb(实测 37.0.0)在 Windows 上**不会**为「adb 进程启动前就已存在」的 ADB 接口补做枚举,
必须收到一次设备到达事件才会识别。实测同一根线、同一状态,拔插后设备立刻出现。

> 排障留痕:当 Windows 已枚举出 ADB 接口而 `adb devices` 仍为空时,可用一个
> WinUSB 直连探针脚本(读接口描述符 + 发 ADB `CNXN` 握手;属本机一次性工具,未入仓库)判定
> 到底是「手机端 adbd 没起来」还是「主机侧 adb 没枚举到」。注意 WinUSB 的 `CreateFile`
> 必须带 `FILE_FLAG_OVERLAPPED`,否则 `WinUsb_Initialize` 报 err=6。

**无线调试(兜底)**

手机与电脑处于同一局域网时(手机:开发者选项 → 无线调试 → 使用配对码配对设备):

```bash
# 1) 手机:设置 → 关于手机 → 连点「版本号」7 次 → 开发者选项 → 打开「无线调试」
# 2) 确保手机与电脑处于同一局域网
# 3) 手机「无线调试 → 使用配对码配对设备」,记下 IP:配对端口 与 6 位配对码
adb pair <手机IP>:<配对端口> <配对码>
adb connect <手机IP>:<连接端口>
adb devices    # 期望输出 <手机IP>:<端口>  device
```

注意事项:

- **无线调试**:配对端口与连接端口**通常不同**,且每次重开无线调试都会变;
  可用 `adb mdns services` 或 mDNS(`_adb-tls-pairing._tcp`)发现。
- **无线调试**:adb daemon **不跨命令存活**,换一条命令后需先 `adb connect`。
- 手机弹「是否允许 USB 调试」时勾选**始终允许**。
- 安装被 MagicOS 拦截:退出「纯净模式」,或安装时点「继续安装」并验证荣耀账号。
- `adb devices` 为空 **≠** 线材坏。Windows 能枚举出手机的接口即说明线材与驱动正常,
  此时按本节「关键坑」拔插一次数据线即可。

---

### 3.5 发布 release APK

发版走 GitHub Actions,**打 tag 即发布**(`.github/workflows/release.yml`):

```bash
git tag v1.0 && git push origin v1.0
```

工作流依次做四件事。每一件都对应一个"不做就会静默出错"的点 —— 这类错误不会让构建失败,
只会产出一个看着正常、实际有问题的包:

| 步骤 | 为什么必须显式做 |
|---|---|
| 从 tag 推导版本号 | `versionName` / `versionCode` 由 tag 反推(`v1.2.3` → `1.2.3` / 10203),经 `-PzifitVersionName` / `-PzifitVersionCode` 传给 Gradle。不传就每个版本都是 `1.0` |
| **拉 RepDB 数据集** | `assets/repdb/` 被 `.gitignore` 忽略(checkout 出来是空的),不跑 `tools/fetch_repdb.sh` 就产出一个「动作库 0 条」的废包 |
| 解码签名密钥库 | 密钥库放在 Secrets 里(base64),解到 `$RUNNER_TEMP` 下随 runner 销毁;工作目录里不留文件 |
| **校验产物已签名** | 签名四项缺一项时,`assembleRelease` 会**安静地产出** `app-release-unsigned.apk` —— 能上传、装不上。工作流显式检查 `app-release.apk` 存在并跑 `apksigner verify` |

**签名的两个来源** —— `app/build.gradle.kts` 的优先级是 **环境变量 > `keystore.properties`**,
两边键名一一对应(`ZIFIT_KEYSTORE_FILE` / `ZIFIT_STORE_PASSWORD` / `ZIFIT_KEY_ALIAS` / `ZIFIT_KEY_PASSWORD`):

- 本机:仓库根的 `keystore.properties`(**已被 gitignore**),`storeFile` 指向仓库外的密钥库;
- CI:Secrets 里的四项。

四项不齐时**刻意不建 `signingConfig`**,产物退回 unsigned 包。**不拿 debug 签名顶上** ——
那样出来的包装得上、却覆盖不了已有的 release 环境,是最难排查的一类问题。

**生成密钥库**(本机,一次性;`<仓库外目录>` 用绝对路径):

```bash
"$JAVA_HOME/bin/keytool" -genkeypair -v \
  -keystore <仓库外目录>/zifit-release.p12 -storetype PKCS12 \
  -alias zifit -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "<口令>" -keypass "<口令>" \
  -dname "CN=ZiFit Release, O=bamboo-jzy, C=CN"

base64 -w0 <仓库外目录>/zifit-release.p12   # 输出填进 ZIFIT_KEYSTORE_BASE64
```

⚠️ `-storepass` 与 `-keypass` 必须**填同一个口令**:PKCS12 不支持两者不同,不一致会有告警。
⚠️ 口令与密钥库都要**离线备份**(C14)。

**发布前核对**(顺序固定,别跳):

1. `./gradlew lint test assembleDebug` 全绿(§6.4)。
2. **包内无密钥**(C13 / C15):看
   `app/build/generated/source/buildConfig/release/com/zifit/app/BuildConfig.java`,
   `AI_ENV_API_KEY` / `AI_ENV_BASE_URL` / `AI_ENV_MODEL` **三项都必须是 `""`**;
   再抽一次 `classes*.dex` 搜 `.env` 里的真值做双重确认。
   ⚠️ 别拿"包里有 `https://api.deepseek.com`"当泄露 —— 那是 `AiPresets` 里公开的服务商预设。
3. **动作库数据进了包**:`unzip -l app-release.apk | grep -c "assets/repdb/"` 应在 1060 左右
   (1056 张图 + `exercises.json` + `LICENSE-DATA.md` + `SNAPSHOT.txt`)。
4. **签名是 release 的**:`apksigner verify --print-certs` 打印的 DN 应为 `CN=ZiFit Release`,
   **不是** `CN=Android Debug`。
5. 公开仓库三项自查(§8):无密钥 / 无本机绝对路径 / 无公司邮箱。

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
├── Navigation.kt            # 根导航:录制背景图形层 + 毛玻璃 dock + NavDisplay 路由分发
├── NavigationKeys.kt        # @Serializable NavKey 路由定义(动作库/训练/饮食/食材库/我的)
├── TopLevelDestination.kt   # 底部五个一级入口(无障碍文案 + 图标 + 对应 NavKey)
├── data/                    # Repository 层
│   ├── repdb/               # 动作库数据源:RepDbDto / Exercise / ExerciseTaxonomy
│   │                        #   / ExerciseRepository(见 §5.4)
│   ├── food/                # 食材库:Food(Room @Entity) / FoodDao / FoodDatabase
│   │                        #   / FoodRepository / FoodSearchSource(AI 接入的唯一接缝)
│   │                        #   / FoodDraft(纯函数校验) / FoodFormat(缺测值渲染)
│   └── ai/                  # AI 接入配置:AiConfig(模型 + 草稿校验 + 密钥掩码)
│   │                        #   / AiConfigRepository(SharedPreferences 唯一出口)
│   │                        #   / AiConnectionTester(HTTP 探针) / AiPresets(填空预设)
│   │                        #   / AiEnvDefaults(读 BuildConfig 里 .env 注入的三项,见 §5.6)
│   └── user/                # 个人资料:UserProfile(名称/生日/年龄/目标/头像路径 + Goal 枚举)
│                            #   / UserProfileRepository(SharedPreferences 唯一出口)
│                            #   / AvatarStore(把相册选中的图复制进私有目录,见 §5.7)
│   └── ui/                  # 界面偏好:UiPreferences(毛玻璃模糊 / 主题模式 / 主题色)
│                            #   / UiPreferencesRepository(**进程内单例**,见 §5.7)
├── theme/                   # Color.kt(**四套调色板 + 取色 getter**,见 §5.3)
│                            #   / Theme.kt(ZiFitTheme(mode, accent) + 系统栏明暗)
│                            #   / Type.kt
└── ui/
    ├── common/              # GlassBackground / GlassSurface / GlassDock(见 §5.3)
    │                        #   + Backdrop(录制器) / GlassBlurLayer(模糊层)
    │                        #   + GlassBackdropSource(页内录制源) —— 三者一起构成实时背景模糊
    │                        #   + GlassControls(FilterPill / glassRow / SectionTitle)
    │                        #   + GlassTextField(表单输入框)
    │                        #   + GlassPickerField(只能选择不能打字的字段,如生日)
    │                        #   + PageHeader(二级页页头 / IconAction / NavRow / SwitchRow
    │                        #       / ChoiceRow 单选胶囊行 / SwatchRow 取色行 / HintText)
    ├── appearance/          # 用户界面设置:UiSettingsScreen + UiSettingsViewModel
    │                        #   + AppearanceLabels(主题模式 / 主题色 / 毛玻璃状态的文案唯一出口)
    ├── exercise/            # 动作库:ExerciseLibraryScreen + ExerciseDetailScreen
    │                        #   + 两个 ViewModel + ExerciseCommon(图片/胶囊/行底)
    ├── training/            # TrainingScreen.kt(训练)
    ├── diet/                # DietScreen.kt(饮食)
    ├── food/                # 食材库:FoodLibraryScreen + FoodLibraryViewModel
    │                        #   + FoodEditorDialog(手动新建/编辑表单)
    └── profile/             # 我的:ProfileScreen(头像 + 用户名 + 设置图标,无标题)
                             #   + SettingsScreen(一张卡四行:个人资料 / AI 接入 / 用户界面 / 关于)
                             #   + ProfileEditScreen(名称/生日/年龄/目标/头像)
                             #   + AiConfigScreen(原 AiConfigDialog,已改为整页)
                             #   + AboutScreen(许可声明与数据出处)
                             #   + ProfileViewModel(AI 接入) / UserProfileViewModel(个人资料)
                             #   + Avatar.kt / ProfileLabels.kt
app/schemas/                 # Room 导出的表结构 JSON(**入库**,是迁移正确性的唯一凭据)
app/src/main/assets/repdb/   # RepDB 免费层数据集(**不入库**,见 C11;由 tools/fetch_repdb.sh 拉取)
app/src/main/res/drawable/   # dock 图标 ic_tab_*.xml + 界面图标 ic_{search,back,settings,chevron_right,person,ai,info,ui,contrast,blur}.xml(矢量,不加依赖)
app/src/main/res/            # strings / themes / 图标 / backup 与 data-extraction 规则
app/src/test/                # JVM 单元测试(无设备可跑)
app/src/androidTest/         # 仪器测试(需真机)
tools/fetch_repdb.sh         # 拉取动作数据的唯一入口(幂等,--force 重拉)
.env                         # 本机调试密钥(**不入库**,见 C13 / §5.6)
.env.example                 # 上者的模板,占位值,**入库**
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
- UI 状态用 `sealed interface` + `object` / `data class` 建模(如 `XxxUiState.Loading` /
  `Error` / `Success`)。
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

### 5.3 视觉规范:毛玻璃 (Glassmorphism)

整体风格为**毛玻璃**,由四部分组成,改动其一即改动全局观感:

| 组成 | 位置 | 说明 |
|---|---|---|
| 背景光斑 | `ui/common/GlassBackground.kt` | 基底渐变 + 六团径向渐变光斑。页面内玻璃卡片的"磨砂感"来源 |
| 玻璃面板 | `ui/common/GlassSurface.kt` | 半透明渐变填充 + 1dp 高光描边 + 淡投影。**不做模糊**,靠背后光斑透出 |
| 悬浮层 | `ui/common/GlassDock.kt`(底部 dock)/ `GlassBlurLayer.kt`(模糊层)/ `Backdrop.kt`(录制器)/ `GlassBackdropSource.kt`(页内录制源) | 压在内容之上,须**实时模糊背后的真实画面**(见「实时背景模糊」) |
| 调色板 | `theme/Color.kt` | **四套调色板**(彩色·浅 / 浅色 / 彩色·深 / 深色)× **主题色**。基底 / 光斑 / 玻璃填充与描边 / 投影 / 文字,全部集中于此 |

**主题可切换(2026-09-30 起,用户要求)**。三个维度,都在「我的 → 设置 → 用户界面」:

| 维度 | 取值 | 实现 |
|---|---|---|
| 主题模式 | **2×2 四档**,见下表 | `ThemeMode` → `ZiFitPalette` 四选一 |
| 主题色 | 蓝(默认) / 薄荷 / 紫 / 陶土 / 玫红 | `AccentColor` → 覆盖 palette 的 `accent` |
| 毛玻璃模糊 | 开(默认) / 关 | 渲染路径开关,见下 |

**主题模式的两条轴**(2026-09-30 二改定稿):**有没有色相** × **明暗**。两条轴互相独立,
不要按"深浅"去理解「彩色」两个字 —— 它说的是**光斑有没有颜色**:

| | 浅(白天) | 深(夜间) |
|---|---|---|
| **彩色**(彩色光斑) | `COLORFUL_LIGHT` 彩色·浅(默认) | `COLORFUL_DARK` 彩色·深 |
| **中性**(钢灰光斑) | `NEUTRAL_LIGHT` 浅色 | `NEUTRAL_DARK` 深色 |

- 四档在页面上按 **2×2** 排(`ChoiceRow(maxPerRow = 2)`),浅组一行、深组一行;
  交给宽度自动换行会随字体与机型漂移成 3+1,分组语义就没了。
- ⚠️ **枚举名就是 `SharedPreferences` 的存储值**。2026-09-30 这一轮把三档改四档、
  名字全换过,所以 `ThemeMode.fromStorage()` 里必须**认旧名**
  (`COLORFUL`→`COLORFUL_LIGHT`、`LIGHT`→`NEUTRAL_LIGHT`、`DARK`→`COLORFUL_DARK`)。
  漏了这一步的表现不是崩溃,而是"存过主题的人一升级,选择被静默重置回默认"——
  比崩溃更难发现。旧 `DARK` 要迁到 `COLORFUL_DARK`(当年的「深色」就是彩色的深色版)。

- **取色一律走 `theme/Color.kt` 里那些 `@Composable` getter**(`Accent` / `InkMuted` /
  `GlassRowFill` …),它们是"当前调色板"的读出口。**不要再往 `Color.kt` 加顶层常量**,
  也不要在业务代码里写 `Color(0x…)` —— 那样写出来的颜色在深色模式下不会跟着变,
  等于在深底上钉死一个浅色。
- ⚠️ 代价:这些 getter 是 `@Composable`,**只能在组合作用域里读**。非组合上下文
  (如 `fun Modifier.glassRow()`)要么加 `@Composable`(本项目采用),要么把颜色当参数传进去。
- `ZiFitTheme(mode, accent, content)` 是**纯函数** —— 参数由 `MainActivity` 从
  `UiPreferencesRepository` 读出后传入,主题自己不去摸存储。别在别处再套一层
  `ZiFitTheme`,也别给它加"自己读偏好"的默认行为。
- 系统栏**图标**明暗由 `ZiFitTheme` 内的 `SideEffect` 按 `palette.isDark` 覆写
  (`WindowCompat.getInsetsController(...).isAppearanceLightStatusBars`)。
  `MainActivity` 里 `enableEdgeToEdge` 用的是 `SystemBarStyle.auto(...)`,只负责首帧猜测。
- 主题**不跟随系统深色**:深色是用户在设置里显式选的档位(四档里带"深"的两个之一)。

约定:

- 页面内的玻璃面板**不做模糊**(`GlassSurface`):背后只有平滑光斑,模糊无视觉收益、纯增开销。
  真正的模糊只发生在悬浮层上,独立成节 ↓

#### 实时背景模糊(2026-09-30 二改定稿)

只给"内容会从它下面滚过去"的悬浮层做:**底部 dock**、**动作库 / 食材库的悬浮筛选头**
(食材库与动作库同款,二者必须同时改,否则切标签就看出两套观感)。

机制 = **录制一层 → 悬浮层采样并按自身位置平移、裁剪**。有两个录制器,各有适用面:

| 录制器 | 谁用 | 录制范围 | 建在哪 |
|---|---|---|---|
| 根录制器 | 底部 dock | 整个"背景 + 页面内容" | `Navigation.kt` 里 `rememberBackdrop()`,根 `drawWithContent` 内**先录后画** |
| 页内录制器 | 页内悬浮筛选头 | 只录悬浮头**下方那块内容** | 页面里 `rememberPageBackdrop()` + `GlassBackdropSource{…}` |

为什么页内要单开一个、不能共用根那个 —— **成环会崩**,见铁律 2。

三条铁律。违反任意一条的表现都不是"效果差一点",而是**崩溃**或**完全没效果**:

1. **内容必须走 `GraphicsLayer.record(...)`,不要手写 `RenderNode` 录制。**
   官方入口是 `GraphicsLayer.record(owner, layoutDirection, size) { owner.drawContent() }`。
   自己 `beginRecording()` + 偷换 `drawContext.canvas` 是死路:`drawIntoCanvas{}` 会读那个 canvas,
   所以自绘探针"看着成了",但 `drawContent()` **不走** `drawContext.canvas` ——
   真内容一个像素都录不进去,表现为"位置、白纱、几何全对,背后内容依旧清晰"(像没开模糊)。
2. **绝对不允许成环。** 录制本质是"指向子节点的引用";HWUI 在 `prepareTree` 时按引用设 `mParent`,
   互指即无限递归 → **RenderThread 栈溢出、进程当场被杀**
   (tombstone: `Cause: stack pointer is not in a rw map; likely due to stack overflow`,
   512 帧全是 `hwui::computeTransformImpl`)。环的典型长法:
   `根录制层 → NavDisplay 图形层 N → 页内悬浮头 → 根录制层`。所以:
   - 页内悬浮头用**页内录制器**,让录制范围落到 N **之下**;
   - 悬浮层**不得带 `Modifier.graphicsLayer`(含 `clipToBounds`)** —— 那会平白多出一个
     RenderNode 去引用录制层,而它自己又在录制层里,又是一个环。遮罩的离屏合成因此改用
     原生 `Canvas.saveLayer`(Skia 的图层栈,**不进** HWUI 的 RenderNode 树);
   - `GlassBackdropSource` 只包**会被压住的内容**,悬浮头必须是它的**兄弟而非子节点**。
   - ⚠️ **"录制遍里让悬浮层跳过自己"不解决这个环**:跳过只是让那一遍画出来是空的,
     `graphicsLayer` 节点自身仍会被录进显示列表,引用照样成立。两件事都要做。
3. **先录后画,顺序不能反。** `record()` → `drawContent()`:悬浮层是在 `drawContent()` 那一遍
   采样录制层的,取的才是**本帧**画面;反过来会慢一帧,快速滑动时能看出模糊"追不上"内容。
   代价是内容每帧画 **2 遍**(录制一遍 + 屏幕一遍)再加一次离屏栅格化 ——
   这是 `RenderEffect` 只能作用于"本层自绘像素"的必然代价,**所以录制器不要随手加**,
   只在真有悬浮层要用时才建。

- **模糊半径是全局唯一值**:`Backdrop.kt` 的 `BackdropBlurRadius`(28dp),由 `Backdrop` 在录制时
  一次性烘进模糊副本;悬浮层**只做平移对齐 + 裁剪采样**。给悬浮层加
  `Modifier.blur` / `graphicsLayer { renderEffect }` 是"什么都没发生",不要再试。
- **模糊层必须隔离在一个 `matchParentSize` 的子节点上**(即 `GlassBlurLayer`)。
  `renderEffect` 作用于施加它的节点**及其整棵子树**;挂在 dock 容器上的话,图标会被一起糊掉
  (实测: dock 里只剩一团白雾、图标消失)。层级顺序固定:模糊层 → 白纱(`DockFill*`)→ 描边 → 图标。
  另:模糊层**自身不能有子内容**,它是"背景",任何子节点都会被自己盖住。
- **录制遍里悬浮层整层让位**(`Backdrop.kt` 的 `isBackdropRecording`)。那一遍画的画面是
  **给悬浮层当背景**用的,把悬浮层自己画进去,白纱会被模糊一遍再叠一遍、"能看见背后色块"就没了。
  它必须是**全局**标志而非"自己那个录制器是否在录":根录制器开始录制时,页内悬浮头用的是
  另一个录制器,只看自己的会照画不误,那一遍还会被烘进根录制层里。
- **关掉毛玻璃 = 连录制一起省掉**(`LocalBackdrop` 传 `null`),悬浮层退回半透明实底兜底 ——
  与 API < 31 走的是同一条路径。
- `BlurEffect` 需 **API 31+**;`Backdrop.kt` 的 `backdropBlurSupported` 已做门槛判断,
  低版本自动退回厚填充兜底,**不得**为此提高 minSdk。
- ⚠️ `backdropBlurSupported` 上的注解必须写成 **`@get:ChecksSdkIntAtLeast`**。只写
  `@ChecksSdkIntAtLeast` 的话 Kotlin 会把它落到**字段**上,lint 读不到 getter,守卫白写
  (实测连报 4 个 NewApi)。另:lint 推不出 `LocalBackdrop.current != null` 的语义,
  调用处(如 `GlassBackdropSource` / `Navigation.kt`)要**显式**再带上 `&& backdropBlurSupported`。
- dock **只有图标,没有文字**:`TopLevelDestination.labelRes` 仅作无障碍描述
  (`contentDescription`),测试也用 `onNodeWithContentDescription` 定位。
- **不使用 `Modifier.blur`** —— 它需 API 31+ 且只能糊自己;**背景光斑**靠径向渐变收边即可柔化
  (悬浮层的模糊走录制器,见上)。
- 玻璃面板**不得**用 `Card` / `Surface` 替代 —— 那些组件会带上 Material 的色调叠加与阴影语义。
- 手写 dock 而非用 `NavigationBar`:后者有 80dp 最小高度与标签强制布局,做不出紧凑的纯图标胶囊。
- dock 项**必须关掉水波纹**:`selectable(..., interactionSource = null, indication = null, ...)`。
  Material 默认 ripple 会在这个手写胶囊里渲染成**灰色方块**(节点无 `shape`,涟漪是矩形),
  点按后一闪即散,是最显眼的廉价感来源。
- dock 选中态 = **图标微放大 + 转主题色**(`Accent`,`DockSelectedScale`),
  **不铺背景胶囊**。放大用 `graphicsLayer { scaleX/scaleY }` 而非改 `Modifier.size` ——
  改 size 会触发重新布局、推挤相邻图标;缩放只影响绘制,其余项纹丝不动。
  颜色与缩放均走 `animateAsState` 过渡,避免生硬跳变。(曾有一档「界面动效」开关
  可关掉过渡,2026-09-30 按要求**已删除** —— 过渡固定开启。)
- **悬浮元素(dock / 悬浮筛选头)的内容避让只能用 `contentPadding`,绝不能用 `padding`。**
  `padding` 会缩小可滚动容器,内容被**硬截**在悬浮元素之上、再也滚不进去 ——
  那截空出来的不透明背景就是用户眼中的「遮挡层」,毛玻璃也无内容可"透"。
  `contentPadding` 则允许内容滚进留白区被悬浮元素覆盖,最后一条也仍能滚出来看清。
  尺寸与 `dockContentInset()` 见 `ui/common/DockMetrics.kt`(**dock 尺寸的唯一来源**);
  `NavDisplay` 层级**刻意不做**底部避让,避让由各可滚动页面自己完成。
  对 `verticalScroll` 的 `Column`,`padding(bottom=…)` 必须写在 `verticalScroll` **之后**。
- **悬浮元素自身不加投影。** 内容会从它们下面穿过,任何投影都会在内容上压出一条暗带。
  `GlassDock` 无投影;`GlassSurface` 的 `elevated` 形参供悬浮件传 `false`。
- **悬浮筛选头 = 模糊层 + 白纱,两者叠加而非二选一。** 全透明时列表行会从筛选胶囊的缝隙里
  原样穿出、字压字看不清;实心则又变回一块遮挡。落地:
  `GlassBlurLayer(fadeMask = HeaderFadeMask, overlay = headerScrimBrush())` ——
  模糊把背后内容化成色块,白纱再把整体压淡一档,胶囊上的文字才压得住;
  两者底缘一起淡出,头部与列表之间才没有硬边。
  - ⚠️ **白纱与模糊遮罩必须共用同一组 stop**(`ScrimSolidEnd` = 顶部 78% 满强度、末 22% 淡出)。
    各写一套迟早错位,表现是底缘出现一条"模糊已断、白纱还在"(或反过来)的带子,像渲染 bug。
  - 白纱取色**随主题变**:浅色下是四分之三白,深色下是近实心深色 —— 深底上叠白纱会让整块发灰。
  - 白纱 α 有**两档**,见下面「调通透度」那条(`headerScrim` / `headerScrimSolid`)。
- **悬浮层必须吞掉落在其上的点击。** 头/ dock 压在列表之上,但它们只是"画"在上面:
  不显式拦截,点在它们的空白处(图标旁留白、胶囊两端的内缩、行间空隙)会**穿透到下方的列表行**,
  用户点一下浮层就误进详情页。做法:`Modifier.pointerInput(Unit) { detectTapGestures(onTap = {}) }`,
  且**必须排在 `padding` 之前**(与 `onSizeChanged` 同理:修饰符链上指针命中区就是该位置节点的尺寸,
  排在 padding 之后只能盖住内容区、盖不住内边距)。只吞点击、不吞拖拽 ——
  子节点(输入框、胶囊)在命中链更内层,始终优先拿到事件,所以不会误伤它们。
- **整块可点的手写输入控件:把 `clickable` 加在最外层,再加 `FocusRequester`。**
  只放一个 `BasicTextField` 时,只有它自己那一条窄缝能点中,图标与内边距都是死区。
  做法:面板最外层 `clickable { focusRequester.requestFocus(); keyboard?.show() }`,
  输入框挂 `focusRequester` —— 有子节点先消费,不必担心两者打架;
  只调 `requestFocus()` 在部分机型上不弹输入法,`show()` 要一起调。
- **占位提示画在 `BasicTextField` 的 `decorationBox` 里,并用 `matchParentSize()` 让它不参与测量。**
  绝不要把提示 `Text` 与输入框**上下堆叠**(`Column { Text(提示); BasicTextField() }`):
  那样空态是两行、输入态一行,搜索框会随"是否输入"变高变矮 —— 用户会直接报
  "输入文字和没输入文字的高度不一致"。约定:**高度一律以输入态为准**。
  `matchParentSize()` 只受父 `Box` 已定尺寸约束、不上报自身尺寸,是唯一稳的写法
  (提示与输入框字体上下留白略有差异,靠"两者行高一样"是赌运气)。
  验证方式:比较**输入框节点自身**的 `bounds`(uiautomator dump),空态与输入态
  应当**逐像素相同**(实测 `[191,149][1125,307]`,h 与顶边差均为 0);顺带核对
  胶囊行与列表首行的 y 也不动。
- **调透明度时先确认方向。** 用户说「透明度小一半」= **更不透**(不透明度向 1 靠拢一半,
  `α_new = α + (1 - α) / 2`),不是更透。本项目实际发生过一次改反。
  验证只能用像素:取不含图标/文字的纯填充带,量区域均值,并用
  `合成 = 底色 × (1-α) + 255 × α` 反算比对(实测与公式吻合到 ±1);
  **不要用 md5 比截图** —— 滚动惯性未停、输入法弹出都会让整图变化,得到假阴性。
- **悬浮层的"底衬"必须铺满屏幕,且与内容内缩分成两层写在同一 Modifier 链上。**
  顺序固定为 `fillMaxWidth → background(底衬) → 安全区内缩 → 内容 16dp 内缩`:
  `background` 画在最外层才会随内缩一道长大,左右与顶部都到屏幕边;**反过来先套外层边距**,
  底衬就比屏幕各少 16dp、顶少一个状态栏,屏幕边缘露出可见直边 —— 用户会读成"贴上去一块",
  反复要求"把遮罩层拉满"。动作库给 `LazyColumn` 的左右 16dp 走 `contentPadding`,
  左右安全区走容器上的 `windowInsetsPadding`(二者叠加)。
- **量悬浮层高度时 `onSizeChanged` 必须放在所有 padding 之前。** 它上报的是**其右侧**
  全部修饰符撑出来的尺寸;放在 `windowInsetsPadding` 之后就会少算一个状态栏,
  列表少让位、首行被压在搜索框下。
- **页面外框(安全区 + 16dp 边距)由各 entry 自己附带**(`Navigation.kt` 的 `screenFrame`),
  不在 `NavDisplay` 上统一套。统一套等于给所有页面一副模具,需要"底衬铺满整屏"的页面
  (动作库)就永远铺不满 —— 它刻意传 `Modifier`,自己按上一条分层。
  新增一级页面时记得给它的 entry 带上 `screenFrame`,否则内容会顶到状态栏与屏幕边。
- 动作库是**无标题页**(顶部标题与条数统计刻意去掉):屏幕空间优先给内容本身。
- 新增任何浮层(对话框、底部弹窗、二级页面容器)一律走 `GlassSurface`,不要在调用处手写填充与描边。
- **主题色 (`Accent`) 决定全站的高亮语义**,换一个色号这些地方一起变:
  底部栏选中图标、动作库筛选胶囊选中(`FilterPill`)、食材库热量数字与「新建」、
  输入框光标、开关轨道、`HintText(emphasized = true)`、「我的」页设置图标。
  新增高亮处**一律用它**,不要另写蓝色。
  - 色号都取**中间调**:它同时当"前景文字"和"选中填充"(胶囊选中时是背景、白字压在上面),
    往两端跑总有一个角色会塌。**不为深色模式另配一套色号** —— 那会让取色板上显示的颜色
    与实际用到的颜色不一致。
  - 设置页四个行首图标里只有「个人资料」跟随主题色(它本就是主色位),
    其余三个(`IconViolet` / `IconCoral` / `IconSky`)是**固定色相**,避免选到某色号时图标撞成一片。
- 调通透度**分三套**,不要混用:页面卡片改 palette 的 `glassFill*`,
  底部 dock 改 `dockFill*`;悬浮头的柔化层改 `headerScrim*`。**四套调色板都要改**。
  当前取值(彩色模式):dock `0xD6` / `0xAC`,柔化层 `headerScrim` `0x99`(2026-09-30 二改,由 `0xC0` 降下来);
  深色模式的对应值是"低透明度白"(玻璃在深底上只能靠提亮表达),柔化层是 `0xA6131922`。
  ⚠️ **柔化层的 α 不是独立的观感参数,它取决于这一层下面有没有模糊**:
  没有模糊时它是唯一的遮挡手段,必须够实(否则胶囊缝隙里原样穿出列表文字、字压字);
  有了模糊,内容已成色块,它就只剩"把整体亮度拉到能压住文字"一件事,
  再糊到 75% 会让人**看不出背后有东西** —— 模糊白做。
  所以柔化层**存两档**:`headerScrim`(有模糊,较透)/ `headerScrimSolid`(无模糊,较厚 ——
  彩色·浅是 `0xC0`,正好就是原来那一档),由 `headerScrimBrush()` 按 `LocalBackdrop != null`
  自动选。**改一档别忘了另一档**。
  **背景色斑不要调淡**,否则玻璃会退化成一块看不清边界的白色矩形;
  中性那两套(浅色 / 深色)要"退彩"改的是光斑**色相**(统一成同一个钢灰)与 `blobAlphaScale`
  (整体系数),而不是逐团改色。⚠️ 深底上光斑必须**比底亮**才看得见,浅底上则是**比底暗** ——
  同一个钢灰值不能两套共用:浅色那套的 `0x8E9BAE` 压到深底上会亮得像贴了块补丁。
  一条实测经验:在浅色列表上,填充 α 从 0.34 提到 0.84 只让 dock 区域均值 +11 亮度 ——
  **底色本身已经很亮时,加白的边际观感很小**;差异只在背后有深色插画/文字时才明显。
  所以"看起来没变"不等于"没生效",要量像素(见上一条),别靠肉眼改回来。
- 系统栏图标明暗**由主题决定**(深色主题 → 浅色图标),实现见 `ZiFitTheme` 的 `SideEffect`;
  `MainActivity` 的 `SystemBarStyle.auto(...)` 只是首帧猜测,不要去那里钉死 `light`。
- **图标**:一律 24dp、单色填充 `#FF000000`(实际着色由 `Icon` 的 tint 决定),
  且内容须占视口 **85–90%** —— Material 图标在 24 视口内只占约 83%,
  移植满幅 SVG(如 16/16)时必须扩大 viewport 并用 `<group android:translateX/Y>`
  居中平移,否则会与相邻标签图标大小不一;斜向构图取偏大比例。
  移植来的 SVG 先**离线渲染**确认形状(逐 `<path>` 独立非零填充,与 Android 的填充语义一致;
  用一台本机脚本即可,不必入仓库)再落地;
  多条 `<path>` **不可合并**(合并会改变填充语义,形状走样)。
- **Material Symbols 的 24px SVG 其 `viewBox` 为 `0 -960 960 960`(y 轴为负)** ——
  转 VectorDrawable 时须 `viewportWidth/Height="960"` 并加 `<group android:translateY="960">`
  把图形移入 Android 的左上原点坐标系,否则整幅会被画到视口之外。

---

### 5.4 第三方数据与许可(动作库数据源)

动作库数据不自建,来自 **RepDB 免费层数据集**(601 条动作 + 1056 张 512×512 WebP 插画)。

| 项 | 说明 |
|---|---|
| 获取 | `bash tools/fetch_repdb.sh`(幂等;`--force` 重拉)。落到 `app/src/main/assets/repdb/` |
| 快照 | `assets/repdb/SNAPSHOT.txt` 记录拉取时间、条数与图片数;脚本会校验 601 / ≥1000,不符即报错 |
| 许可 | RepDB Free Tier License v1.0,原文见 `assets/repdb/LICENSE-DATA.md` |
| 署名 | **必需且可见**:`Exercise data by RepDB (repdb.co)` —— 已落在「我的 → 关于」 |

许可条款对本项目的硬性影响:

| 条款 | 要求 | 本项目如何满足 |
|---|---|---|
| Term 1 | 允许个人/商业**应用内**使用 | 数据随 APK 分发 |
| Term 2 | 必须可见署名 | 关于页卡片(与 GPL §5(d) 声明同处) |
| Term 3 | **禁止**作为数据集再分发 | 数据目录在 `.gitignore`(见 C11);译文同样不入库 |
| Term 4 | 图片可缩放/裁剪/改色用于应用内 | 未做图像处理 |
| Term 5 | **禁止**图片作生成式模型输入 | 见 C12。将来做多模态识图功能时,不得把这些插画喂给模型 |
| Term 6 | `premium-samples/` 仅评估用 | fetch 脚本**只拉** `exercises.json` 与 `images/flat/`,不含该目录 |
| Term 7 | 无担保、非医疗建议 | 关于页健康免责声明 |

**分区约定**(哪边入库、哪边不入库):

- `data/repdb/**`(代码)→ **入库**。其中 `ExerciseTaxonomy` 译的是**通用解剖/训练术语**
  (胸大肌、壶铃、复合动作),不是 RepDB 的创作性文本,可随源码走。
- `assets/repdb/**`(原始数据 + `zh/` 译文覆盖层)→ **一律不入库**。译文属派生内容,受 Term 3 同等约束。

**中文覆盖层**(`assets/repdb/zh/`,可分批产出、随时补齐):

| 文件 | 结构 | 缺失时行为 |
|---|---|---|
| `names.json` | `{ "动作id": "中文名" }` | 回退英文名 |
| `details.json` | `{ "动作id": { "description": "", "instructions": [], "tips": [] } }` | 逐字段回退英文原文 |

回退是**逐字段**的:只译了名字不影响要领照常显示英文原文,不会整条失效。
`Exercise.isTranslated` 标记长文本是否已译,详情页据此提示「暂为英文原文」。

**筛选分类**(`data/repdb/ExerciseCategory.kt`):

- 筛选栏的**唯一词表来源**。不要拿 `Exercise.bodyPart` 直接当筛选项 —— 数据集把腿拆成
  `upper_legs` / `lower_legs`、臂拆成 `upper_arms` / `lower_arms`(共 9 类),颗粒度不合使用直觉;
  `ExerciseCategory` 按需**合并**(腿部 / 手臂)与**补全**(臀部来自主要发力肌、有氧来自 `category`),
  顺序 = 枚举声明顺序 = 展示顺序(`sort()` 依赖这一点)。
- 分类在**数据层**算好写进 `Exercise.categories`:判定要读原始枚举 key 与肌群 key,
  而 key 到 UI 层就只剩中文标签了。`bodyPart` 仍用于列表与详情展示,**两者不可互换**。
- 只展示**数据集里真有动作的**分类(`ViewModel` 里 `ExerciseCategory.sort(distinct)`),
  空分类的胶囊点进去是空列表,不如不显示。
- 参考词表里的 **颈部 / 拳击·格斗 / 腹部** 三类**刻意未设**:前两者数据集中零支撑,
  后者与「核心」重合 92%(78 条 vs 70 条)。这是**决策留痕**,`ExerciseTaxonomyTest`
  有对应断言;数据集升级后补一个枚举值 + 一条 `when` 分支即可,UI 与仓库层无需改动。

**列表排序**(`ExerciseTaxonomy.difficultyRank` + `ExerciseLibraryViewModel`):

- 动作库列表按**难度由易到难**排:入门 → 进阶 → 高阶,覆盖「全部」与各分类筛选后的结果。
- 权重取 `difficultyLabels` 的**声明顺序**(值越小越靠前),**不要对中文标签直接 `sorted()`**:
  那是 Unicode 码位序,当前这组词恰好与难度序一致纯属巧合,换一组(新手/熟练/专家)立刻乱。
  词表外的未知值排在最后,不能丢。
- 排序用 `sortedBy`(**稳定**),同档内保持数据集原序(数据集本身按英文名 A→Z),
  所以三段各自仍是字母序,结果可预期、也不会把同肌群动作打散。
- 排序在 **ViewModel**(展示层选择),不改数据集与仓库的原始顺序。

> 数据集升级只需重跑 fetch 脚本;若新增枚举值,`ExerciseTaxonomy` 的 `lookup` 会降级为
> 「下划线换空格」的可读英文而不是崩掉 —— 但**记得补译**(见 `ExerciseTaxonomyTest`)。

### 5.5 食材库与 AI 接入

**食材库**(`data/food/`,Room 库 `zifit.db`):

- 表 `food` 字段:`id` / `name` / `energyKcal` / `proteinG` / `fatG` / `carbohydrateG`
  / `origin` / `createdAt`。v2 起**只有这四个营养值** —— v1 的 `ediblePercent`(食部)与
  `sodiumMg`(钠)已移除:二者只服务于「市售重量→可食部」折算与钠摄入统计,配餐用不到,
  留着等于每条数据多录两栏、收益为零。
- 全部营养值**可空**,且**缺测 = `null` ≠ `0`**;展示统一走 `MISSING_VALUE = "—"`,
  **不要**写 `?: "0"` —— 那会让「这份食材不含脂肪」凭空成立。
- 版本升级**必须显式写 `Migration`**,不得用 `fallbackToDestructiveMigration()`(会静默清库)。
  改列走**重表法**(CREATE 新表 → `INSERT … SELECT` → DROP 旧表 → RENAME):
  `ALTER TABLE … DROP COLUMN` 要 SQLite **3.35+**,minSdk 26 自带的是 3.18,旧机上直接报语法错。
- 已开 `exportSchema = true` + `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`,
  `app/schemas/` **入库**。⚠️ v1 的 schema 当时未导出,故 `1 → 2` 这条迁移**没有 schema 可比对**,
  靠**装着 v1 库的真机升级安装**来验(库里的用户数据还在 = 迁移生效,抛异常 = 写错了)。
- `FoodSearchSource` 是**下一轮 AI 查询的唯一接缝**:`Unavailable`(没接上)与
  `Hit(emptyList())`(查过确实没有)必须分开,合成一个空列表就是对用户说谎。
- 数值半进位一律 `BigDecimal.valueOf(x).setScale(n, HALF_UP)`;`round(x*10)/10` 在 `6.85` 上会错成 `6.8`。

**AI 接入**(`data/ai/` + `ui/profile/ProfileViewModel` + `ui/profile/AiConfigDialog`):

- 配置就是三项:**服务地址 / API Key / 模型**。校验走纯函数 `AiConfigDraft.toConfig()`
  (在 data 层、可单测):三栏都不得为空,地址必须以 `http://` 或 `https://` 开头
  (漏写协议头是最常见错法);`http://` **刻意放行** —— 本地推理服务就是 http。
- 存储:`SharedPreferences("ai_config")`,**明文**(`androidx.security.crypto` 已停止维护,
  不引黑盒)。硬性约束:**密钥不进日志、不进 Git、界面上不出现完整明文**
  (配置卡片只显示 `maskApiKey` 的掩码,输入框走 `PasswordVisualTransformation`)。
- 端点拼装唯一入口是 `AiConfig.endpoint(path)`,别在别处再拼一遍 URL。
- 连通性测试 `AiConnectionTester` 只打 `GET {baseUrl}/models` 带 `Bearer` 头 ——
  OpenAI 兼容协议下最轻的鉴权探针,**不烧 token**;404 单独成一档(有些代理不提供 `/models`,
  那不代表密钥错)。失败按 `NetworkErrorKind`(DNS / TIMEOUT / REFUSED / TLS / OTHER)
  翻译成人话,**不要**把 Java 异常原文甩给用户。
- 新增唯一权限 `INTERNET`(其余能力仍走系统本地通知,见 C9)。
- `AiPresets` 只是**填空便利**,不是白名单,值**复核于 2026-09-30**(DeepSeek 官方文档、
  阿里云百炼 OpenAI 兼容文档)。将来反馈「连不上」时,先复核预设里的域名与模型名是否已过期。

**页面外框**(补充 §5.3):`Navigation.kt` 里的 `screenFrame`(安全区 + 16dp 边距)
**只发给需要页内边距的页面**。动作库与食材库刻意**不套** —— 它们由页面自己用
`windowInsetsPadding(safeDrawing.only(Horizontal))` + `contentPadding(16.dp)` 内缩,
柔化头才铺得到屏幕边缘,且两页卡片宽度**逐像素一致**(实测 1224px 屏上均为 `x 53..1171`)。
一旦给某页多套一层 `screenFrame`,该页卡片会窄掉 32px —— 用户一眼就能看出来。

**首屏内的可操作元素必须在 dock 之上**(2026-09-30 真机抓到):悬浮 dock 只是浮层,
不参与布局,所以**页面里排在后面的按钮会被它盖住**。实测 1224×2688 上 dock 上沿在
`y = 2477`,超过它的东西**看得见一半、点不到**(AI 接入页原先把只读的状态卡放最上面,
把「测试连接」的结论与「保存」顶到 y2478 以下 —— 点了按钮屏幕上什么都不变)。
规则:
① 输入框、测试/保存这类**要点的东西排前面**,只读回显(状态卡、说明)排最后;
② 关键反馈(如测试结论)**贴在触发它的按钮旁边**,别另起一行丢在页面底部;
③ 验收方式:`uiautomator dump` 后逐个核对可点元素的 `y2 < 2477`,
"看起来在屏幕里"不算数(这是 lesson 52 那条「先量后改」的同一条原则)。

### 5.6 调试用密钥:`.env`(仅 debug)

每次重装 APK 都要在界面上重填「地址 + Key + 模型」是 AI 相关开发里最没意义的一步。
于是加了一条构建期通道:仓库根放 `.env`,构建时读出来注入 **debug 包**的 `BuildConfig`,
App 首次启动就带着这套配置。

| 文件 | 入库 | 内容 |
|---|---|---|
| `.env` | ❌ **绝不**(见 C13) | `AI_BASE_URL` / `AI_API_KEY` / `AI_MODEL` 三个键,值 = 真密钥 |
| `.env.example` | ✅ | 同名三键 + **占位值**;`cp .env.example .env` 即用 |

流程:`.env` →(配置期读取,`app/build.gradle.kts`)→ `buildConfigField` →
`BuildConfig.AI_ENV_*` → `data/ai/AiEnvDefaults.config` → 仓库实现的默认参数。

规则(改这块之前必读):

- **只有 debug 注入**。release 的 `buildTypes` 里显式声明**同名同类型的空串** ——
  只写在 `debug` 里会让读这些字段的代码在 release 下**编译不过**,而写进 release 又是泄露。
- 优先级:`用户保存的` > `.env` > `无`(见 `resolveAiConfigSource`)。`.env` **绝不覆盖**
  用户在界面上保存过的配置,也不会被写进 `SharedPreferences`(它本来就在 APK 里,抄一份没意义)。
- 用户点过「清除配置」后会在 prefs 里留下 `env_opt_out` 标记 —— 没有这一笔,
  下一次读取又把 `.env` 的值捞回来,那个按钮按了等于没按。
- 界面必须**如实标注来源**:配置卡片有一行 `来源:仓库根 .env(构建时注入)` /
  `本机保存`,且 ENV 档的隐私说明**不写**「保存在本机私有目录」(那句在那一档是假的)。
  AI 接入页里有「载入 .env」胶囊(**只在三项齐全时显示**,否则就是一个点了没反应的按钮)。
- **不要**把带 `.env` 密钥的 debug 包装到别人手机上:密钥在 dex 里,是明文。
  这条与 C13 一起构成红线。
- 验证方式(比看代码可靠):构建时 Gradle 会打一行 `ZiFit: 已从 .env 注入…`;
  真凭据看生成物
  `app/build/generated/source/buildConfig/{debug,release}/com/zifit/app/BuildConfig.java`,
  release 那份的 `AI_ENV_*` 必须全是 `""`;`git check-ignore -v .env` 必须命中。
- 坑:值要 `trim()`(本机 `core.autocrlf=true`,CRLF 会在值尾挂一个 `\r`);
  反斜杠与引号要转义(否则生成的 `BuildConfig.java` 编译不过);
  **单测不要断言 `.env` 的当前内容**(它是本机状态,填了真 key 之后断言就会翻脸)——
  要测的是 `resolveAiConfigSource` 那张**规则**表。

### 5.7 「我的」:页面结构、二级页与个人资料

**页面结构**(2026-09-30 按用户要求定稿):

```
我的(无标题页)  ← 顶部一行:头像 + 用户名 + 右上设置图标(无底色)
└── 设置页   **一张分组卡、四行**:个人资料 / AI 接入 / 用户界面 / 关于(行首彩色图标 + 右侧箭头)
    ├── 个人资料设置页  头像 / 名称 / 生日(日历选择) / 年龄 / 目标;保存键在页头右上
    ├── AI 接入设置页   服务地址 / 模型 / 密钥 + 连通性测试(原弹窗改为整页)
    ├── 用户界面设置页  **两张卡**:① 主题(主题模式 / 主题色) ② 效果(毛玻璃模糊)
    └── 关于页          许可声明、数据出处、健康免责
```

- **首页刻意没有标题**。原先那行「个人中心」是纯噪音 —— 头像与名字本身就说清了这是谁的页面。
  改版时**先问要不要动**已知的用户要求,别自作主张把标题加回去。
- **首页的空副标题整行不渲染**(2026-09-30 第十八轮)。目标与年龄都没填时,
  `profileSubtitle()` 返回 null —— 曾经给过一句「点头像右侧的设置…」的引导语,
  用户直说是废话。同理:**设置图标不画白底**(「孤立的动作不需要圆底来证明能点」),
  `IconAction`(无底,触摸区域仍是 40dp)。
- **个人资料的保存键在页头右上**(`PageHeader` 的 `trailing`),校验错误显示在页头正下方 ——
  底部是 dock 盲区(见 5.5 末尾),而右上角是唯一不随滚动跑掉的位置。**报错必须贴着保存键**。
- **生日是选择不是输入**(第十八轮):`GlassPickerField`(标签 + 值 + 箭头,`ui/common/`)
  + Material3 `DatePicker`。有唯一正确写法的值(ISO 日期)不要交给键盘;
  picker 毫秒 ↔ `LocalDate` 只能走 data 层的 `toPickerMillis` / `pickerMillisToLocalDate`
  (**钉死 UTC**,单测锁死常量 —— 用系统时区转,东八区测不出来、UTC-5 的机器上差一天)。
  「清除生日」在选择器对话框里,且只在已有值时出现。
- **头像卡片没有按钮**(第十八轮):点头像即换,「点击头像更换」「移除头像」都已删。
  代价是头像设了去不掉 —— 这是用户明确的取舍,别擅自把「移除」加回去。
- 四个二级页共用「我的」标签那条返回栈(见 `Navigation.kt`),所以「设置 → 个人资料 → 返回」
  回到设置页,而不是一路弹回首页。新加二级页时:`NavigationKeys.kt` 加 `@Serializable data object`,
  `Navigation.kt` 的 entry 里给 `screenFrame` + `onBack = { stacks[currentIndex.intValue].removeLastOrNull() }`。
- 二级页统一用 `ui/common/PageHeader`(返回图标 + 标题 + 可选尾部动作);内容区自己
  `verticalScroll` + `padding(bottom = dockContentInset())`,**页头在滚动区之外**(不跟着滚走)。
  **PageHeader 是全站页头标题的唯一出处** —— 别再手写「返回按钮 + 标题」那一行
  (动作详情页原先手写过一版,2026-09-30 已并入;标题要两行时传 `titleMaxLines`)。
  - **返回键无底**:`IconAction` 只画图标(触摸区 40dp),不套白圆 —— 用户 2026-09-30
    明确要求"取消背景色",全站二级页一起改。别因为"看不见就以为不能点"再加回圆底。
  - **页头标题字号 = `typography.titleLarge` = 20sp**(定义在 `theme/Type.kt`,同一轮下调)。
    注意 `headlineSmall`(Material 默认 24sp)**没有动**:它现在只用于「我的」首页的用户名,
    那是页面主角不是页头标题,两者**不要合并成同一档**。
  - 角落里的图标动作一律用 `IconAction`,
  「只能选择、不能打字」的字段用 `GlassPickerField`(与 `GlassTextField` 同一框型)。
- **设置页的行是"分组卡",不是"每行一张卡"**(2026-09-30 按用户给的参考图定稿)。
  骨架:`SettingsGroup { NavRow(…); NavRow(…) }` —— `SettingsGroup` 是一个
  `GlassSurface(contentPadding = 0.dp)`,整组共用一张圆角卡;`NavRow` **自己不画底**,
  单拎出来会看不见边界。组间留白 14dp,**组内不画分隔线**(参考图只靠留白分行;
  1dp 实线在毛玻璃半透明底上会出现"线比底还实")。
  - 行首图标是**裸彩色字形**(不套彩色方底),22dp;标题用 `titleMedium`(17sp),
    与参考图的行高/字重同档。**同一组内要么都带图标、要么都不带**,混着放文字左边界会参差。
  - 图标色只做"扫视定位":个人资料 `Accent`(**主题色**,它本就是主色位)、
    AI 接入 `IconViolet`、用户界面 `IconCoral`、关于 `IconSky`(后三个是固定色相)。
    **不要照搬参考图的高饱和彩虹**(橙车/绿铃),与冷色玻璃底不同族、互相打架;
    也**别让四个图标都跟主题色走** —— 选到某色号时它们会撞成一片。
  - 需要**开关**的行用 `SwitchRow`(与 `NavRow` 同一槽位,右侧换成 `Switch`):
    **整行可点、`Switch` 自身不接手势**(`onCheckedChange = null`)—— 两处都能点会让
    "点空白算不算点开关"变成没人能预判的问题。只用于**立刻生效**的开/关项,
    需要确认或要跳页的仍然用 `NavRow`。
  - 需要**三选一**的行用 `ChoiceRow`(标题下方一行 `FilterPill`),
    需要**取色**的行用 `SwatchRow`(标题下方一行色点,选中态是外圈一道环)。
    两者都是"标题在上、控件在下"的两段式,所以图标**顶部对齐**而不是像 `NavRow` 那样居中。
    `SwatchRow` 的每个点必须挂 `contentDescription`(颜色名)—— 色号对读屏毫无意义,
    仪器测试也只能靠它定位配色点(里面没有文字,`onNodeWithText` 找不到)。
  - **当前只有一张卡、四行**(2026-09-30 从"两张卡"并成一张)。原先按"要填的 / 只读的"
    分两组,但四行都是**同一类东西**——点进去改一项设置。分组的意义是"组内同质、
    组间异质",为同类项硬分组只会让人去猜这两条分界到底意味着什么。
    `SettingsGroup` 与 14dp 的组间留白都留着(真出现异质的两类时直接用),别顺手删。
- **「用户界面」这一页管三件事,两类**(2026-09-30 大改):
  - **主题**(配色):主题模式(**四档 2×2**:彩色·浅 / 浅色 / 彩色·深 / 深色)+
    主题色(蓝 / 薄荷 / 紫 / 陶土 / 玫红)。
    选完立刻重建主题 —— 主题在 `MainActivity` 读偏好后传给 `ZiFitTheme`,是纯函数。
  - **效果**(渲染路径):「毛玻璃模糊」= 根节点是否录制整屏图形层
    (关掉时 dock 退回实底兜底,连录制都省掉)。
  - **「界面动效」已删除**(2026-09-30 用户要求):dock 的弹性过渡固定开启。
    想再引入这类开关时注意:它对应的是**代码里已经存在的一条分支**(`snap()` vs `spring()`),
    不是把某个颜色调淡 —— 开关要真的是开关,否则就是个摆设。
  - **主题不跟随系统深色**。深色是用户在设置里显式选的档位;加"跟随系统"要先想清楚
    "用户在 App 内选的"和"系统给的"谁说了算,别默默加一个自动覆盖。
  - **开关行不设"保存"**:这类偏好没有填错的可能,再要一次确认只是多一步。
  - 数据在 `data/ui/`(`UiPreferences` + `UiPreferencesRepository`),
    存 `SharedPreferences("ui_preferences")`(布尔存值、枚举存 `name`,读回来必须容错 ——
    `valueOf` 遇到旧值/坏值会在启动那一刻崩)。⚠️ 这个 Repository 是**进程内单例**,
    与 `UserProfileRepository` 的取舍**刻意不同**:同一份偏好被"设置页写、根导航与主题读"
    多处消费,两个实例各持一份 `StateFlow` 的话,**改了配置当场不生效**、得重启 App。
    照抄 `UserProfileRepository` 每次新建的写法就会踩这个坑。
  - 不支持的设备要**说清楚**:API < 31 没有 `RenderEffect`,开关拨到"开"也不会有效果,
    所以设置页副标题在那种机器上显示「不可用」而不是「开」(`blurStateLabel`)。
  - 主题模式 / 主题色 / 毛玻璃状态的**文案只有一个出口**:`ui/appearance/AppearanceLabels.kt`
    (设置页那一行的副标题与用户界面页的选项标签共用)。同一个枚举在两个页面上被叫成两个名字,
    是"改了一处、另一处还是旧说法"的经典来源。
- **ViewModel 是 Activity 级的**:Navigation 3 的默认宿主就是 Activity,所以同一 Activity 内
  同类型 `viewModel()` 返回**同一个实例**(默认 key 是类名)。设置页、个人资料页、AI 接入页
  因此天然共享状态,不必传参。**要区分实例才传 `key`**(见 `ExerciseDetailScreen` 用 `exerciseId` 做 key)。
  反过来说:一个页面的 ViewModel 里**别放页面私有的临时状态**,那会串到别的页面去。

**个人资料数据**(`data/user/`):

- 存 `SharedPreferences("user_profile")`,理由与 AI 配置相同:五个键值,不值得开表。
  **头像只存路径**,图片本体在私有目录。
- 头像必须**复制进 `filesDir/avatar/`**(`AvatarStore`),不能直接用相册给的 `content://` URI ——
  那个读权限是临时的,进程重启或原图被删就变空白。文件名**带时间戳**:
  Coil 按路径做内存缓存,沿用同名文件会出现「换了头像还是显示旧图」。换头像后删旧文件。
- 选图用 `ActivityResultContracts.PickVisualMedia`:**API 33+ 用系统相册,以下回退
  `ACTION_OPEN_DOCUMENT`,不需要任何权限、不依赖 GMS**(与 C2 相容)。
  ⚠️ **不要**为了头像申请 `READ_MEDIA_IMAGES`。
- 校验全在纯函数 `UserProfileDraft.toProfile(today)` 里(可单测):名称可空、生日要么空
  要么严格 `YYYY-MM-DD`、年龄要么空要么 `0..120`、**生日合法而年龄留空时用生日推算**
  (推算结果要落库,别只活在界面上)。`today` 显式注入,别在断言里写死"今天"。
- `Goal` 是**枚举**不是自由文本(增肌/减脂/保持/增力/健康):这个值要喂给 AI 生成计划,
  「减脂」「减肥」「刷脂」不能是三个东西。存盘存枚举名,读到不认识的值降级为「没选」而不是崩。
- 改这块要同步三处测试:`data/user/UserProfileTest`(纯函数)、
  `ui/profile/UserProfileViewModelTest`(校验不过不落库 / 换头像删旧文件)、
  `androidTest/.../ProfileNavigationTest`(下钻路径)。

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
> `android` CLI 并发争锁;USB 调试需拔插一次数据线才会被 adb 识别(不要误判为「USB 不可用」);`.workbuddy` 必须 gitignore。
> 新踩的坑请**追加**,不要只留在对话里。

### 6.4 完成前验证 (Verification Before Done)

- **绝不在证明它能工作前**标记任务完成。
- 本项目判定"能工作"的最低标准(按改动范围取用):
  1. `./gradlew lint test assembleDebug` 全绿;
  2. `adb install -r` 成功,**App 能启动且不崩溃**;
  3. `adb logcat` 中无 `FATAL EXCEPTION`、无新增 error 级日志;
  4. 涉及 UI 的改动:真机截屏取证,或在 `connectedAndroidTest` 中有对应断言。
  5. **收尾时手机上必须仍装有最新 APK 且能启动**(用户要有实物可点按,见 §3.2 提示)。
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
| 发布 tag | `vX.Y[.Z]`(如 `v1.0`),推上去即触发 Release 构建(§3.5)。tag 名会被解析成版本号,格式不符工作流直接失败 |

提交前自查:

- 已过 §6.4 验证。
- 未提交 `local.properties`、`build/`、`.gradle/`、`.kotlin/`、`.workbuddy/`。
- 未提交 `.env`、`keystore.properties`,以及任何 `.jks` / `.p12` / `.keystore`(C13 / C14)。
- 无密钥 / 令牌,**无本机绝对路径**,无公司邮箱。
- 新增 `.kt` 文件均已带 GPL 头(§5.2)。

---

## 9. 核心原则 (Core Principles)

- **简洁优先**:每次变更尽量简单,只影响最小代码。
- **绝不偷懒**:找到根因,不用临时修复,坚持资深开发者标准。
- **最小影响**:只修改必要部分,避免引入新 bug。
- **约束优先**:§2 的硬约束优先于任何"更省事"的实现方案。
