/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ai

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 仓库实现的单测。真实实现依赖 `SharedPreferences`，JVM 里没有 —— 所以下面是个内存版假实现。
 *
 * 要覆盖的正是**真机很难反复验的那几条**：`.env` 默认值什么时候生效、
 * 「清除配置」之后会不会被 `.env` 悄悄灌回来。
 */
class AiConfigRepositoryTest {

  private val env = AiConfig("https://env.example", "env-key", "env-model")
  private val saved = AiConfig("https://saved.example", "saved-key", "saved-model")

  @Test
  fun envIsUsedWhenNothingSavedLocally() {
    val repo = SharedPrefsAiConfigRepository(FakePrefs(), envDefaults = env)

    assertEquals(env, repo.config.value)
    assertEquals(AiConfigSource.ENV, repo.source.value)
  }

  @Test
  fun savedConfigWinsOverEnv() {
    val repo = SharedPrefsAiConfigRepository(FakePrefs(), envDefaults = env)

    repo.save(saved)

    assertEquals(saved, repo.config.value)
    assertEquals(AiConfigSource.SAVED, repo.source.value)
  }

  /**
   * **最要紧的一条**：在 `.env` 有值的情况下点「清除配置」，配置要真的空掉，
   * 而不是下一次读取又被 `.env` 灌回来（那样按钮按了等于没按）。
   */
  @Test
  fun clearKeepsEnvFromComingBack() {
    val repo = SharedPrefsAiConfigRepository(FakePrefs(), envDefaults = env)

    repo.save(saved)
    repo.clear()

    assertEquals(AiConfig(), repo.config.value)
    assertEquals(AiConfigSource.NONE, repo.source.value)
  }

  /** 从没保存过、直接点清除，也要停在「什么都没有」而不是退回 `.env`。 */
  @Test
  fun clearWithoutAnySaveAlsoStaysEmpty() {
    val prefs = FakePrefs()
    val repo = SharedPrefsAiConfigRepository(prefs, envDefaults = env)

    repo.clear()

    assertEquals(AiConfig(), repo.config.value)
    assertEquals(AiConfigSource.NONE, repo.source.value)
    // 而且这个「别灌回来」的状态是**落盘**的：换一个实例读同一份 prefs，结论不变
    assertEquals(AiConfigSource.NONE, SharedPrefsAiConfigRepository(prefs, envDefaults = env).source.value)
  }

  /** release 包：`.env` 三项是空串，等价于「没有调试默认值」。 */
  @Test
  fun releaseBuildWithoutEnvIsSimplyUnconfigured() {
    val repo = SharedPrefsAiConfigRepository(FakePrefs(), envDefaults = AiConfig())

    assertEquals(AiConfig(), repo.config.value)
    assertEquals(AiConfigSource.NONE, repo.source.value)
  }
}

/**
 * 内存版 `SharedPreferences`。只实现仓库用得到的那几个方法，其余按接口语义给最小实现。
 *
 * ⚠️ `apply()` 必须**立即**落到内存 —— 真机上的 SharedPreferences 也是这样（异步落盘、
 * 同步更新内存），因此 `save()` 之后紧接着 `config.value` 就能读到新值。
 */
private class FakePrefs : SharedPreferences {

  private val values = mutableMapOf<String, Any?>()

  override fun getAll(): MutableMap<String, *> = values

  override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue

  override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
    @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String>) ?: defValues

  override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

  override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

  override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

  override fun getBoolean(key: String?, defValue: Boolean): Boolean =
    values[key] as? Boolean ?: defValue

  override fun contains(key: String?): Boolean = values.containsKey(key)

  override fun edit(): SharedPreferences.Editor = EditorImpl()

  override fun registerOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener?
  ) = Unit

  override fun unregisterOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener?
  ) = Unit

  private inner class EditorImpl : SharedPreferences.Editor {

    private val puts = mutableMapOf<String, Any?>()

    private val removes = mutableSetOf<String>()

    override fun putString(key: String?, value: String?): SharedPreferences.Editor = put(key, value)

    override fun putStringSet(
      key: String?,
      values: MutableSet<String>?,
    ): SharedPreferences.Editor = put(key, values)

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor = put(key, value)

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor = put(key, value)

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = put(key, value)

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = put(key, value)

    override fun remove(key: String?): SharedPreferences.Editor = put(key, null, removed = true)

    override fun clear(): SharedPreferences.Editor {
      values.clear()
      puts.clear()
      removes.clear()
      return this
    }

    override fun commit(): Boolean = true.also { apply() }

    override fun apply() {
      removes.forEach { values.remove(it) }
      puts.forEach { (key, value) -> values[key] = value }
    }

    private fun put(key: String?, value: Any?, removed: Boolean = false): SharedPreferences.Editor {
      if (key != null) {
        if (removed) removes += key else puts[key] = value
      }
      return this
    }
  }
}
