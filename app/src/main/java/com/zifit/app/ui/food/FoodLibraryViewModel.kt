/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.food

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zifit.app.data.food.Food
import com.zifit.app.data.food.FoodRepository
import com.zifit.app.data.food.FoodSearchOutcome
import com.zifit.app.data.food.FoodSearchSource
import com.zifit.app.data.food.FoodSuggestion
import com.zifit.app.data.food.UnavailableFoodSearchSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 营养库（下一轮：AI）查询在界面上的四种状态。 */
internal sealed interface FoodSearchUiState {
  /** 关键词为空，没在查。 */
  data object Idle : FoodSearchUiState

  data object Searching : FoodSearchUiState

  /** 查到了。`suggestions` 可能为空 —— 那是「确实没这个食材」。 */
  data class Hit(val suggestions: List<FoodSuggestion>) : FoodSearchUiState

  /** 查询通道还没接上。**与 [Hit] 的空列表严格区分**，否则界面会谎称「没有匹配」。 */
  data object Unavailable : FoodSearchUiState

  /** 查失败（网络 / 解析 / 配额……），`reason` 直接展示给用户。 */
  data class Failed(val reason: String) : FoodSearchUiState
}

internal data class FoodLibraryUiState(
  /** Room 首次发射之前为 true —— 只有这一小段是真在等数据。 */
  val loading: Boolean = true,
  val query: String = "",
  /** 关键词过滤后的**本地**食材。 */
  val foods: List<Food> = emptyList(),
  /** 本地食材总数（不受关键词影响），用于空态话术。 */
  val totalFoods: Int = 0,
  val search: FoodSearchUiState = FoodSearchUiState.Idle,
) {
  /** 关键词非空 = 界面要多显示「营养库」那一段。 */
  val isFiltered: Boolean
    get() = query.isNotBlank()

  /** 搜了关键词，但本地一条都没沾边。 */
  val noLocalMatch: Boolean
    get() = isFiltered && foods.isEmpty()
}

internal class FoodLibraryViewModel(
  private val repository: FoodRepository,
  private val searchSource: FoodSearchSource = UnavailableFoodSearchSource,
  /** 注入时间源，便于测试断言 createdAt 而不依赖真实时钟。 */
  private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

  private val query = MutableStateFlow("")
  private val search = MutableStateFlow<FoodSearchUiState>(FoodSearchUiState.Idle)

  /** `null` = Room 还没吐出第一条 —— 用它把「加载中」和「加载完但是空的」分开。 */
  private val stored: StateFlow<List<Food>?> =
    repository
      .observeAll()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  val uiState: StateFlow<FoodLibraryUiState> =
    combine(stored, query, search) { foods, q, remote ->
        val needle = q.trim()
        FoodLibraryUiState(
          loading = foods == null,
          query = q,
          foods =
            foods.orEmpty().filter {
              needle.isEmpty() || it.name.contains(needle, ignoreCase = true)
            },
          totalFoods = foods.orEmpty().size,
          search = remote,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodLibraryUiState())

  init {
    viewModelScope.launch {
      @OptIn(FlowPreview::class)
      query
        // 防抖：下一轮接上 AI 后，不能让每个按键都打一次请求。
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        // 关键词一变就取消上一次查询：旧请求的结果不能覆盖新关键词的结果。
        .collectLatest { keyword ->
          if (keyword.isBlank()) {
            search.value = FoodSearchUiState.Idle
          } else {
            search.value = FoodSearchUiState.Searching
            search.value =
              when (val outcome = searchSource.search(keyword)) {
                is FoodSearchOutcome.Hit -> FoodSearchUiState.Hit(outcome.suggestions)
                FoodSearchOutcome.Unavailable -> FoodSearchUiState.Unavailable
                is FoodSearchOutcome.Failed -> FoodSearchUiState.Failed(outcome.reason)
              }
          }
        }
    }
  }

  fun onQueryChange(value: String) {
    query.value = value
  }

  /** 「一键加入食材库」：把查到的候选落库。 */
  fun addSuggestion(suggestion: FoodSuggestion) {
    viewModelScope.launch { repository.add(suggestion.toFood(now())) }
  }

  /** 新建或编辑：`id == 0` 走 insert，否则 update。 */
  fun save(food: Food) {
    viewModelScope.launch {
      if (food.id == 0L) repository.add(food) else repository.update(food)
    }
  }

  fun delete(food: Food) {
    viewModelScope.launch { repository.delete(food) }
  }

  companion object {
    private const val SEARCH_DEBOUNCE_MS = 300L

    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer { FoodLibraryViewModel(FoodRepository.get(context)) }
    }
  }
}
