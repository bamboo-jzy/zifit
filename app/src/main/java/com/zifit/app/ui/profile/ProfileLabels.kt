/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zifit.app.R
import com.zifit.app.data.user.Goal
import com.zifit.app.data.user.UserProfile

/**
 * 目标枚举与资料的展示文案。
 *
 * 单独成文件是因为 [ProfileScreen]（首页副标题）与 [ProfileEditScreen]（目标胶囊）
 * **必须说同一句话** —— 两处各写一份 `when`，加一个目标时必然会漏改一处。
 */

/** 目标的人话。 */
@Composable
internal fun goalLabel(goal: Goal): String =
  stringResource(
    when (goal) {
      Goal.MUSCLE_GAIN -> R.string.profile_goal_muscle_gain
      Goal.FAT_LOSS -> R.string.profile_goal_fat_loss
      Goal.MAINTAIN -> R.string.profile_goal_maintain
      Goal.STRENGTH -> R.string.profile_goal_strength
      Goal.HEALTH -> R.string.profile_goal_health
    }
  )

/**
 * 首页头部的副标题：`增肌 · 24 岁`。
 *
 * 两样都没填时返回 **null**（调用方整行不渲染）—— 这一行不是标题的占位，
 * 有内容才叫信息，没内容就是一行噪音。曾经在这里给过一句引导语
 * 「点头像右侧的设置，填好个人资料与 AI 接入」，用户看了直说是废话，已删。
 */
@Composable
internal fun profileSubtitle(profile: UserProfile): String? {
  val parts =
    listOfNotNull(
      profile.goal?.let { goalLabel(it) },
      profile.age?.let { stringResource(R.string.profile_age_years, it) },
    )
  return parts.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)
}

/** 年月之间用间隔号分隔，与食材行的 `·` 保持一致。 */
private const val SEPARATOR = " · "
