/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zifit.app.theme.AccentColor
import com.zifit.app.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 界面偏好的唯一存取出口（AGENTS.md §4 分层约束）。
 *
 * 接口化的理由与其它 Repository 相同：真实实现依赖 `SharedPreferences`，JVM 单测拿不到。
 */
interface UiPreferencesRepository {

  val preferences: StateFlow<UiPreferences>

  fun setGlassBlur(enabled: Boolean)

  fun setThemeMode(mode: ThemeMode)

  fun setAccent(accent: AccentColor)

  companion object {
    /**
     * **进程内单例**（与 `ExerciseRepository` 同款）。
     *
     * ⚠️ 这里与 `UserProfileRepository` 的取舍**刻意不同**，别照着那边抄：
     * 那边每次 `get()` 都新建实例，靠「用之前现读一次」兜住（个人资料只在进入页面时读）。
     * 界面偏好不能这么办 —— 同一份偏好同时被多处消费：设置页写、根导航与主题读，
     * 两个实例各持一份 `StateFlow` 的话，**改了配置当场不生效**，得重启 App。
     */
    @Volatile private var instance: UiPreferencesRepository? = null

    fun get(context: Context): UiPreferencesRepository =
      instance
        ?: synchronized(this) {
          instance
            ?: SharedPrefsUiPreferencesRepository(
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
              )
              .also { instance = it }
        }

    private const val PREFS_NAME = "ui_preferences"
  }
}

/** 几个标量偏好，用 `SharedPreferences` 而不是 Room —— 它不是表。 */
class SharedPrefsUiPreferencesRepository(private val prefs: SharedPreferences) :
  UiPreferencesRepository {

  private val state = MutableStateFlow(read())

  override val preferences: StateFlow<UiPreferences> = state.asStateFlow()

  override fun setGlassBlur(enabled: Boolean) {
    prefs.edit { putBoolean(KEY_GLASS_BLUR, enabled) }
    state.value = state.value.copy(glassBlur = enabled)
  }

  override fun setThemeMode(mode: ThemeMode) {
    prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    state.value = state.value.copy(themeMode = mode)
  }

  override fun setAccent(accent: AccentColor) {
    prefs.edit { putString(KEY_ACCENT, accent.name) }
    state.value = state.value.copy(accent = accent)
  }

  /** 读不到就用默认值（首次安装）——默认值与 [UiPreferences] 的声明保持同一处口径。 */
  private fun read(): UiPreferences {
    val defaults = UiPreferences()
    return UiPreferences(
      glassBlur = prefs.getBoolean(KEY_GLASS_BLUR, defaults.glassBlur),
      // 主题模式走它自己的还原函数，不套下面的通用版：2026-09-30 枚举由三档改四档、
      // 名字全换过，只有 [ThemeMode.fromStorage] 认得旧名，通用版只认现名 ——
      // 套错的表现是"存过主题的人一升级，选择被静默重置回默认"。
      themeMode =
        ThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, null)) ?: defaults.themeMode,
      accent = enumOrDefault(prefs.getString(KEY_ACCENT, null), defaults.accent),
    )
  }

  /**
   * 枚举按名字存，读回来要**容错**：值可能是旧版本写的、也可能被手改坏。
   * 直接用 `valueOf` 会在启动那一刻抛异常 —— 那是一个"只有装过旧版的人才会踩、
   * 而且一踩就崩"的坑，不值得为省一行代码留着。
   *
   * 只用于 [AccentColor]：它的枚举名自引入起没变过。**改过名的枚举不能用这一套**
   * （见上面 [ThemeMode.fromStorage]），那需要按旧名逐个映射。
   */
  private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, fallback: T): T =
    name?.let { raw -> enumValues<T>().firstOrNull { it.name == raw } } ?: fallback

  private companion object {
    const val KEY_GLASS_BLUR = "glass_blur"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_ACCENT = "theme_accent"
  }
}
