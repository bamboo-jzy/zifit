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
 * 「服务地址 + 模型」的填空预设。
 *
 * 预设**只是省打字**，不是白名单：地址与模型在界面上都能改，`自定义` 就是全空手填。
 * ⚠️ 各家改名/换域名的频率不低，下面的值**复核于 2026-09-30**（DeepSeek 官方 API 文档、
 * 阿里云百炼 OpenAI 兼容文档）。加了半年后如果有人反馈连不上，先复核这里再怀疑代码。
 *
 * 参考：
 * - DeepSeek：`https://api.deepseek.com`（OpenAI 兼容；模型 `deepseek-v4-flash` / `deepseek-v4-pro`，
 *   旧的 `deepseek-chat` / `deepseek-reasoner` 已于 2026-07-24 起标记弃用）
 * - 阿里云百炼：`https://dashscope.aliyuncs.com/compatible-mode/v1`（模型 `qwen-plus` 等）
 */
data class AiPreset(val name: String, val baseUrl: String, val model: String)

/** 界面上的预设列表。刻意**没有**「自定义」这一项 —— 它是「一个都没选中」的那一态
 * （手改任意一栏，预设胶囊就自动变回未选中），再摆一个同名胶囊只会让人以为要先去点它。 */
val AI_PRESETS: List<AiPreset> =
  listOf(
    AiPreset(name = "DeepSeek", baseUrl = "https://api.deepseek.com", model = "deepseek-v4-flash"),
    AiPreset(
      name = "阿里云百炼",
      baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
      model = "qwen-plus",
    ),
  )
