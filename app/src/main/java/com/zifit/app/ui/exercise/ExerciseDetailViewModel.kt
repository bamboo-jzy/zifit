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
import com.zifit.app.data.repdb.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface ExerciseDetailUiState {
  data object Loading : ExerciseDetailUiState

  /** id 在数据集中不存在 —— 数据集升级后旧路由可能指向已删动作，需要显式兜底。 */
  data object NotFound : ExerciseDetailUiState

  data class Ready(val exercise: Exercise) : ExerciseDetailUiState
}

internal class ExerciseDetailViewModel(
  private val repository: ExerciseRepository,
  private val exerciseId: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow<ExerciseDetailUiState>(ExerciseDetailUiState.Loading)
  val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val found = runCatching { repository.byId(exerciseId) }.getOrNull()
      _uiState.value =
        if (found == null) ExerciseDetailUiState.NotFound else ExerciseDetailUiState.Ready(found)
    }
  }

  companion object {
    fun factory(context: Context, exerciseId: String): ViewModelProvider.Factory =
      viewModelFactory {
        initializer { ExerciseDetailViewModel(ExerciseRepository.get(context), exerciseId) }
      }
  }
}
