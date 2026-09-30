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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zifit.app.theme.BackgroundBottom
import com.zifit.app.theme.BackgroundMid
import com.zifit.app.theme.BackgroundTop
import com.zifit.app.theme.BlobAlpha
import com.zifit.app.theme.BlobCoral
import com.zifit.app.theme.BlobLilac
import com.zifit.app.theme.BlobMint
import com.zifit.app.theme.BlobSky

/**
 * App 背景：基底渐变打底 + 六团柔和光斑。具体色号取自当前调色板（见 `theme/Color.kt`），
 * 于是同一套几何在三种主题模式下分别表现为"冷灰蓝 + 彩色斑"（彩色）、
 * "近白 + 银色明暗"（浅色）、"暗底 + 透出的彩光"（深色）。
 *
 * 光斑用径向渐变收边（中心有色、边缘透明），无需模糊即可得到柔和过渡，
 * 全 API 版本表现一致（`Modifier.blur` 需 API 31+）。
 * 这一层是玻璃面板"透出磨砂感"的唯一来源 —— 改色相或降低饱和度会直接让玻璃失去存在感，
 * 所以浅色模式的"退彩"靠的是**降低整体透明度 + 统一色相**，而不是把光斑抹掉。
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundMid, BackgroundBottom)))
  ) {
    blobs().forEach { Blob(it) }
  }
}

private data class BlobSpec(val color: Color, val diameter: Dp, val x: Dp, val y: Dp, val alpha: Float)

/**
 * 位置相对屏幕左上角，尺寸需小于半个屏宽，否则会退化成整屏渐变、失去"玻璃背后有东西"的观感。
 *
 * 每团的 α 都乘上 [BlobAlpha]（调色板里的整体系数）：浅色模式靠它一次性退彩，
 * 不必逐团改数，也就不会出现"改了五团、漏了一团"。
 */
@Composable
private fun blobs(): List<BlobSpec> {
  val scale = BlobAlpha
  return listOf(
    // 氛围底：大而极淡，把整体色调统一到冷调
    BlobSpec(BlobSky, 520.dp, (-160).dp, 120.dp, 0.22f * scale),
    // 色彩焦点：小而浓，分散在四角与中段
    BlobSpec(BlobMint, 300.dp, (-70).dp, (-90).dp, 0.62f * scale),
    BlobSpec(BlobSky, 260.dp, 170.dp, 30.dp, 0.58f * scale),
    BlobSpec(BlobLilac, 330.dp, (-90).dp, 300.dp, 0.50f * scale),
    BlobSpec(BlobCoral, 270.dp, 180.dp, 520.dp, 0.42f * scale),
    BlobSpec(BlobMint, 250.dp, (-50).dp, 690.dp, 0.32f * scale),
  )
}

@Composable
private fun Blob(spec: BlobSpec) {
  Box(
    modifier =
      Modifier.offset(x = spec.x, y = spec.y)
        .size(spec.diameter)
        .background(
          Brush.radialGradient(
            listOf(
              spec.color.copy(alpha = spec.alpha),
              spec.color.copy(alpha = spec.alpha * 0.4f),
              Color.Transparent,
            )
          )
        )
  )
}
