/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ai

/**
 * AI 服务接入配置。**三项都必填** —— 少任何一项都拼不出一次可用的请求。
 *
 * 刻意只留这三项：「服务地址 + 密钥 + 模型」是 OpenAI 兼容接口的全部必要信息，
 * 也是国内各家（DeepSeek / 百炼 / 智谱 / 本地 Ollama…）通用的那一层。
 * 温度、最大 token 这类参数等真正需要时再加，现在加只会让配置界面多出没人改的旋钮。
 *
 * ⚠️ [apiKey] 以**明文**存在应用私有目录的 SharedPreferences 里。
 * 不引入 `EncryptedSharedPreferences`：`androidx.security.crypto` 已停止维护，
 * 换进来等于引一个没人修的黑盒。这台机器只有自己用、APK 侧载，明文可接受 ——
 * 代价是：**密钥不得进日志、不得进 Git、不得出现在任何上报里**。
 */
data class AiConfig(
  /** 服务地址（Base URL），如 `https://api.deepseek.com`。末尾斜杠会被规整掉。 */
  val baseUrl: String = "",
  /** API Key。 */
  val apiKey: String = "",
  /** 模型名，如 `deepseek-v4-flash`。 */
  val model: String = "",
) {
  /** 三项齐全才算配好；少一项就是没配好，界面必须如实说「缺 X」。 */
  val isConfigured: Boolean
    get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

  /** 还缺哪几项。已配置时为空集。 */
  val missingFields: Set<AiField>
    get() =
      buildSet {
        if (baseUrl.isBlank()) add(AiField.BASE_URL)
        if (apiKey.isBlank()) add(AiField.API_KEY)
        if (model.isBlank()) add(AiField.MODEL)
      }

  /** 拼一个 OpenAI 兼容的端点。**唯一**的 URL 拼装处，别在别处再写一遍。 */
  fun endpoint(path: String): String = baseUrl.trimEnd('/') + path
}

/** 配置里的一栏，用于把「缺哪一项 / 哪一项填错了」精确报给用户。 */
enum class AiField {
  BASE_URL,
  API_KEY,
  MODEL,
}

/**
 * 当前生效的配置**来自哪里**。界面必须说清楚 —— 否则用户会疑惑
 * 「我明明在界面上填过，怎么显示的是另一个地址」，或者反过来。
 */
enum class AiConfigSource {
  /** 什么都没配。 */
  NONE,

  /** 用户在界面上填并保存的（存在本机 SharedPreferences）。 */
  SAVED,

  /** 来自仓库根 `.env` 的调试默认值（仅 debug 包可能落到这一档）。 */
  ENV,
}

/**
 * 配置表单的**原始文本**。与 `FoodDraft` 同构：用户在输入途中会停在
 * 空串、`https:/`、`sk-` 这类中间状态，过早校验只会边打字边报错。
 */
data class AiConfigDraft(
  val baseUrl: String = "",
  val apiKey: String = "",
  val model: String = "",
)

/** [AiConfigDraft.toConfig] 的结果。 */
sealed interface AiConfigDraftResult {
  data class Valid(val config: AiConfig) : AiConfigDraftResult

  /** 某一栏没填。 */
  data class Missing(val field: AiField) : AiConfigDraftResult

  /** 服务地址不是 http/https 开头 —— 拼出来的 URL 根本无法建立连接。 */
  data object UrlNotHttp : AiConfigDraftResult
}

/**
 * 校验并规整成可保存的 [AiConfig]。纯函数，可单测。
 *
 * 规则：
 * - 三栏都去首尾空白，**都不得为空**（这里的「留空」不像营养值那样有「未测」的含义）；
 * - 服务地址必须以 `http://` 或 `https://` 开头：漏写协议头是最常见的错法，
 *   拼出来的 `api.deepseek.com/models` 在 `URL()` 里直接被当成相对路径，报错让人摸不着头脑。
 *   ⚠️ 刻意**不禁** `http://`：本地推理服务（Ollama / llama.cpp）就是 http。
 */
fun AiConfigDraft.toConfig(): AiConfigDraftResult {
  val url = baseUrl.trim().trimEnd('/')
  val key = apiKey.trim()
  val name = model.trim()

  if (url.isEmpty()) return AiConfigDraftResult.Missing(AiField.BASE_URL)
  if (key.isEmpty()) return AiConfigDraftResult.Missing(AiField.API_KEY)
  if (name.isEmpty()) return AiConfigDraftResult.Missing(AiField.MODEL)
  if (!url.startsWith("http://") && !url.startsWith("https://")) {
    return AiConfigDraftResult.UrlNotHttp
  }

  return AiConfigDraftResult.Valid(AiConfig(baseUrl = url, apiKey = key, model = name))
}

/** 把已保存的配置回填成表单初值。 */
fun AiConfig.toDraft(): AiConfigDraft =
  AiConfigDraft(baseUrl = baseUrl, apiKey = apiKey, model = model)

/**
 * 密钥的展示形式：`sk-abcd…wxyz` 只露头尾，中间一律用 `•`。
 *
 * 界面上**永不显示完整密钥** —— 输入框已经是密码框，配置卡片里更不能把明文印出来。
 * 太短的（≤ 8 位）连头尾都不露，全遮。
 */
fun maskApiKey(apiKey: String): String {
  val key = apiKey.trim()
  if (key.isEmpty()) return ""
  if (key.length <= 8) return "•".repeat(key.length.coerceAtLeast(4))
  return key.take(3) + "•".repeat(4) + key.takeLast(4)
}
