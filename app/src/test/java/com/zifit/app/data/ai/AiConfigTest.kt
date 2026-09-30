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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AI 配置的解析与展示约定。重点在「三栏都不许空」与「密钥永不整串示人」。 */
class AiConfigTest {

  @Test
  fun allThreeFieldsAreRequired() {
    assertTrue(AiConfig("https://api.deepseek.com", "sk-1", "deepseek-v4-flash").isConfigured)
    assertFalse(AiConfig("https://api.deepseek.com", "sk-1", "").isConfigured)
    assertFalse(AiConfig("", "sk-1", "deepseek-v4-flash").isConfigured)
    assertFalse(AiConfig("https://api.deepseek.com", "", "deepseek-v4-flash").isConfigured)
  }

  @Test
  fun missingFieldsReportsExactlyWhatIsMissing() {
    assertEquals(setOf(AiField.API_KEY, AiField.MODEL), AiConfig(baseUrl = "https://a.com").missingFields)
    assertEquals(emptySet<AiField>(), AiConfig("https://a.com", "k", "m").missingFields)
  }

  @Test
  fun draftIsTrimmedAndTrailingSlashRemoved() {
    val result =
      AiConfigDraft(baseUrl = "  https://api.deepseek.com/  ", apiKey = " sk-1 ", model = " m ")
        .toConfig()
    val config = (result as AiConfigDraftResult.Valid).config
    assertEquals("https://api.deepseek.com", config.baseUrl)
    assertEquals("sk-1", config.apiKey)
    assertEquals("m", config.model)
  }

  @Test
  fun blankFieldIsReportedAsMissing_notAsValid() {
    assertEquals(
      AiConfigDraftResult.Missing(AiField.BASE_URL),
      AiConfigDraft(apiKey = "sk-1", model = "m").toConfig(),
    )
    assertEquals(
      AiConfigDraftResult.Missing(AiField.API_KEY),
      AiConfigDraft(baseUrl = "https://a.com", model = "m").toConfig(),
    )
    assertEquals(
      AiConfigDraftResult.Missing(AiField.MODEL),
      AiConfigDraft(baseUrl = "https://a.com", apiKey = "sk-1").toConfig(),
    )
  }

  /** 漏写协议头是最常见的错法，必须在这里拦下，而不是等 URL 拼出来才报错。 */
  @Test
  fun urlWithoutSchemeIsRejected() {
    assertEquals(
      AiConfigDraftResult.UrlNotHttp,
      AiConfigDraft(baseUrl = "api.deepseek.com", apiKey = "sk-1", model = "m").toConfig(),
    )
  }

  /** `http://` 刻意放行：本地推理服务（Ollama / llama.cpp）就是 http。 */
  @Test
  fun plainHttpIsAllowed() {
    assertTrue(
      AiConfigDraft(baseUrl = "http://192.168.1.5:11434/v1", apiKey = "k", model = "m")
        .toConfig() is AiConfigDraftResult.Valid
    )
  }

  /** 端点拼装的唯一入口：baseUrl 末尾带不带斜杠都要拼出同一个结果。 */
  @Test
  fun endpointIsBuiltFromBaseUrlOnly() {
    assertEquals(
      "https://a.com/v1/models",
      AiConfig(baseUrl = "https://a.com/v1/").endpoint("/models"),
    )
    assertEquals(
      "https://a.com/v1/models",
      AiConfig(baseUrl = "https://a.com/v1").endpoint("/models"),
    )
  }

  @Test
  fun draftRoundTripKeepsValues() {
    val config = AiConfig("https://a.com/v1", "sk-abcdefgh", "m")
    assertEquals(config, (config.toDraft().toConfig() as AiConfigDraftResult.Valid).config)
  }

  /** 界面上永不出现完整密钥：只露头 3 位与尾 4 位。 */
  @Test
  fun apiKeyIsMasked() {
    assertEquals("sk-••••cdef", maskApiKey("sk-abcdefghijklmnopcdef"))
    // 太短的连头尾都不露，否则「露出的部分」就是它的大半
    assertEquals("••••••", maskApiKey("sk-123"))
    assertEquals("", maskApiKey(""))
  }
}
