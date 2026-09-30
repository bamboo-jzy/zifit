/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 「当前配置从哪来」这张优先级表的单测。
 *
 * 刻意**不**断言 [AiEnvDefaults.isAvailable] 的真值：它取决于构建机上 `.env` 里有没有填 key，
 * 填了之后这条断言就会翻脸。要测的是**规则**，不是「本机现在恰好填了没」。
 */
class AiEnvDefaultsTest {

  @Test
  fun savedConfigWinsOverEnv() {
    assertEquals(
      AiConfigSource.SAVED,
      resolveAiConfigSource(savedIsConfigured = true, envOptOut = false, envAvailable = true),
    )
    // 用户自己保存过，即便是「清过 .env 默认值」之后也一样
    assertEquals(
      AiConfigSource.SAVED,
      resolveAiConfigSource(savedIsConfigured = true, envOptOut = true, envAvailable = true),
    )
  }

  @Test
  fun envIsUsedWhenNothingSavedLocally() {
    assertEquals(
      AiConfigSource.ENV,
      resolveAiConfigSource(savedIsConfigured = false, envOptOut = false, envAvailable = true),
    )
  }

  /**
   * **本组最要紧的一条**：点过「清除配置」之后不许再把 `.env` 的值灌回来 ——
   * 否则那个按钮按了等于没按，用户会觉得配置删不掉。
   */
  @Test
  fun envOptOutOutranksEnv() {
    assertEquals(
      AiConfigSource.NONE,
      resolveAiConfigSource(savedIsConfigured = false, envOptOut = true, envAvailable = true),
    )
  }

  @Test
  fun nothingAvailableMeansNone() {
    assertEquals(
      AiConfigSource.NONE,
      resolveAiConfigSource(savedIsConfigured = false, envOptOut = false, envAvailable = false),
    )
    // release 包（env 恒空）+ opt-out 的组合
    assertEquals(
      AiConfigSource.NONE,
      resolveAiConfigSource(savedIsConfigured = false, envOptOut = true, envAvailable = false),
    )
  }
}
