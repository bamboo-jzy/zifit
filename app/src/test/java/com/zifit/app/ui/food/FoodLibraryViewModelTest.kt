/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.food

import com.zifit.app.MainDispatcherRule
import com.zifit.app.data.food.Food
import com.zifit.app.data.food.FoodOrigin
import com.zifit.app.data.food.FoodRepository
import com.zifit.app.data.food.FoodSearchOutcome
import com.zifit.app.data.food.FoodSearchSource
import com.zifit.app.data.food.FoodSuggestion
import com.zifit.app.data.food.UnavailableFoodSearchSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLibraryViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val dao = FakeFoodDao()
  private val now = 1_700_000_000_000L

  private fun viewModel(searchSource: FoodSearchSource = UnavailableFoodSearchSource) =
    FoodLibraryViewModel(FoodRepository(dao), searchSource, now = { now })

  /** 显式建对象而不是 SAM 简写：`suspend` 成员接口的简写可读性差，也容易踩到类型推断。 */
  private fun source(block: suspend (String) -> FoodSearchOutcome): FoodSearchSource =
    object : FoodSearchSource {
      override suspend fun search(keyword: String): FoodSearchOutcome = block(keyword)
    }

  private fun food(id: Long, name: String, createdAt: Long = 0L) =
    Food(id = id, name = name, createdAt = createdAt)

  @Test
  fun loadsExistingFoods_newestFirst() = runTest(mainDispatcherRule.testDispatcher) {
    dao.setAll(listOf(food(1, "旧", 10L), food(2, "新", 20L)))
    val vm = viewModel()
    val state = vm.uiState.collectForTest(this)

    advanceUntilIdle()

    assertEquals(listOf("新", "旧"), state.value.foods.map { it.name })
    assertEquals(2, state.value.totalFoods)
    assertTrue(!state.value.loading)
  }

  @Test
  fun blankQuery_doesNotHitTheSearchSource() = runTest(mainDispatcherRule.testDispatcher) {
    var calls = 0
    val vm = viewModel(source { calls++; FoodSearchOutcome.Hit(emptyList()) })
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("   ")
    advanceUntilIdle()

    assertEquals(0, calls)
    assertEquals(FoodSearchUiState.Idle, state.value.search)
  }

  @Test
  fun queryFiltersLocalFoodsByName() = runTest(mainDispatcherRule.testDispatcher) {
    dao.setAll(listOf(food(1, "小麦粉", 2L), food(2, "鸡蛋", 1L)))
    val vm = viewModel()
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("鸡")
    advanceUntilIdle()

    assertEquals(listOf("鸡蛋"), state.value.foods.map { it.name })
    // 本地被过滤掉不等于库里没有 —— totalFoods 报的是总数
    assertEquals(2, state.value.totalFoods)
  }

  @Test
  fun queryWithNoLocalMatch_setsNoLocalMatch() = runTest(mainDispatcherRule.testDispatcher) {
    dao.setAll(listOf(food(1, "小麦粉", 1L)))
    val vm = viewModel()
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("龙虾")
    advanceUntilIdle()

    assertTrue(state.value.foods.isEmpty())
    assertTrue(state.value.noLocalMatch)
  }

  /** 查询通道没接上时必须报 Unavailable，界面才不会谎称「没有匹配」。 */
  @Test
  fun unavailableSource_surfacesUnavailable() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = viewModel()
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("牛奶")
    advanceUntilIdle()

    assertEquals(FoodSearchUiState.Unavailable, state.value.search)
  }

  /**
   * **本组最要紧的一条**：`Hit(空列表)` 表示「查过了，确实没有」，
   * 与 `Unavailable`（压根没查成）必须是两种状态。合成一个空列表就是对用户说谎。
   */
  @Test
  fun emptyHit_isNotTheSameAsUnavailable() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = viewModel(source { FoodSearchOutcome.Hit(emptyList()) })
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("不存在的食材")
    advanceUntilIdle()

    assertEquals(FoodSearchUiState.Hit(emptyList()), state.value.search)
  }

  @Test
  fun hitCarriesSuggestions() = runTest(mainDispatcherRule.testDispatcher) {
    val suggestion = FoodSuggestion(name = "小麦", energyKcal = 338.0, proteinG = 11.9)
    val vm = viewModel(source { FoodSearchOutcome.Hit(listOf(suggestion)) })
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("小麦")
    advanceUntilIdle()

    assertEquals(FoodSearchUiState.Hit(listOf(suggestion)), state.value.search)
  }

  @Test
  fun failureSurfacesReason() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = viewModel(source { FoodSearchOutcome.Failed("网络不可用") })
    val state = vm.uiState.collectForTest(this)

    vm.onQueryChange("小麦")
    advanceUntilIdle()

    assertEquals(FoodSearchUiState.Failed("网络不可用"), state.value.search)
  }

  @Test
  fun addSuggestion_landsInTheLibraryWithOriginAndTimestamp() =
    runTest(mainDispatcherRule.testDispatcher) {
      val vm = viewModel()
      val state = vm.uiState.collectForTest(this)

      vm.addSuggestion(FoodSuggestion(name = "小麦", energyKcal = 338.0))
      advanceUntilIdle()

      assertEquals(1, dao.rows.value.size)
      val stored = dao.rows.value.single()
      assertEquals("小麦", stored.name)
      assertEquals(338.0, stored.energyKcal!!, 1e-9)
      // 「一键加入」进来的，出处必须是 AI 而非手动
      assertEquals(FoodOrigin.AI, stored.origin)
      assertEquals(now, stored.createdAt)
      assertEquals(listOf("小麦"), state.value.foods.map { it.name })
    }

  @Test
  fun saveInsertsWhenIdIsZero_andUpdatesOtherwise() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = viewModel()
    vm.uiState.collectForTest(this)

    vm.save(food(0L, "手动录入"))
    advanceUntilIdle()
    assertEquals(1, dao.rows.value.size)
    val inserted = dao.rows.value.single()
    assertTrue(inserted.id != 0L)

    vm.save(inserted.copy(name = "改过名"))
    advanceUntilIdle()
    assertEquals(1, dao.rows.value.size)
    assertEquals("改过名", dao.rows.value.single().name)
  }

  @Test
  fun deleteRemovesTheRow() = runTest(mainDispatcherRule.testDispatcher) {
    val vm = viewModel()
    val state = vm.uiState.collectForTest(this)

    vm.save(food(0L, "要删的"))
    advanceUntilIdle()
    vm.delete(dao.rows.value.single())
    advanceUntilIdle()

    assertTrue(dao.rows.value.isEmpty())
    assertTrue(state.value.foods.isEmpty())
  }
}
