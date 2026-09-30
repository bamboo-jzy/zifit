/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.ui

import com.zifit.app.theme.AccentColor
import com.zifit.app.theme.ThemeMode

/**
 * 界面偏好（「我的 → 设置 → 用户界面」）。
 *
 * 三项里两类东西，含义不同，别混着理解：
 *
 * - [themeMode] / [accent] 是**配色参数**，直接决定画成什么样；
 * - [glassBlur] 是**渲染路径开关** —— 关掉不是"把某个颜色调淡"，而是走另一条
 *   已经存在的分支：根节点不再录制整屏图形层，dock 退回"高不透明度填充"
 *   （与 API < 31 的兜底路径同一条）。省掉每帧一次离屏录制，低端机 / 省电场景有意义。
 *
 * 默认值 = **彩色·浅 + 蓝 + 毛玻璃开**，也就是 2026-09-30 之前那一版的样子：
 * 加了可选项之后，默认仍应是"改动前看到的那个界面"，否则用户会以为 App 换了张脸。
 *
 * ⚠️ 枚举定义在 `theme` 包而不是这里：它们的唯一含义就是"渲染参数"，
 * 数据层只负责**存取**，不该自己解释颜色。
 */
data class UiPreferences(
  /** 底部栏实时模糊背后画面（毛玻璃）。Android 12 以下无 `RenderEffect`，无论如何都是实底。 */
  val glassBlur: Boolean = true,
  /** 主题模式：彩色·浅 / 浅色 / 彩色·深 / 深色（见 [ThemeMode] 的 2×2 表）。 */
  val themeMode: ThemeMode = ThemeMode.Default,
  /** 主题色：决定全站"高亮"（底部栏选中、筛选胶囊、热量数字、新建、设置图标等）。 */
  val accent: AccentColor = AccentColor.Default,
)
