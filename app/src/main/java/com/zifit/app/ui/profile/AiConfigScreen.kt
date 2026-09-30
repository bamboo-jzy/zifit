/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.data.ai.AI_PRESETS
import com.zifit.app.data.ai.AiConfig
import com.zifit.app.data.ai.AiConfigDraft
import com.zifit.app.data.ai.AiConfigDraftResult
import com.zifit.app.data.ai.AiConfigSource
import com.zifit.app.data.ai.AiField
import com.zifit.app.data.ai.AiTestResult
import com.zifit.app.data.ai.NetworkErrorKind
import com.zifit.app.data.ai.maskApiKey
import com.zifit.app.data.ai.toConfig
import com.zifit.app.data.ai.toDraft
import com.zifit.app.theme.Accent
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.common.FilterPill
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.GlassTextField
import com.zifit.app.ui.common.HintText
import com.zifit.app.ui.common.PageHeader
import com.zifit.app.ui.common.dockContentInset

/**
 * AI 接入设置页：服务地址 / 模型 / API Key 三项 + 连通性测试。
 *
 * 2026-09-30 由**弹窗**改成**整页**：它本来就有状态卡 + 三项输入 + 测试结论，
 * 挤在 88% 屏高的弹窗里滚动，远不如一整页来得从容；而且「我的 → 设置 → AI 接入」
 * 这条路径本来就需要一个能返回的页面。
 *
 * 校验用 [AiConfigDraft.toConfig] 这个纯函数（在 data 层、可单测），界面只做文案映射。
 */
@Composable
internal fun AiConfigScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
  val config by viewModel.config.collectAsStateWithLifecycle()
  val source by viewModel.source.collectAsStateWithLifecycle()
  val testState by viewModel.test.collectAsStateWithLifecycle()

  // 进页面就作废上一次的测试结论：那可能是拿旧地址测出来的，留着就是误导
  LaunchedEffect(Unit) { viewModel.resetTest() }

  // `remember(config)`：保存成功后 config 会变，草稿随之刷新成刚落库的那份
  var draft by remember(config) { mutableStateOf(config.toDraft()) }
  var error by remember { mutableStateOf<AiConfigDraftResult?>(null) }

  // 有没有可清的东西：三个键全空时「清除配置」没有意义，不如不显示
  val hasStored = config.baseUrl.isNotBlank() || config.apiKey.isNotBlank() || config.model.isNotBlank()

  Column(modifier = modifier.fillMaxSize()) {
    PageHeader(title = stringResource(R.string.ai_editor_title), onBack = onBack)
    Spacer(Modifier.height(16.dp))

    Column(
      modifier =
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(bottom = dockContentInset()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // ⚠️ 顺序是**表单在前、状态卡在后**，不是随手排的：真机实测（1224×2688）
      // 「状态卡在上」时整页高 2.6k+，把「保存」与测试结论挤到悬浮 dock 之下（y2478 > dock 上沿 2477），
      // 用户点「测试连接」屏幕上什么都不会变。表单在上之后，输入框、测试按钮、结论、保存
      // 全在首屏，状态卡退到下面当「当前生效配置」的回显。
      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = stringResource(R.string.ai_editor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = InkFaint,
          )
          Text(
            text = stringResource(R.string.ai_editor_presets),
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AI_PRESETS.forEach { preset ->
              val picked = draft.baseUrl == preset.baseUrl && draft.model == preset.model
              FilterPill(
                label = preset.name,
                selected = picked,
                // 预设只是**填空**：点一下把地址与模型写进草稿，密钥不动。
                // 手改过任何一栏后它会自动变回未选中 —— 那时它就名副其实地是「自定义」了。
                onClick = {
                  draft = draft.copy(baseUrl = preset.baseUrl, model = preset.model)
                  error = null
                  viewModel.resetTest()
                },
              )
            }
          }

          GlassTextField(
            label = stringResource(R.string.ai_editor_base_url),
            value = draft.baseUrl,
            onValueChange = {
              draft = draft.copy(baseUrl = it)
              error = null
              viewModel.resetTest()
            },
            keyboardType = KeyboardType.Uri,
          )
          GlassTextField(
            label = stringResource(R.string.ai_editor_model),
            value = draft.model,
            onValueChange = {
              draft = draft.copy(model = it)
              error = null
              viewModel.resetTest()
            },
            keyboardType = KeyboardType.Text,
          )
          GlassTextField(
            label = stringResource(R.string.ai_editor_api_key),
            value = draft.apiKey,
            onValueChange = {
              draft = draft.copy(apiKey = it)
              error = null
              viewModel.resetTest()
            },
            keyboardType = KeyboardType.Text,
            // 密钥不落在屏幕上：输入框走密码变换，状态卡里也只显示掩码
            visualTransformation = PasswordVisualTransformation(),
          )

          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterPill(
              label = stringResource(R.string.ai_editor_test),
              selected = false,
              onClick = {
                when (val result = draft.toConfig()) {
                  is AiConfigDraftResult.Valid -> {
                    error = null
                    viewModel.testConnection(result.config)
                  }
                  // 测试也要能报「哪一栏没填」：拿着一个空地址去测，用户只会得到一句
                  // 莫名其妙的「连不上」
                  else -> error = result
                }
              },
            )
            // `.env` 的三项已经在包里了，一键抄进表单 —— 主要用途是「清除配置」之后想拿回来，
            // 以及核对构建时到底注入进去了什么。**不是**自动保存：用户仍要按「保存」。
            viewModel.envDefaults?.let { envDefaults ->
              FilterPill(
                label = stringResource(R.string.ai_editor_load_env),
                selected = false,
                onClick = {
                  draft = envDefaults.toDraft()
                  error = null
                  viewModel.resetTest()
                },
              )
            }
            // 结论就贴在按钮旁边。放在表单下方独立一行的话，它会掉到 dock 底下 ——
            // 用户点了「测试连接」，屏幕上什么都不会变（这是真机上抓到的，不是推测）。
            when (val state = testState) {
              is AiTestUiState.Testing ->
                Text(
                  text = stringResource(R.string.ai_editor_testing),
                  style = MaterialTheme.typography.bodySmall,
                  color = InkMuted,
                )
              is AiTestUiState.Done ->
                Text(
                  text = testResultText(state.result),
                  style = MaterialTheme.typography.bodySmall,
                  color = if (state.result is AiTestResult.Ok) Accent else InkMuted,
                  modifier = Modifier.weight(1f),
                )
              AiTestUiState.Idle -> Unit
            }
          }
        }
      }

      error?.let { HintText(text = errorText(it), emphasized = true) }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (hasStored) {
          FilterPill(
            label = stringResource(R.string.ai_editor_clear),
            selected = false,
            onClick = {
              viewModel.clear()
              onBack()
            },
          )
        }
        Spacer(Modifier.weight(1f))
        FilterPill(
          label = stringResource(R.string.ai_editor_save),
          selected = true,
          onClick = {
            when (val result = draft.toConfig()) {
              is AiConfigDraftResult.Valid -> {
                viewModel.save(result.config)
                onBack()
              }
              // 保存也走同一条校验：**不许**存进一个连不上/填不全的配置
              else -> error = result
            }
          },
        )
      }

      StatusCard(config = config, source = source)
    }
  }
}

/**
 * 状态卡：**如实**说「配好了 / 还缺什么」，密钥只以掩码示人。
 *
 * 配好了才有意义显示服务地址与模型 —— 没配好时那两行是空的，印出来只是噪音。
 *
 * 还多一行**来源**：`.env` 注入的调试配置和用户自己填的长得一模一样，
 * 不标注的话，用户看到「密钥掩码」根本分不清那是自己填的还是构建时带进来的。
 */
@Composable
private fun StatusCard(config: AiConfig, source: AiConfigSource) {
  GlassSurface(modifier = Modifier.fillMaxWidth()) {
    Text(text = stringResource(R.string.profile_ai), style = MaterialTheme.typography.titleMedium)
    Text(
      text = stringResource(R.string.profile_ai_desc),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 4.dp),
    )

    Text(
      text =
        stringResource(
          if (config.isConfigured) R.string.profile_ai_status_on else R.string.profile_ai_status_off
        ),
      style = MaterialTheme.typography.labelLarge,
      color = if (config.isConfigured) Accent else InkFaint,
      modifier = Modifier.padding(top = 8.dp),
    )

    if (config.isConfigured) {
      AiLine(stringResource(R.string.profile_ai_line_service, config.baseUrl))
      AiLine(stringResource(R.string.profile_ai_line_model, config.model))
      AiLine(stringResource(R.string.profile_ai_line_key, maskApiKey(config.apiKey)))
      AiLine(stringResource(R.string.profile_ai_line_source, sourceLabel(source)))
    } else {
      // ⚠️ 拼接必须走 `map` + `joinToString` 两步，不能把 `fieldLabel`（@Composable）
      // 直接写进 `joinToString` 的 lambda —— 那是个普通（非 inline）lambda，不是 Composable 上下文。
      AiLine(
        stringResource(
          R.string.profile_ai_missing,
          config.missingFields.map { fieldLabel(it) }.joinToString("、"),
        )
      )
    }

    Text(
      // `.env` 那套密钥是编进 APK 的，跟「存进本机私有目录」是两回事，说法必须跟着变，
      // 否则这句隐私说明对 ENV 那个档就是假的。
      text =
        stringResource(
          if (source == AiConfigSource.ENV) R.string.profile_ai_privacy_env
          else R.string.profile_ai_privacy
        ),
      style = MaterialTheme.typography.labelSmall,
      color = InkFaint,
      modifier = Modifier.padding(top = 8.dp),
    )
  }
}

@Composable
private fun AiLine(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = InkMuted,
    modifier = Modifier.padding(top = 6.dp),
  )
}

/** 配置来源的人话。 */
@Composable
private fun sourceLabel(source: AiConfigSource): String =
  stringResource(
    when (source) {
      AiConfigSource.SAVED -> R.string.profile_ai_source_saved
      AiConfigSource.ENV -> R.string.profile_ai_source_env
      // 走到这里说明没配好，状态卡本不会显示这一行；给个中性文案，别让 when 漏项
      AiConfigSource.NONE -> R.string.profile_ai_source_none
    }
  )

@Composable
private fun errorText(result: AiConfigDraftResult): String =
  when (result) {
    is AiConfigDraftResult.Valid -> ""
    is AiConfigDraftResult.Missing -> stringResource(R.string.ai_error_missing_field, fieldLabel(result.field))
    AiConfigDraftResult.UrlNotHttp -> stringResource(R.string.ai_error_url_not_http)
  }

/** 栏位名（服务地址 / API Key / 模型）。状态卡与表单共用一份，避免两处文案走偏。 */
@Composable
internal fun fieldLabel(field: AiField): String =
  stringResource(
    when (field) {
      AiField.BASE_URL -> R.string.ai_editor_base_url
      AiField.API_KEY -> R.string.ai_editor_api_key
      AiField.MODEL -> R.string.ai_editor_model
    }
  )

@Composable
private fun testResultText(result: AiTestResult): String =
  when (result) {
    is AiTestResult.Ok -> stringResource(R.string.ai_test_ok, result.code)
    is AiTestResult.Unauthorized -> stringResource(R.string.ai_test_unauthorized, result.code)
    is AiTestResult.NoModelsEndpoint -> stringResource(R.string.ai_test_no_models)
    is AiTestResult.HttpError -> stringResource(R.string.ai_test_http_error, result.code)
    is AiTestResult.NetworkError ->
      stringResource(
        when (result.kind) {
          NetworkErrorKind.DNS -> R.string.ai_test_err_dns
          NetworkErrorKind.TIMEOUT -> R.string.ai_test_err_timeout
          NetworkErrorKind.REFUSED -> R.string.ai_test_err_refused
          NetworkErrorKind.TLS -> R.string.ai_test_err_tls
          // 归不到类的那一档才把原始信息带出来 —— 至少让用户能搜得到
          NetworkErrorKind.OTHER -> R.string.ai_test_network_error
        },
        result.detail,
      )
  }
