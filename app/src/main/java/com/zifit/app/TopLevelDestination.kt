/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey

/**
 * 底部导航栏的五个一级入口。声明顺序即展示顺序：
 * 动作库 → 训练 → 饮食 → 食材库 → 我的（左侧为训练域，右侧为饮食域）。
 */
enum class TopLevelDestination(
  val key: NavKey,
  @param:StringRes val labelRes: Int,
  @param:DrawableRes val iconRes: Int,
) {
  EXERCISE_LIBRARY(ExerciseLibrary, R.string.tab_exercise, R.drawable.ic_tab_exercise),
  TRAINING(Training, R.string.tab_training, R.drawable.ic_tab_training),
  DIET(Diet, R.string.tab_diet, R.drawable.ic_tab_diet),
  FOOD_LIBRARY(FoodLibrary, R.string.tab_food, R.drawable.ic_tab_food),
  PROFILE(Profile, R.string.tab_profile, R.drawable.ic_tab_profile);

  companion object {
    /** 冷启动落在哪个标签页。 */
    val START: TopLevelDestination = DIET
  }
}
