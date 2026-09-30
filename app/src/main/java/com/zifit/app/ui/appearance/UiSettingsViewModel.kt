/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.appearance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zifit.app.data.ui.UiPreferences
import com.zifit.app.data.ui.UiPreferencesRepository
import com.zifit.app.theme.AccentColor
import com.zifit.app.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * 界面偏好的 ViewModel。
 *
 * 薄得几乎只有转发 —— 这是对的：这里没有业务规则可编排，
 * 加进来只是为了让 **UI 层不直接摸 Repository**（AGENTS.md §4）。
 *
 * 与 [com.zifit.app.ui.profile.ProfileViewModel] 分开：一个管"我连哪个模型"，
 * 一个管"界面怎么画"，两件不相干的事不塞进同一个 ViewModel。
 */
internal class UiSettingsViewModel(private val repository: UiPreferencesRepository) : ViewModel() {

  val preferences: StateFlow<UiPreferences> = repository.preferences

  fun setGlassBlur(enabled: Boolean) = repository.setGlassBlur(enabled)

  fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)

  fun setAccent(accent: AccentColor) = repository.setAccent(accent)

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      // 统一取 applicationContext：ViewModel 活得比页面久，别攥着 Activity
      initializer { UiSettingsViewModel(UiPreferencesRepository.get(context.applicationContext)) }
    }
  }
}
