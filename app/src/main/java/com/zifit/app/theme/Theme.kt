/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** 比 Material 默认更圆：玻璃面板靠大圆角与高光描边立住形状。 */
private val ZiFitShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

/**
 * 把 [ZiFitPalette] 映射成 Material 色板。
 *
 * `primary` **跟着主题色走**：本项目自己画的控件都直接取 [Accent]，但 Material 自带的
 * （滑块、选中框、将来可能引入的组件）默认吃 `primary` —— 不给它主题色，
 * 就会出现"自己画的跟着变、系统组件还是蓝的"这种半吊子状态。
 */
private fun colorSchemeFor(palette: ZiFitPalette) =
  if (palette.isDark) {
    darkColorScheme(
      primary = palette.accent,
      onPrimary = Color.White,
      primaryContainer = palette.brandContainer,
      onPrimaryContainer = palette.onBrandContainer,
      secondary = palette.brand,
      onSecondary = Color.White,
      background = palette.backgroundTop,
      onBackground = palette.ink,
      surface = palette.surface,
      onSurface = palette.ink,
      surfaceVariant = palette.surfaceVariant,
      onSurfaceVariant = palette.inkMuted,
      outline = palette.outline,
      outlineVariant = palette.outlineVariant,
    )
  } else {
    lightColorScheme(
      primary = palette.accent,
      onPrimary = Color.White,
      primaryContainer = palette.brandContainer,
      onPrimaryContainer = palette.onBrandContainer,
      secondary = palette.brand,
      onSecondary = Color.White,
      background = palette.backgroundTop,
      onBackground = palette.ink,
      surface = palette.surface,
      onSurface = palette.ink,
      surfaceVariant = palette.surfaceVariant,
      onSurfaceVariant = palette.inkMuted,
      outline = palette.outline,
      outlineVariant = palette.outlineVariant,
    )
  }

/**
 * 全站主题：毛玻璃配色 + 字体 + 形状。
 *
 * [mode] 与 [accent] 来自「我的 → 设置 → 用户界面」，由 `MainActivity` 从
 * `UiPreferencesRepository` 读出后传进来 —— **主题在这里是纯函数**（给什么画什么），
 * 不自己去摸偏好存储：那样既违反分层，也会让"当前主题是什么"变得没法从调用处看出来。
 *
 * ⚠️ 主题**不跟随系统深色**：深色是用户在本页显式选的档位（彩色·浅 / 浅色 / 彩色·深 / 深色），
 * 系统开关只影响"App 还没起来时系统栏长什么样"。
 */
@Composable
fun ZiFitTheme(
  mode: ThemeMode = ThemeMode.Default,
  accent: AccentColor = AccentColor.Default,
  content: @Composable () -> Unit,
) {
  val palette = remember(mode, accent) { paletteOf(mode, accent) }
  val scheme = remember(palette) { colorSchemeFor(palette) }

  // 系统栏图标的明暗必须跟着主题走：深色主题下仍是深色图标 = 状态栏那几个字消失
  val view = LocalView.current
  val dark = palette.isDark
  if (!view.isInEditMode) {
    SideEffect {
      val window = view.context.findActivity()?.window ?: return@SideEffect
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
      }
    }
  }

  CompositionLocalProvider(LocalZiFitPalette provides palette) {
    MaterialTheme(
      colorScheme = scheme,
      typography = Typography,
      shapes = ZiFitShapes,
      content = content,
    )
  }
}

/**
 * 从 Compose 拿到的 [Context] 里掏出 Activity。
 *
 * 不能直接 `as Activity`：Compose 传下来往往是 `ContextThemeWrapper` 之类的包装，
 * 直接转型会抛 `ClassCastException`。逐层剥包装，剥不到就返回 null（预览等场景，静默跳过）。
 */
private fun Context.findActivity(): Activity? {
  var context: Context? = this
  while (context is ContextWrapper) {
    if (context is Activity) return context
    context = context.baseContext
  }
  return null
}
