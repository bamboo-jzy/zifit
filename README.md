# ZiFit

> 一个由 AI 驱动的个人健身管理 Android 应用。

ZiFit 以三个数据底座为输入 —— **健身动作库**、**食材库**、**身体数据管理** ——
自动生成**训练计划**与**饮食计划**，并且两份计划都可以手动调整。

## 功能规划

| 模块 | 说明 |
|---|---|
| 健身动作库 | 动作条目、目标肌群、动作要领与替代动作 |
| 食材库 | 食材营养数据，供饮食计划计算使用 |
| 身体数据 | 体重、体脂、围度等随时间的记录与趋势 |
| 训练计划 | 依据身体数据与训练目标自动生成，支持手动修改 |
| 饮食计划 | 依据训练安排与营养目标自动生成，支持手动修改 |
| AI 驱动 | 计划由 AI 生成，而非套用固定模板 |

## 技术栈

| 项 | 值 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose |
| 构建 | Gradle + Android Gradle Plugin |
| 最低版本 | Android 8.0（API 26） |
| 目标版本 | Android 16（API 36） |
| 包名 | `com.zifit.app` |

## 下载与安装

到 [Releases](https://github.com/bamboo-jzy/zifit/releases) 下载最新的 `ZiFit-x.y.apk`，
在手机上允许「安装未知来源应用」后直接安装。同签名的新版本可以直接覆盖升级。

- 最低支持 **Android 8.0（API 26）**，目标 **Android 16（API 36）**，
  含 `arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64` 四种 ABI。
- 动作库数据**随 APK 携带**，离线可用 —— 不联网、不需要谷歌服务（GMS）。
- 发布包**不含任何密钥**。首次使用请自行填写「AI 服务地址 + 密钥 + 模型」
  （「我的 → AI 接入」，内置 DeepSeek / 阿里云百炼等预设）。
- 每个 Release 附有 `.apk.sha256`，可自行校验。

## 构建

项目采用命令行构建，不需要 Android Studio。依赖 JDK 21 与 Android SDK
（platform 36 与 build-tools 36.0.0）。

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk

cd zifit
./gradlew assembleDebug     # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # 连接真机后直接安装
./gradlew assembleRelease   # 正式包（需先配好签名，见「发版」）
```

## 本地开发：调试用密钥

App 需要一份「AI 服务地址 + 密钥 + 模型」才能用 AI 相关功能，可以在界面上手填
（「我的 → AI 接入」）。开发时不想每次重装都重填，就用仓库根的 `.env`：

```bash
cp .env.example .env    # 然后填上自己的值
./gradlew assembleDebug
```

构建时会把这三项注入 **debug 包**的 `BuildConfig`，装上即生效；`release` 包不注入。

> ⚠️ `.env` 已被 `.gitignore` 忽略，**不要提交** —— 里面是明文密钥，提交即泄露，
> 且会永久留在 Git 历史里。可以提交的只有 `.env.example`（里面只能有占位值）。

## 发版（维护者）

发版由 GitHub Actions 自动完成，**打 tag 即发布**：

```bash
git tag v1.0
git push origin v1.0
```

推送后 `.github/workflows/release.yml` 会从 tag 推导 `versionName` / `versionCode`
（`v1.2.3` → `1.2.3` / 10203），拉取动作库数据集，用 Secrets 里的密钥库签名，
校验产物确实已签名，然后创建 Release 并附上 APK 与 SHA256。产物名形如 `ZiFit-1.0.apk`。

维护者需一次性在仓库配置四个 Secrets
（**Settings → Secrets and variables → Actions**）：

| Secret | 内容 |
|---|---|
| `ZIFIT_KEYSTORE_BASE64` | 签名密钥库文件的 base64：`base64 -w0 zifit-release.p12` |
| `ZIFIT_STORE_PASSWORD` | 密钥库口令 |
| `ZIFIT_KEY_ALIAS` | 密钥别名 |
| `ZIFIT_KEY_PASSWORD` | 密钥口令 |

本机构建 release 时不用 Secrets，改成在仓库根放 `keystore.properties`
（**已被 `.gitignore` 忽略**）：

```properties
storeFile=/absolute/path/to/zifit-release.p12
storePassword=…
keyAlias=…
keyPassword=…
```

`app/build.gradle.kts` **环境变量优先、该文件兜底**，两边键名一一对应。
签名四项没配齐时不会偷偷用 debug 签名顶上，而是产出装不上的
`app-release-unsigned.apk` —— 让问题在打包阶段就暴露。

> ⚠️ 密钥库与口令**绝不入库**。密钥库丢失后无法再发布同签名的更新包，
> 已经装了的人只能卸载重装（数据全丢）。请离线备份密钥库**和**口令。

## 设计约束

目标机型为**国内荣耀手机（MagicOS，不带 GMS）**，因此：

- 不引入任何 Google 服务依赖（`play-services-*`、Firebase/FCM、Google Maps 等）
- 不引入闭源第三方 SDK；提醒类功能使用系统本地通知（`AlarmManager` / `WorkManager`）
- 当前全部依赖均为 Apache-2.0，无许可证冲突

## 许可证

本项目采用 **GNU General Public License v3.0** 许可，全文见 [LICENSE](LICENSE)。

```
Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

### 由此产生的项目义务

GPL-3.0 是 copyleft 许可 —— 它管的是**分发**，不管你自己怎么用。本项目的落实情况：

| 义务 | 条款 | 落实方式 |
|---|---|---|
| 分发二进制须提供完整对应源码 | §6 | 本仓库公开即为源码 |
| 衍生版本必须沿用同一许可 | §5(c) | 由 GPL-3.0 自动继承，无需额外动作 |
| 修改须显著标注 | §5(a) | Git 提交历史天然满足 |
| 带交互界面的作品须展示法律声明 | §5(d) / §0 | 已实现 —— App「我的 → 关于」展示版权行、无担保声明与许可证获取方式 |
| 不得引入闭源 SDK | §5(c) / §10 | 已列入上方设计约束 |

两点需要说清：

- **仅在自有设备上使用、不向他人分发时，上述义务不触发**；一旦把 APK 发给他人或提供下载，即全部生效。
- **你是唯一版权人**，因此仍可自行对其他方另行授权（双许可）。GPL-3.0 约束的是收到代码的第三方，不是你本人。
