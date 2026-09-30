/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/*
 * 动作库专属的公共件。
 * 与 feature 无关的玻璃控件（胶囊 / 行底 / 分区标题）在 `ui/common/GlassControls.kt`，
 * 别把它们再搬回来 —— 食材库也要用同一套。
 */

/** RepDB 数据集在 assets 内的根目录，与 `tools/fetch_repdb.sh` 的落位一致。 */
private const val REPDB_ASSET_ROOT = "repdb/"

/** 数据集里的相对路径（`images/flat/xxx.webp`）→ Coil 可读的 assets URI。 */
internal fun repDbImageUri(relativePath: String): String =
  "file:///android_asset/$REPDB_ASSET_ROOT$relativePath"

/**
 * 动作插画。图片随包携带，故无网络加载、无错误重试策略，只留空态占位。
 *
 * RepDB 的 flat 插画是**带底色的方图**，必须用 [ContentScale.Fit] 完整显示 ——
 * 裁切会切掉人物动作的关键部分。
 */
@Composable
internal fun ExerciseImage(
  relativePath: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Fit,
) {
  if (relativePath == null) {
    Box(modifier = modifier.background(Color.White.copy(alpha = 0.4f)))
    return
  }
  AsyncImage(
    model = repDbImageUri(relativePath),
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
  )
}
