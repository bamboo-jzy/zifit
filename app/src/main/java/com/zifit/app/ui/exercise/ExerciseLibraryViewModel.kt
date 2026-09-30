/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.exercise

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zifit.app.data.repdb.Exercise
import com.zifit.app.data.repdb.ExerciseCategory
import com.zifit.app.data.repdb.ExerciseRepository
import com.zifit.app.data.repdb.ExerciseTaxonomy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class ExerciseLibraryUiState(
  val loading: Boolean = true,
  /** assets 里没有数据集 —— 提示用户去跑 tools/fetch_repdb.sh，而不是显示空白页。 */
  val dataMissing: Boolean = false,
  val query: String = "",
  /** 选中的筛选分类（中文标签）；null = 全部。 */
  val selectedCategory: String? = null,
  /** 可供筛选的分类，只含**当前数据集里真有动作的**那些，已按展示顺序排好。 */
  val categories: List<String> = emptyList(),
  val exercises: List<Exercise> = emptyList(),
  val total: Int = 0,
) {
  val isFiltered: Boolean
    get() = query.isNotBlank() || selectedCategory != null
}

/** 已加载的静态数据集：601 条随包数据，载入后不再变化。 */
private data class ExerciseSource(
  val loading: Boolean = true,
  val dataMissing: Boolean = false,
  val exercises: List<Exercise> = emptyList(),
  val categories: List<String> = emptyList(),
)

internal class ExerciseLibraryViewModel(private val repository: ExerciseRepository) : ViewModel() {

  private val source = MutableStateFlow(ExerciseSource())
  private val query = MutableStateFlow("")
  private val category = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ExerciseLibraryUiState> =
    combine(source, query, category) { src, q, cat ->
        val needle = q.trim().lowercase()
        val filtered =
          src.exercises.filter { exercise ->
            (cat == null || cat in exercise.categories) &&
              (needle.isEmpty() || exercise.searchHaystack.contains(needle))
          }
        ExerciseLibraryUiState(
          loading = src.loading,
          dataMissing = src.dataMissing,
          query = q,
          selectedCategory = cat,
          categories = src.categories,
          // 按难度由易到难：入门 → 进阶 → 高阶。
          // `sortedBy` 是**稳定排序**，同档内保持数据集原序（按英文名 A→Z），
          // 因此排序结果可预期，也不会把同一肌群的动作打散成随机序。
          // 顺序取自 ExerciseTaxonomy 的词表声明序，别直接对中文标签排序（那是码位序）。
          exercises = filtered.sortedBy { ExerciseTaxonomy.difficultyRank(it.difficulty) },
          total = src.exercises.size,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

  init {
    viewModelScope.launch {
      val loaded = runCatching { repository.all() }.getOrNull()
      source.value =
        ExerciseSource(
          loading = false,
          dataMissing = loaded == null,
          exercises = loaded.orEmpty(),
          // 只留真有动作的分类：空分类的胶囊点进去是空列表，不如不显示。
          // 顺序取 ExerciseCategory.palette（声明序），不是中文字符串序（那是乱序）。
          categories = ExerciseCategory.sort(loaded.orEmpty().flatMap { it.categories }.distinct()),
        )
    }
  }

  fun onQueryChange(value: String) {
    query.value = value
  }

  /** 传入 null 表示「全部」；再次点击已选中的分类 = 取消选择。 */
  fun onCategorySelect(value: String?) {
    category.value = if (category.value == value) null else value
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer { ExerciseLibraryViewModel(ExerciseRepository.get(context)) }
    }
  }
}
