/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 悬浮 dock 的尺寸，**全项目唯一来源** —— 导航层画它、页面层避让它，
 * 两处必须用同一组数字，否则 dock 与内容会错位。
 */

/** 单个 dock 项的触摸区宽度：够大便于点按，图标本身仍为 24dp。 */
internal val DockItemWidth = 56.dp

/** 单个 dock 项的触摸区高度。 */
internal val DockItemHeight = 44.dp

/** dock 四周的外边距，形成"悬浮"观感。 */
internal val DockMargin = 12.dp

/**
 * 悬浮 dock 占用的垂直空间：dock 本体 + 上下外边距。
 *
 * ⚠️ 页面**不能**用 `Modifier.padding(bottom = ...)` 避让 dock。
 * `padding` 会缩小可滚动容器的尺寸，内容被**硬截**在 dock 之上，
 * 于是永远滚不到 dock 下面 —— 那层空出来的背景就成了用户眼中的「遮挡层」，
 * 毛玻璃也就无内容可"透"。
 *
 * 正确做法是把它交给可滚动容器的 `contentPadding(bottom = ...)`：
 * 内容可以滚进这块留白并被 dock 覆盖，最后一条也能被滚出来看清。
 */
internal val DockClearance = DockItemHeight + 16.dp + DockMargin * 2

/** [DockClearance] + 系统导航区高度。可滚动内容用它作为 `contentPadding` 的 bottom。 */
@Composable
internal fun dockContentInset(): Dp =
  DockClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
