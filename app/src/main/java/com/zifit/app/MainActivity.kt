/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.theme.ZiFitTheme
import com.zifit.app.ui.appearance.UiSettingsViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /*
     * 边缘到边：系统栏完全透明，画面铺到屏幕边缘。
     *
     * 系统栏**图标**的明暗不在这里定 —— 它取决于用户在「用户界面 → 主题模式」里
     * 选的是不是深色，而那个偏好要等 Compose 起来才知道。这里用 `auto` 给一个
     * "跟随系统深色"的初始猜测（多数情况下与用户选择一致，少一次闪），
     * 真正的值由 `ZiFitTheme` 在首次组合后按调色板覆盖。
     */
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    )

    setContent {
      /*
       * 主题参数在**组合根**读一次就往下传（`ZiFitTheme` 因此是纯函数）。
       * 这里拿到的 UiSettingsViewModel 与设置页、根导航拿到的是同一个实例
       * （Activity 作 ViewModelStoreOwner + 同一 key），所以改了设置立刻重建主题。
       */
      val uiViewModel: UiSettingsViewModel = viewModel(factory = UiSettingsViewModel.factory(this))
      val preferences by uiViewModel.preferences.collectAsStateWithLifecycle()

      ZiFitTheme(mode = preferences.themeMode, accent = preferences.accent) { MainNavigation() }
    }
  }
}
