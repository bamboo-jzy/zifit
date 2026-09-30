/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.theme.Accent
import com.zifit.app.theme.AccentColor
import com.zifit.app.theme.ThemeMode
import com.zifit.app.ui.common.ChoiceRow
import com.zifit.app.ui.common.HintText
import com.zifit.app.ui.common.PageHeader
import com.zifit.app.ui.common.SettingsGroup
import com.zifit.app.ui.common.Swatch
import com.zifit.app.ui.common.SwatchRow
import com.zifit.app.ui.common.SwitchRow
import com.zifit.app.ui.common.backdropBlurSupported
import com.zifit.app.ui.common.dockContentInset

/**
 * 用户界面设置页：**两张卡** —— 主题与显示效果。
 *
 * - 「主题」：主题模式（彩色·浅 / 浅色 / 彩色·深 / 深色）+ 主题色（五个色号）。
 * - 「效果」：毛玻璃模糊。
 *
 * 为什么是两张卡：**组内同质、组间异质**（lessons #71）。三项都会改"画成什么样"，
 * 但前两项改的是**配色**、第三项改的是**渲染路径**（关掉毛玻璃是的的确确少录一遍整屏），
 * 一句话能说清的分界线才值得画出来。
 *
 * 三项都**选完立刻生效**，没有"保存"这一步：这类偏好没有填错的可能，
 * 再要一次确认只是多一步。主题与主题色尤其如此 —— 选色靠的就是当下看见的效果。
 *
 * 图标跟着主题色走（[Accent]）：这两行的图标本身就是"当前主题色"的一个样本，
 * 改色时不必翻到别处去看效果。
 */
@Composable
internal fun UiSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: UiSettingsViewModel = viewModel(factory = UiSettingsViewModel.factory(context))
  val preferences by viewModel.preferences.collectAsStateWithLifecycle()

  Column(modifier = modifier.fillMaxSize()) {
    PageHeader(title = stringResource(R.string.settings_ui), onBack = onBack)
    Spacer(Modifier.height(16.dp))

    Column(
      modifier =
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(bottom = dockContentInset()),
      // 与设置页同一档卡间距
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SettingsGroup {
        ChoiceRow(
          title = stringResource(R.string.ui_theme_mode),
          subtitle = stringResource(R.string.ui_theme_mode_desc),
          options = ThemeMode.entries.map { stringResource(it.labelRes) },
          selectedIndex = ThemeMode.entries.indexOf(preferences.themeMode),
          onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
          // 四档按 **2×2** 排，而不是四个挤一行：`ThemeMode.entries` 的顺序就是
          // 「浅组（彩色·浅、浅色）→ 深组（彩色·深、深色）」，每行两个正好一行一组，
          // 分组语义直接看得见。交给宽度自动换行则会随字体与机型漂移成 3+1。
          maxPerRow = 2,
          iconRes = R.drawable.ic_contrast,
          iconTint = Accent,
        )
        SwatchRow(
          title = stringResource(R.string.ui_theme_color),
          subtitle = stringResource(R.string.ui_theme_color_desc),
          swatches =
            AccentColor.entries.map {
              Swatch(color = it.color, label = stringResource(it.labelRes))
            },
          selectedIndex = AccentColor.entries.indexOf(preferences.accent),
          onSelect = { viewModel.setAccent(AccentColor.entries[it]) },
          iconRes = R.drawable.ic_ui,
          iconTint = Accent,
        )
      }

      SettingsGroup {
        SwitchRow(
          title = stringResource(R.string.ui_blur),
          subtitle = stringResource(R.string.ui_blur_desc),
          checked = preferences.glassBlur,
          onCheckedChange = viewModel::setGlassBlur,
          iconRes = R.drawable.ic_blur,
          iconTint = Accent,
        )
      }

      // 低版本兜底要说清楚：开关拨到"开"也不会有任何效果，不解释就是让人以为坏了
      if (!backdropBlurSupported) {
        HintText(text = stringResource(R.string.ui_blur_unsupported))
      }
    }
  }
}
