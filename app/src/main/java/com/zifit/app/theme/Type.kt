/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 只覆盖本项目实际用到的几个层级：大标题略加粗以压住浅色背景，
 * 正文收紧字距（默认 0.5sp 在中文下偏散）。
 */
val Typography =
  Typography(
    headlineMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
      ),
    /**
     * **二级页面的标题**（返回箭头右边那一个），由 [com.zifit.app.ui.common.PageHeader] 使用。
     *
     * 取 20sp 而不是 Material 默认的 22sp（更不是 headlineSmall 的 24sp）：
     * 页头标题与下面的行标题（`titleMedium` 17sp）只差 3sp 时，两者之间的层级
     * 靠的是留白，不是字号差 —— 24sp 的标题在 6.8 寸屏上比内容大出一整圈，
     * 压着整页往下沉。用户 2026-09-30 明确要求"小一点"，全站二级页一起降。
     *
     * `headlineSmall` 保持 Material 默认（24sp）：它现在只用于「我的」首页的用户名，
     * 那是**页面主角**，不是页头标题，两者不该共用一档。
     */
    titleLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
      ),
  )
