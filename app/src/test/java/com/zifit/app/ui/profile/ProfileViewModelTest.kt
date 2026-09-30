/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import com.zifit.app.MainDispatcherRule
import com.zifit.app.data.ai.AiConfig
import com.zifit.app.data.ai.AiConfigRepository
import com.zifit.app.data.ai.AiConfigSource
import com.zifit.app.data.ai.AiConnectionTester
import com.zifit.app.data.ai.AiTestResult
import com.zifit.app.data.ai.NetworkErrorKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** 内存版配置仓库：真实实现依赖 `SharedPreferences`，JVM 单测里拿不到。 */
private class FakeAiConfigRepository : AiConfigRepository {
  private val state = MutableStateFlow(AiConfig())
  private val sourceState = MutableStateFlow(AiConfigSource.NONE)
  override val config: StateFlow<AiConfig> = state.asStateFlow()
  override val source: StateFlow<AiConfigSource> = sourceState.asStateFlow()

  override fun save(config: AiConfig) {
    state.value = config
    sourceState.value = AiConfigSource.SAVED
  }

  override fun clear() {
    state.value = AiConfig()
    sourceState.value = AiConfigSource.NONE
  }
}

/** 可控的探针：`gate` 不为 null 时先挂住，便于制造「在途」这一态。 */
private class FakeTester(
  private val result: AiTestResult,
  private val gate: CompletableDeferred<Unit>? = null,
) : AiConnectionTester {
  override suspend fun test(config: AiConfig): AiTestResult {
    gate?.await()
    return result
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val dnsFailure =
    AiTestResult.NetworkError(NetworkErrorKind.DNS, "Unable to resolve host \"x\"")

  @Test
  fun saveGoesThroughToRepository() = runTest(mainDispatcherRule.testDispatcher) {
    val repo = FakeAiConfigRepository()
    val vm = ProfileViewModel(repo, FakeTester(AiTestResult.Ok(200)))

    vm.save(AiConfig("https://a.com", "k", "m"))
    advanceUntilIdle()

    assertEquals(AiConfig("https://a.com", "k", "m"), repo.config.value)
    assertEquals(AiConfig("https://a.com", "k", "m"), vm.config.value)
  }

  @Test
  fun clearEmptiesConfiguration() = runTest(mainDispatcherRule.testDispatcher) {
    val repo = FakeAiConfigRepository()
    val vm = ProfileViewModel(repo, FakeTester(AiTestResult.Ok(200)))

    vm.save(AiConfig("https://a.com", "k", "m"))
    vm.clear()
    advanceUntilIdle()

    assertEquals(AiConfig(), repo.config.value)
  }

  @Test
  fun testResultShowsUpWhenNothingChanges() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = ProfileViewModel(FakeAiConfigRepository(), FakeTester(dnsFailure))

    vm.testConnection(AiConfig("https://a.com", "k", "m"))
    advanceUntilIdle()

    assertEquals(AiTestUiState.Done(dnsFailure), vm.test.value)
  }

  /**
   * **本组最要紧的一条**（真机验证抓出来的）：测出「域名解析失败」之后用户改了地址，
   * 屏幕上那条结论说的是旧地址 —— 必须作废，尤其**在途**的那一次。
   */
  @Test
  fun staleInFlightResultIsDiscardedAfterInputChanges() = runTest(mainDispatcherRule.testDispatcher) {
    val gate = CompletableDeferred<Unit>()
    val vm = ProfileViewModel(FakeAiConfigRepository(), FakeTester(dnsFailure, gate))

    vm.testConnection(AiConfig("https://old.example", "k", "m"))
    assertEquals(AiTestUiState.Testing, vm.test.value)

    // 用户在测试还没回来时改了地址
    vm.resetTest()
    assertEquals(AiTestUiState.Idle, vm.test.value)

    gate.complete(Unit)
    advanceUntilIdle()

    // 旧结论不许落回来
    assertEquals(AiTestUiState.Idle, vm.test.value)
  }

  /**
   * 「载入 .env」按钮的显示条件：`.env` 三项齐全才有值。空配置时给 null ——
   * 界面据此不显示按钮，避免一个点了没反应的胶囊。
   */
  @Test
  fun envDefaultsExposedOnlyWhenFilled() = runTest(mainDispatcherRule.testDispatcher) {
    val ok = FakeTester(AiTestResult.Ok(200))

    assertNull(ProfileViewModel(FakeAiConfigRepository(), ok, AiConfig()).envDefaults)

    val env = AiConfig("https://env.example", "k", "m")
    assertEquals(env, ProfileViewModel(FakeAiConfigRepository(), ok, env).envDefaults)
  }
}
