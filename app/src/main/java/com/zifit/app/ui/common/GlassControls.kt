/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.zifit.app.theme.Accent
import com.zifit.app.theme.GlassRowBorder
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkMuted

/*
 * 通用玻璃控件。**放在 common 而不是某个 feature 包里**：
 * 动作库与食材库都要用同一套胶囊 / 行底样式，任何一方都不该反向依赖另一方。
 */

/**
 * 玻璃胶囊按钮，用于筛选项与轻量动作。
 *
 * 与 dock 一致：**关掉水波纹**，选中态由底色与字色表达，避免 Material 默认的灰方块。
 */
@Composable
fun FilterPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(percent = 50)
  Box(
    modifier =
      modifier
        .clip(shape)
        .background(if (selected) Accent else GlassRowFill)
        .border(1.dp, if (selected) Accent else GlassRowBorder, shape)
        .clickable(interactionSource = null, indication = null, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) Color.White else InkMuted,
    )
  }
}

/**
 * 列表行的轻量玻璃底：**不带投影**。
 * 长列表逐行投影会在滚动时反复离屏绘制，卡顿得不偿失；层次交给描边与间距。
 *
 * ⚠️ `@Composable`：底色与描边取自当前调色板（`GlassRowFill` 是随主题变的 getter），
 * 非组合上下文里拿不到。调用处都在 composable 里，代价只是这个修饰符必须跟在
 * 组合作用域内使用 —— 换来的是深色模式下这些行底会自动跟着变，不必逐个传参。
 */
@Composable
fun Modifier.glassRow(shape: Shape = RoundedCornerShape(20.dp)): Modifier =
  this.clip(shape).background(GlassRowFill).border(1.dp, GlassRowBorder, shape)

/** 页面内的分区标题。 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = modifier,
  )
}
