/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.data.user.UserProfile
import com.zifit.app.theme.Accent
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.common.IconAction
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.dockContentInset

/**
 * 我的标签页首页：**顶部一行 = 头像 + 用户名 + 设置图标**，下面只留身体数据。
 *
 * 原本压在顶部的「个人中心」标题已按用户要求去掉 —— 头像与名字本身就说清了这是谁的页面。
 * AI 接入与关于**不在这里**：它们被收进设置页（[SettingsScreen]），首页只做「我是谁」。
 * 设置图标也不再有白色圆底（第十八轮要求）。
 */
@Composable
fun ProfileScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: UserProfileViewModel =
    viewModel(factory = UserProfileViewModel.factory(context))
  val profile by viewModel.profile.collectAsStateWithLifecycle()

  Column(
    modifier =
      modifier.fillMaxSize().verticalScroll(rememberScrollState())
        // 让开悬浮 dock；加在 verticalScroll 之后才算滚动内容的一部分
        .padding(bottom = dockContentInset()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    ProfileHeader(profile = profile, onOpenSettings = onOpenSettings)

    GlassSurface(modifier = Modifier.fillMaxWidth()) {
      Text(text = stringResource(R.string.profile_body), style = MaterialTheme.typography.titleMedium)
      Text(
        text = stringResource(R.string.profile_body_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

/**
 * 头像 + 用户名 + 设置图标。**没有页面标题** —— 这三样就是标题。
 *
 * 名称没填时显示「未设置名称」而不是留白：留白的分不清是「没填」还是「加载失败」。
 * 名称下面那行只在**真有内容**（目标 / 年龄）时才出现 —— 曾经在空的时候给过一句
 * 「点头像右侧的设置…」的引导语，用户嫌它是废话，已删。
 */
@Composable
private fun ProfileHeader(profile: UserProfile, onOpenSettings: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Avatar(path = profile.avatarPath, size = 56.dp)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = profile.name.ifBlank { stringResource(R.string.profile_name_unset) },
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      profileSubtitle(profile)?.let { subtitle ->
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    IconAction(
      iconRes = R.drawable.ic_settings,
      contentDescription = stringResource(R.string.profile_open_settings),
      onClick = onOpenSettings,
      // 用**主题色**（2026-09-30 起）：它是首页右上角唯一的动作，用主色标出来最省事，
      // 顺带把"当前选了什么主题色"带到用户面前
      tint = Accent,
    )
  }
}
