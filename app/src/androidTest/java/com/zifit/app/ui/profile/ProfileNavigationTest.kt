/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.activity.ComponentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zifit.app.MainNavigation
import com.zifit.app.R
import com.zifit.app.data.ui.UiPreferencesRepository
import com.zifit.app.data.user.UserProfileRepository
import com.zifit.app.theme.AccentColor
import com.zifit.app.theme.ThemeMode
import com.zifit.app.theme.ZiFitTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 「我的」标签的下钻路径：首页 → 设置 → 个人资料 / AI 接入 / 用户界面 / 关于。
 *
 * 与 `BottomNavigationTest` 分开：那边管五标签骨架，这边管「我的」内部的路由。
 * 定位一律用 `contentDescription`（图标按钮）与 `text`（可见文案），
 * 不依赖坐标 —— 坐标会随字号与屏幕变化。
 *
 * ⚠️ **不要写死「未设置」这类占位文案**：仪器测试跑在开发者自己的手机上，
 * 那份 `user_profile` 里可能已经有真名真生日，写死就会在**有数据时反而红**（实测踩过）。
 * 一律按「真值，空则占位」的规则现场算出期望值 —— 与 `ProfileScreen` /
 * `ProfileEditScreen` 的渲染规则同源，两边一起变。
 */
class ProfileNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun text(resId: Int): String = composeTestRule.activity.getString(resId)

  /** 每次现取，不缓存 —— 页面刚保存过的值也必须读到（见 [UserProfileRepository]）。 */
  private fun currentProfile() = UserProfileRepository.get(composeTestRule.activity).profile.value

  private fun openProfile() {
    composeTestRule.onNodeWithContentDescription(text(R.string.tab_profile)).performClick()
  }

  private fun openSettings() {
    openProfile()
    composeTestRule.onNodeWithContentDescription(text(R.string.profile_open_settings)).performClick()
  }

  @Before
  fun setup() {
    /*
     * 与 `MainActivity` 一样把偏好喂给主题（少一层 ViewModel，测试里不需要）。
     * 照抄 `ZiFitTheme { … }` 会使主题永远停在默认值上，
     * 于是"主题参数没接上"这类 bug 在测试里看不见。
     */
    val repository = UiPreferencesRepository.get(composeTestRule.activity)
    composeTestRule.setContent {
      val preferences by repository.preferences.collectAsStateWithLifecycle()
      ZiFitTheme(mode = preferences.themeMode, accent = preferences.accent) { MainNavigation() }
    }
  }

  @Test
  fun profileHome_hasNoTitle_showsNameAndSettingsIcon() {
    openProfile()
    // 首页刻意没有「个人中心」这类标题：头像 + 用户名 + 设置图标就是它自己。
    // 名字可能已经被填过，所以期望值 = 真名，空则占位（见类注释）
    val name = currentProfile().name.ifBlank { text(R.string.profile_name_unset) }
    composeTestRule.onNodeWithText(name).assertIsDisplayed()
    composeTestRule
      .onNodeWithContentDescription(text(R.string.profile_open_settings))
      .assertIsDisplayed()
  }

  @Test
  fun settings_hasFourRows() {
    openSettings()
    composeTestRule.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.settings_profile)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.profile_ai)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.settings_ui)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.profile_about)).assertIsDisplayed()
  }

  @Test
  fun settingsProfile_opensEditor() {
    openSettings()
    composeTestRule.onNodeWithText(text(R.string.settings_profile)).performClick()
    composeTestRule.onNodeWithText(text(R.string.profile_field_name)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.profile_field_goal)).assertIsDisplayed()
    // 保存键在**页头右上角**，不在页面底部 —— 底部那颗会被悬浮 dock 压住点不着
    composeTestRule.onNodeWithText(text(R.string.profile_save)).assertIsDisplayed()
  }

  @Test
  fun editor_birthdayIsPickerNotTextField() {
    openSettings()
    composeTestRule.onNodeWithText(text(R.string.settings_profile)).performClick()
    // 生日只读，由日历选择器赋值。期望值同样按「真值，空则占位」现场算（见类注释）
    val shown =
      currentProfile().birthday.ifBlank { text(R.string.profile_birthday_unset) }
    composeTestRule.onNodeWithText(shown).assertIsDisplayed()
    // 占位 / 真值文案本身不是热区，**可点的是它所在的那一行**。
    // ⚠️ 必须 `useUnmergedTree = true`：`clickable` 会**合并后代语义**，
    // 在默认（合并后）的树里那段文字已被并入行节点自身、不再是它的「后代」，
    // `hasAnyDescendant` 于是一无所获 —— 报错原文也会提示补这个参数。
    composeTestRule
      .onNode(hasClickAction() and hasAnyDescendant(hasText(shown)), useUnmergedTree = true)
      .performClick()
    composeTestRule.onNodeWithText(text(R.string.picker_confirm)).assertIsDisplayed()
  }

  @Test
  fun settingsAi_opensConfigPage() {
    openSettings()
    composeTestRule.onNodeWithText(text(R.string.profile_ai)).performClick()
    composeTestRule.onNodeWithText(text(R.string.ai_editor_title)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.ai_editor_api_key)).assertIsDisplayed()
  }

  @Test
  fun settingsAbout_opensAboutPage() {
    openSettings()
    composeTestRule.onNodeWithText(text(R.string.profile_about)).performClick()
    // 许可声明与 RepDB 署名必须真的渲染出来（GPL §5(d) / RepDB Term 2 的硬要求）
    composeTestRule.onNodeWithText(text(R.string.profile_about_copyright)).assertIsDisplayed()
  }

  @Test
  fun settingsUi_opensUiPage_andThemeChoiceAppliesImmediately() {
    openSettings()
    composeTestRule.onNodeWithText(text(R.string.settings_ui)).performClick()
    // 三项：主题模式 / 主题色 / 毛玻璃模糊（「界面动效」已按要求去掉）
    composeTestRule.onNodeWithText(text(R.string.ui_theme_mode)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.ui_theme_color)).assertIsDisplayed()
    composeTestRule.onNodeWithText(text(R.string.ui_blur)).assertIsDisplayed()

    /*
     * 主题模式是四档 2×2（{彩色, 中性} × {浅, 深}）。四档必须**同时可见** ——
     * 胶囊容器从 `Row` 换成 FlowRow 之后，唯一会"静默退化成三档"的方式
     * 就是换行算错、把最后一档推出了卡片。断言钉住这一点。
     */
    listOf(
        R.string.theme_mode_colorful_light,
        R.string.theme_mode_light,
        R.string.theme_mode_colorful_dark,
        R.string.theme_mode_dark,
      )
      .forEach { composeTestRule.onNodeWithText(text(it)).assertIsDisplayed() }

    val repository = UiPreferencesRepository.get(composeTestRule.activity)
    val beforeMode = repository.preferences.value.themeMode
    val beforeAccent = repository.preferences.value.accent
    try {
      // 选「深色」（中性深色那一档）：这类设置**没有"保存"这一步**，偏好必须当场变
      composeTestRule.onNodeWithText(text(R.string.theme_mode_dark)).performClick()
      composeTestRule.waitForIdle()
      assertTrue(
        "选中主题模式后偏好应立刻改变",
        repository.preferences.value.themeMode == ThemeMode.NEUTRAL_DARK,
      )

      /*
       * 取色点靠 **contentDescription**（颜色名）定位，按 `text` 找不到 ——
       * 它里面没有文字。这也是 [com.zifit.app.ui.common.SwatchRow] 给每个点挂名字的原因。
       */
      composeTestRule
        .onNodeWithContentDescription(text(R.string.theme_accent_rose))
        .performClick()
      composeTestRule.waitForIdle()
      assertTrue(
        "点取色点后主题色应立刻改变",
        repository.preferences.value.accent == AccentColor.ROSE,
      )
    } finally {
      /*
       * ⚠️ 复原：仪器测试跑在开发者自己的手机上，改完必须改回去，
       * 否则每跑一次测试就把人家的真实偏好翻一次面。
       * 放 `finally` 里 —— 断言失败时也要还原，不然红一次就永久改了手机上的设置。
       */
      repository.setThemeMode(beforeMode)
      repository.setAccent(beforeAccent)
    }
  }
}
