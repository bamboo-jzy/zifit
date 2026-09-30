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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.zifit.app.theme.DockFillEnd
import com.zifit.app.theme.DockFillStart
import com.zifit.app.theme.GlassBorderEnd
import com.zifit.app.theme.GlassBorderStart
import com.zifit.app.theme.GlassFillEnd
import com.zifit.app.theme.GlassFillStart

/**
 * 底部悬浮 dock：**实时模糊**背景 + 半透明白纱 + 高光描边。宽度随内容收缩并居中。
 *
 * 与 [GlassSurface] 的分工：[GlassSurface] 是页面内的玻璃卡片（靠背后光斑透出形成磨砂感，
 * 不做模糊，省开销）；本组件压在内容之上，**必须模糊"透过来"的画面**，
 * 因此底层是 [GlassBlurLayer]（它消费根节点录制的整屏图形层）。
 *
 * ⚠️ 三个致命顺序约束：
 * 1. **模糊层必须是独立的、无子内容的节点**。`renderEffect` 作用于施加它的节点**及其整棵子树** ——
 *    若挂在容器上，图标会被一并糊掉（实测：dock 里只剩一团白色雾，图标消失）。
 *    [GlassBlurLayer] 内部已是空 `Box`，这里直接 `matchParentSize()` 即可。
 * 2. 白纱与描边要叠在模糊层**之上**、图标在**最上层**：模糊层画的是不透明的屏幕副本，
 *    排在它下面的东西全被盖住。
 * 3. **不加投影**。dock 之下会有内容滚过（内容避让交给页面的 contentPadding），
 *    任何投影都会在内容上压出一条暗带 —— 那就是"看不见的遮挡层"的来源。
 *    悬浮感由模糊 + 描边 + 留白本身提供，不依赖阴影。
 */
@Composable
fun GlassDock(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(30.dp),
  contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
  content: @Composable RowScope.() -> Unit,
) {
  /*
   * `LocalBackdrop` 为空有**两种**来源，都走同一条兜底填充（本组件不区分，也不必区分）：
   * 1. API < 31 没有 `RenderEffect`；
   * 2. 用户在「用户界面」里关掉了毛玻璃模糊（此时根节点连录制都省了）。
   *
   * 关掉时改用更实的一档填充（`GlassFill*`）：没有模糊兜底，白纱再透就压不住下面的内容。
   */
  val backdrop = LocalBackdrop.current?.takeIf { backdropBlurSupported }

  val fill =
    if (backdrop != null) {
      Brush.verticalGradient(listOf(DockFillStart, DockFillEnd))
    } else {
      Brush.linearGradient(listOf(GlassFillStart, GlassFillEnd))
    }
  val stroke = Brush.linearGradient(listOf(GlassBorderStart, GlassBorderEnd))

  Box(modifier = modifier.clip(shape)) {
    // 层级：模糊 → 白纱/描边 → 内容（顺序错一处就是"图标消失"或"内容被盖住"）
    GlassBlurLayer(modifier = Modifier.matchParentSize())

    Box(modifier = Modifier.matchParentSize().background(fill).border(width = 1.dp, brush = stroke, shape = shape))

    Row(
      modifier = Modifier.padding(contentPadding),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )
  }
}
