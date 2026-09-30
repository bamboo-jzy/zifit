/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zifit.app.data.ai.AiConfig
import com.zifit.app.data.ai.AiConfigRepository
import com.zifit.app.data.ai.AiConfigSource
import com.zifit.app.data.ai.AiConnectionTester
import com.zifit.app.data.ai.AiEnvDefaults
import com.zifit.app.data.ai.AiTestResult
import com.zifit.app.data.ai.HttpAiConnectionTester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 连通性测试在界面上的状态。 */
internal sealed interface AiTestUiState {
  /** 还没测过（或改了输入，旧结论已作废）。 */
  data object Idle : AiTestUiState

  data object Testing : AiTestUiState

  data class Done(val result: AiTestResult) : AiTestUiState
}

/**
 * 「我的」页的 ViewModel。目前只承载 AI 接入配置 —— 身体数据还没做，
 * 有了之后也应该收在这里，而不是在 Screen 里直接摸 Repository。
 */
internal class ProfileViewModel(
  private val repository: AiConfigRepository,
  private val tester: AiConnectionTester = HttpAiConnectionTester(),
  /** `.env` 注入的调试默认值。默认参数读 BuildConfig，release 包必须是全空配置。 */
  private val env: AiConfig = AiEnvDefaults.config,
) : ViewModel() {

  val config: StateFlow<AiConfig> = repository.config

  /** 当前配置的来源（自己保存的 / `.env` / 没有）。 */
  val source: StateFlow<AiConfigSource> = repository.source

  /**
   * `.env` 三项齐全时才有值 —— 界面据此决定要不要显示「载入 .env」。
   * 显示一个点了没反应的按钮比不显示更糟，所以这里没配齐就是 null。
   */
  val envDefaults: AiConfig? = env.takeIf { it.isConfigured }

  private val _test = MutableStateFlow<AiTestUiState>(AiTestUiState.Idle)
  val test: StateFlow<AiTestUiState> = _test.asStateFlow()

  /**
   * 「测试结论属于哪一次输入」的代次号。
   *
   * ⚠️ 真机验证抓出来的问题：测出「域名解析失败」之后用户改了地址，屏幕上那条结论
   * 说的却是**旧地址**。所以在途结果回来时要核对代次，对不上就丢弃。
   */
  private var testToken = 0

  fun save(config: AiConfig) {
    repository.save(config)
    // 配置变了，上一次「连接正常」的结论对新的地址/密钥不再成立，必须作废
    resetTest()
  }

  fun clear() {
    repository.clear()
    resetTest()
  }

  /** 用**当前表单里**的草稿去测，而不是已保存的配置 —— 用户想先验证再保存。 */
  fun testConnection(config: AiConfig) {
    if (_test.value is AiTestUiState.Testing) return
    val token = ++testToken
    _test.value = AiTestUiState.Testing
    viewModelScope.launch {
      val result = tester.test(config)
      // 期间用户又改了输入（[resetTest] 已经推进代次）→ 这条结论已经过期，丢掉
      if (token == testToken) _test.value = AiTestUiState.Done(result)
    }
  }

  /** 输入一变就调用：作废上一次的结论（含还在路上的那一次）。 */
  fun resetTest() {
    testToken++
    _test.value = AiTestUiState.Idle
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer { ProfileViewModel(AiConfigRepository.get(context)) }
    }
  }
}
