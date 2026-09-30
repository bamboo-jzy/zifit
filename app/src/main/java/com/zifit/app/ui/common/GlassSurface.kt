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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.zifit.app.theme.GlassBorderEnd
import com.zifit.app.theme.GlassBorderStart
import com.zifit.app.theme.GlassFillEnd
import com.zifit.app.theme.GlassFillStart
import com.zifit.app.theme.GlassShadow

/**
 * 毛玻璃面板：半透明白底 + 左上受光的高光描边 + 极淡投影。
 *
 * 透视感来自**背后的柔和光斑**（见 [GlassBackground]），因此不使用模糊：
 * 背景本身没有锐利细节，再叠加实时模糊只会增加开销而无视觉收益。
 */
@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.large,
  contentPadding: PaddingValues = PaddingValues(18.dp),
  /** 是否投影。面板下方若会有内容滚过（如悬浮筛选头），传 false —— 阴影会在内容上压出一条暗带。 */
  elevated: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val fill = Brush.linearGradient(listOf(GlassFillStart, GlassFillEnd))
  val stroke = Brush.linearGradient(listOf(GlassBorderStart, GlassBorderEnd))

  Column(
    modifier =
      modifier
        .then(
          if (elevated) {
            Modifier.shadow(elevation = 16.dp, shape = shape, ambientColor = GlassShadow, spotColor = GlassShadow)
          } else {
            Modifier
          }
        )
        .clip(shape)
        .background(fill)
        .border(width = 1.dp, brush = stroke, shape = shape)
        .padding(contentPadding),
    content = content,
  )
}
