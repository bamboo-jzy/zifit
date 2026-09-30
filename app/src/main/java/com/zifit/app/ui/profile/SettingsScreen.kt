/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

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
import com.zifit.app.theme.IconCoral
import com.zifit.app.theme.IconSky
import com.zifit.app.theme.IconViolet
import com.zifit.app.ui.appearance.UiSettingsViewModel
import com.zifit.app.ui.appearance.blurStateLabel
import com.zifit.app.ui.appearance.labelRes
import com.zifit.app.ui.common.NavRow
import com.zifit.app.ui.common.PageHeader
import com.zifit.app.ui.common.SettingsGroup
import com.zifit.app.ui.common.dockContentInset

/**
 * 设置页：**一张卡片、四行** —— 个人资料 / AI 接入 / 用户界面 / 关于。
 *
 * 版式照用户给的参考图（iOS 分组列表）：浅底上一张白色圆角卡，卡内每行是
 * **行首一个彩色图标 + 标题 + 右侧箭头**，行与行只靠留白分开、不画分隔线。
 *
 * 2026-09-30 从"两张卡"并成**一张**：原先按"要填的 / 只读的"分成两组，
 * 但四行都是**同一类东西**——点进去改一项设置。分组卡的意义是"组内同质、
 * 组间异质"，为四行同类项硬分两组，读者只会去猜这两组的分界到底意味着什么。
 *
 * 图标四个色相（主题色 / 紫 / 陶土橙 / 钢蓝）只做行间扫视定位：
 * 「个人资料」那一行用**主题色**（它本来就是"主色"所在的位置），
 * 其余三个是固定色相，不随主题色变 —— 否则选到某个色号时，几个图标会撞成一片。
 *
 * 每行右侧的副标题显示**当前值**（名字 / 配没配好 / 界面偏好），省得点进去才知道。
 * 三个 ViewModel 都用 Activity 级实例（Navigation 3 的宿主就是 Activity），
 * 与各自的二级页共享同一份状态 —— 保存完返回，这里立刻就是新值。
 */
@Composable
internal fun SettingsScreen(
  onBack: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenAi: () -> Unit,
  onOpenUi: () -> Unit,
  onOpenAbout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val profileViewModel: UserProfileViewModel =
    viewModel(factory = UserProfileViewModel.factory(context))
  val aiViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(context))
  val uiViewModel: UiSettingsViewModel = viewModel(factory = UiSettingsViewModel.factory(context))

  val profile by profileViewModel.profile.collectAsStateWithLifecycle()
  val config by aiViewModel.config.collectAsStateWithLifecycle()
  val ui by uiViewModel.preferences.collectAsStateWithLifecycle()

  Column(modifier = modifier.fillMaxSize()) {
    PageHeader(title = stringResource(R.string.settings_title), onBack = onBack)
    Spacer(Modifier.height(16.dp))

    Column(
      modifier =
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(bottom = dockContentInset()),
    ) {
      SettingsGroup {
        NavRow(
          title = stringResource(R.string.settings_profile),
          subtitle =
            profile.name.ifBlank { stringResource(R.string.settings_profile_subtitle_unset) },
          iconRes = R.drawable.ic_person,
          iconTint = Accent,
          onClick = onOpenProfile,
        )
        NavRow(
          title = stringResource(R.string.profile_ai),
          // 与 AI 接入页里的状态同一套文案，别在设置页另起一套说法
          subtitle =
            stringResource(
              if (config.isConfigured) R.string.profile_ai_status_on
              else R.string.profile_ai_status_off
            ),
          iconRes = R.drawable.ic_ai,
          iconTint = IconViolet,
          onClick = onOpenAi,
        )
        NavRow(
          title = stringResource(R.string.settings_ui),
          // 三项当前值一次报完（主题模式 · 主题色 · 毛玻璃），文案取自 ui/appearance 里
          // 那份唯一出口，别在这里另拼一套说法
          subtitle =
            stringResource(
              R.string.settings_ui_subtitle,
              stringResource(ui.themeMode.labelRes),
              stringResource(ui.accent.labelRes),
              blurStateLabel(enabled = ui.glassBlur),
            ),
          iconRes = R.drawable.ic_ui,
          iconTint = IconCoral,
          onClick = onOpenUi,
        )
        NavRow(
          title = stringResource(R.string.profile_about),
          subtitle = stringResource(R.string.settings_about_subtitle),
          iconRes = R.drawable.ic_info,
          iconTint = IconSky,
          onClick = onOpenAbout,
        )
      }
    }
  }
}
