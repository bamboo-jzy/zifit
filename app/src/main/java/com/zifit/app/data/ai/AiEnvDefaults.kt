/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ai

import com.zifit.app.BuildConfig

/**
 * 调试用的默认 AI 配置 —— 值来自仓库根的 `.env`，由 `app/build.gradle.kts` 在**构建时**
 * 塞进 [BuildConfig]（见 AGENTS.md §5.6）。
 *
 * 为什么要有这个：每次改完代码重装 APK，都要在界面上把服务地址、Key、模型再填一遍，
 * 是整个 AI 相关开发里最没意义的一步。有了它，装完即用。
 *
 * ⚠️ 三条边界，改这里之前先读：
 * 1. **只有 debug 包有值**。release 包的三个字段恒为空串，所以 `isAvailable` 为 false，
 *    侧载发行的 APK 里不含任何密钥。
 * 2. `.env` 在 `.gitignore` 里，**永不入库**（本仓库是 public）；可以提交的是 `.env.example`。
 * 3. 这里拿到的是明文密钥，落在 dex 里。故：`isAvailable` 为 true 的包**只在自己手机上装**，
 *    不要转发给别人，也不要放进 CI 产物。
 *
 * 另：Debug 与 Release 都是编译期常量，Kotlin 会把它们内联，运行期没有额外开销。
 */
object AiEnvDefaults {

  /** `.env` 三项的当前值；没填或 release 包里，是三项全空的 [AiConfig]。 */
  val config: AiConfig =
    AiConfig(
      baseUrl = BuildConfig.AI_ENV_BASE_URL,
      apiKey = BuildConfig.AI_ENV_API_KEY,
      model = BuildConfig.AI_ENV_MODEL,
    )

  /** `.env` 三项齐全 —— 界面据此决定要不要显示「载入 .env」。 */
  val isAvailable: Boolean
    get() = config.isConfigured
}

/**
 * 决定「当前生效的配置从哪来」。
 *
 * 优先级：**用户自己保存的 > `.env` 的调试默认值 > 没有**。
 * 用户保存过的配置永远优先 —— `.env` 是省打字用的，不该反过来覆盖用户在界面上填的东西。
 *
 * @param savedIsConfigured 本地是否存着一份三项齐全的配置
 * @param envOptOut 用户是否**主动**点过「清除配置」。没有这一条的话，`clear()` 之后下一次
 *   读取又会把 `.env` 的值灌回来，「清除配置」就成了一个按了没反应的按钮。
 * @param envAvailable `.env` 三项是否齐全
 */
fun resolveAiConfigSource(
  savedIsConfigured: Boolean,
  envOptOut: Boolean,
  envAvailable: Boolean,
): AiConfigSource =
  when {
    savedIsConfigured -> AiConfigSource.SAVED
    envAvailable && !envOptOut -> AiConfigSource.ENV
    else -> AiConfigSource.NONE
  }
