/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.appearance

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zifit.app.R
import com.zifit.app.theme.AccentColor
import com.zifit.app.theme.ThemeMode
import com.zifit.app.ui.common.backdropBlurSupported

/*
 * 界面外观偏好的**唯一文案出口**。
 *
 * 两处消费这份文案：用户界面设置页本身（选项胶囊 / 取色点的无障碍标签），
 * 以及设置页那一行的副标题（"彩色·浅 · 主题色蓝 · 毛玻璃开"）。
 * 放在这里而不是各自写一份：同一个枚举在两个页面上被叫成两个名字，
 * 是那种"改了一处、另一处还是旧说法"的经典来源。
 */

@get:StringRes
internal val ThemeMode.labelRes: Int
  get() =
    when (this) {
      ThemeMode.COLORFUL_LIGHT -> R.string.theme_mode_colorful_light
      ThemeMode.NEUTRAL_LIGHT -> R.string.theme_mode_light
      ThemeMode.COLORFUL_DARK -> R.string.theme_mode_colorful_dark
      ThemeMode.NEUTRAL_DARK -> R.string.theme_mode_dark
    }

@get:StringRes
internal val AccentColor.labelRes: Int
  get() =
    when (this) {
      AccentColor.BLUE -> R.string.theme_accent_blue
      AccentColor.MINT -> R.string.theme_accent_mint
      AccentColor.VIOLET -> R.string.theme_accent_violet
      AccentColor.CORAL -> R.string.theme_accent_coral
      AccentColor.ROSE -> R.string.theme_accent_rose
    }

/**
 * 毛玻璃的当前状态文案。
 *
 * 三态而不是两态：**系统不支持时显示「不可用」而不是「开」** ——
 * 开关确实存着"开"，但那条渲染分支在 API < 31 上永远不会走，
 * 照着开关值显示"开"就是在骗人（这也是 [backdropBlurSupported] 存在的意义）。
 * 用户**主动关掉**时优先显示「关」：那是他的意图，比系统的能力限制更该被看到。
 */
@Composable
internal fun blurStateLabel(enabled: Boolean): String =
  stringResource(
    when {
      !enabled -> R.string.ui_state_off
      !backdropBlurSupported -> R.string.ui_state_unsupported
      else -> R.string.ui_state_on
    }
  )
