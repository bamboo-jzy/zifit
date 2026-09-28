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

## 构建

项目采用命令行构建，不需要 Android Studio。依赖 JDK 21 与 Android SDK
（platform 36 与 build-tools 36.0.0）。

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/Android/Sdk

cd zifit
./gradlew assembleDebug     # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # 连接真机后直接安装
```

## 设计约束

目标机型为**国内荣耀手机（MagicOS，不带 GMS）**，因此：

- 不引入任何 Google 服务依赖（`play-services-*`、Firebase/FCM、Google Maps 等）
- 不引入闭源第三方 SDK；提醒类功能使用系统本地通知（`AlarmManager` / `WorkManager`）
- 当前全部依赖均为 Apache-2.0，无许可证冲突

## 许可证

本项目采用 **GNU General Public License v3.0** 许可，全文见 [LICENSE](LICENSE)。

```
Copyright (C) 2026 bamboomail_j@163.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```
