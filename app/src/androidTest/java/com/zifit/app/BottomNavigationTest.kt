/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zifit.app.data.user.UserProfileRepository
import com.zifit.app.theme.ZiFitTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 底部 dock 的真机行为测试。
 *
 * dock **只有图标**，标签文案仅作无障碍描述，故定位节点一律用
 * `onNodeWithContentDescription`，而非 `onNodeWithText`。
 */
class BottomNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun text(resId: Int): String = composeTestRule.activity.getString(resId)

  private fun tab(resId: Int) = composeTestRule.onNodeWithContentDescription(text(resId))

  @Before
  fun setup() {
    composeTestRule.setContent { ZiFitTheme { MainNavigation() } }
  }

  @Test
  fun allFiveTabs_areShown() {
    tab(R.string.tab_exercise).assertIsDisplayed()
    tab(R.string.tab_training).assertIsDisplayed()
    tab(R.string.tab_diet).assertIsDisplayed()
    tab(R.string.tab_food).assertIsDisplayed()
    tab(R.string.tab_profile).assertIsDisplayed()
  }

  @Test
  fun tabLabels_areNotRenderedAsText() {
    composeTestRule.onAllNodesWithText(text(R.string.tab_exercise)).assertCountEquals(0)
    composeTestRule.onAllNodesWithText(text(R.string.tab_training)).assertCountEquals(0)
    composeTestRule.onAllNodesWithText(text(R.string.tab_diet)).assertCountEquals(0)
    composeTestRule.onAllNodesWithText(text(R.string.tab_food)).assertCountEquals(0)
    composeTestRule.onAllNodesWithText(text(R.string.tab_profile)).assertCountEquals(0)
  }

  @Test
  fun startTab_isDiet() {
    composeTestRule.onNodeWithText(text(R.string.diet_title)).assertIsDisplayed()
  }

  @Test
  fun tapExerciseLibrary_showsExerciseContent() {
    tab(R.string.tab_exercise).performClick()
    // 动作库刻意无页面标题，改以悬浮搜索框的存在作为"页面已就位"的判据
    composeTestRule.onNodeWithText(text(R.string.exercise_search_hint)).assertIsDisplayed()
  }

  @Test
  fun tapTraining_showsTrainingContent() {
    tab(R.string.tab_training).performClick()
    composeTestRule.onNodeWithText(text(R.string.training_title)).assertIsDisplayed()
  }

  @Test
  fun tapFoodLibrary_showsFoodContent() {
    tab(R.string.tab_food).performClick()
    // 食材库同样刻意无页面标题（不然「食材库」会在 dock 描述与标题上各命中一次），
    // 改以悬浮搜索框的存在作为"页面已就位"的判据。
    composeTestRule.onNodeWithText(text(R.string.food_search_hint)).assertIsDisplayed()
  }

  @Test
  fun tapProfile_showsHomeHeaderAndBodyCard() {
    tab(R.string.tab_profile).performClick()
    // 「我的」首页 2026-09-30 起**没有标题**（原「个人中心」已按用户要求去掉），
    // 改以用户名那行 + 设置图标 + 「身体数据」卡作为"页面已就位"的判据。
    // 更细的下钻路径见 `ui/profile/ProfileNavigationTest`。
    //
    // ⚠️ 用户名**不能写死占位文案**：仪器测试跑在开发者本人的手机上，
    // 那份 `user_profile` 里通常已经有真名 ⇒ 写死「未设置名称」会在**有数据时反而红**
    // （实测：真机存了「竹子是不秋草」，本用例曾因此失败）。按「真值，空则占位」现算，
    // 与 `ProfileScreen` 的渲染规则同源 —— 同款坑见 tasks/lessons.md #66。
    val name =
      UserProfileRepository.get(composeTestRule.activity).profile.value.name
        .ifBlank { text(R.string.profile_name_unset) }
    composeTestRule.onNodeWithText(name).assertIsDisplayed()
    composeTestRule
      .onNodeWithContentDescription(text(R.string.profile_open_settings))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.profile_body)).assertIsDisplayed()
  }
}
