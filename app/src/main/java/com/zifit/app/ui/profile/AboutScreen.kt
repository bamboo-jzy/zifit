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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zifit.app.BuildConfig
import com.zifit.app.R
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.PageHeader
import com.zifit.app.ui.common.dockContentInset

/**
 * 关于页。从「我的」首页的那张卡片独立成页（2026-09-30）。
 *
 * 独立成页**不是为了好看**，而是这几段话有外部约束，必须一直可见、可读：
 * 1. GPL-3.0 §5(d) 要求交互式程序展示法律声明（版权行 + 无担保 + 许可获取方式）；
 * 2. RepDB 免费层许可 Term 2 要求在应用内提供**可见**署名
 *    （原文：Exercise data by RepDB (repdb.co)）；
 * 3. 健康免责对应 RepDB 许可 Term 7（非医疗建议），对健身类程序同样必要。
 *
 * ⚠️ 删减任何一段之前，先回去看 AGENTS.md 的许可章节 —— 这不是文案自由度的问题。
 */
@Composable
internal fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize()) {
    PageHeader(title = stringResource(R.string.profile_about), onBack = onBack)
    Spacer(Modifier.height(16.dp))

    Column(
      modifier =
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(bottom = dockContentInset()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.about_tagline),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp),
        )
        // 版本号取 BuildConfig，别无第二处硬编码 —— 改 versionName 时不会漏
        Text(
          text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
          style = MaterialTheme.typography.labelMedium,
          color = InkFaint,
          modifier = Modifier.padding(top = 8.dp),
        )
      }

      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        AboutLine(R.string.profile_about_copyright)
        AboutLine(R.string.profile_about_license)
        AboutLine(R.string.profile_about_source)

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = GlassRowFill)
        Spacer(Modifier.height(10.dp))

        AboutLine(R.string.profile_about_exercises)
        AboutLine(R.string.profile_about_food)
        AboutLine(R.string.profile_about_health)
      }
    }
  }
}

@Composable
private fun AboutLine(textRes: Int) {
  Text(
    text = stringResource(textRes),
    style = MaterialTheme.typography.bodySmall,
    color = InkMuted,
    modifier = Modifier.padding(top = 8.dp),
  )
}
