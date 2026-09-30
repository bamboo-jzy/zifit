/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI 配置的**唯一存取出口**（AGENTS.md §4 分层约束）。
 *
 * 做成接口而不是只有一个具体类：真实实现依赖 `SharedPreferences`，
 * 在 JVM 单测里拿不到（要 Robolectric）。接口化之后 `ProfileViewModel` 可以喂内存版，
 * 「改了输入要作废上一次测试结论」这类**逻辑**才测得到。
 */
interface AiConfigRepository {

  /** 当前配置。配置界面与卡片的唯一数据源。 */
  val config: StateFlow<AiConfig>

  /** 当前配置**来自哪里**（自己保存的 / `.env` 的 / 没有）。卡片据此如实标注。 */
  val source: StateFlow<AiConfigSource>

  fun save(config: AiConfig)

  /** 清空配置（等价于「清除接入」）。 */
  fun clear()

  companion object {
    fun get(context: Context): AiConfigRepository =
      SharedPrefsAiConfigRepository(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      )

    private const val PREFS_NAME = "ai_config"
  }
}

/**
 * 用 `SharedPreferences` 而不是 Room：这里是三项键值，不是表。
 * 为三项配置单开一张表要多一套 DAO / 迁移，收益为零；DataStore 又要多一个依赖。
 *
 * ⚠️ 写到这里的 [AiConfig.apiKey] 是**明文**，落在应用私有目录
 * （`/data/data/com.zifit.app/shared_prefs/ai_config.xml`）。理由与取舍见 [AiConfig] 的注释。
 * 这里**不打印、不上报**任何配置内容，日志里也不出现。
 *
 * `.env` 里那套调试默认值**不写进 prefs** —— 它本来就在 APK 里，再抄一份没有意义，
 * 反而让「用户自己填的值」和「构建时注入的值」在同一处混淆。读取时按
 * [resolveAiConfigSource] 的优先级现算，见 [read]。
 */
class SharedPrefsAiConfigRepository(
  private val prefs: SharedPreferences,
  /**
   * 调试默认值（来自 `.env`）。默认参数读 `BuildConfig`，release 包恒为全空配置。
   * 做成参数是为了让「`.env` 生效 / 不生效」这两条路径都能被单测直接喂进来。
   */
  private val envDefaults: AiConfig = AiEnvDefaults.config,
) : AiConfigRepository {

  private val initial = read()

  private val state = MutableStateFlow(initial.first)

  private val sourceState = MutableStateFlow(initial.second)

  override val config: StateFlow<AiConfig> = state.asStateFlow()

  override val source: StateFlow<AiConfigSource> = sourceState.asStateFlow()

  override fun save(config: AiConfig) {
    prefs.edit {
      putString(KEY_BASE_URL, config.baseUrl)
      putString(KEY_API_KEY, config.apiKey)
      putString(KEY_MODEL, config.model)
    }
    publish()
  }

  override fun clear() {
    prefs.edit {
      remove(KEY_BASE_URL)
      remove(KEY_API_KEY)
      remove(KEY_MODEL)
      // 记住「用户是主动清掉的」。没这一笔的话，下一次 read() 又会把 .env 的值捞回来，
      // 「清除配置」按了就等于没按。
      putBoolean(KEY_ENV_OPT_OUT, true)
    }
    publish()
  }

  private fun publish() {
    val (config, source) = read()
    state.value = config
    sourceState.value = source
  }

  /** 现算配置与其来源。优先级见 [resolveAiConfigSource]。 */
  private fun read(): Pair<AiConfig, AiConfigSource> {
    val saved =
      AiConfig(
        baseUrl = prefs.getString(KEY_BASE_URL, null).orEmpty(),
        apiKey = prefs.getString(KEY_API_KEY, null).orEmpty(),
        model = prefs.getString(KEY_MODEL, null).orEmpty(),
      )
    val source =
      resolveAiConfigSource(
        savedIsConfigured = saved.isConfigured,
        envOptOut = prefs.getBoolean(KEY_ENV_OPT_OUT, false),
        envAvailable = envDefaults.isConfigured,
      )
    // NONE 档把（可能是半截的）saved 原样交出去，别把用户填过的东西吞掉
    return when (source) {
      AiConfigSource.SAVED -> saved to source
      AiConfigSource.ENV -> envDefaults to source
      AiConfigSource.NONE -> saved to source
    }
  }

  private companion object {
    const val KEY_BASE_URL = "base_url"
    const val KEY_API_KEY = "api_key"
    const val KEY_MODEL = "model"
    const val KEY_ENV_OPT_OUT = "env_opt_out"
  }
}
