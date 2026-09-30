/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zifit.app.R
import com.zifit.app.theme.GlassRowBorder
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkFaint
import java.io.File

/**
 * 圆形头像。没有头像时退回到玻璃底 + 人影图标，**不用姓名首字**：
 * 名称可能没填，一个孤零零的「？」比一个人影剪影更让人摸不着头脑。
 *
 * [onClick] 为 null 时不可点（首页那个头像就只是展示，换头像的入口在个人资料页，
 * 免得在首页误触就弹出图库）。
 */
@Composable
internal fun Avatar(
  path: String,
  size: Dp,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val shape = CircleShape
  // 按路径 remember：换头像后文件名会变，模型随之重建，Coil 不会拿旧的内存缓存
  val model = remember(path) { path.takeIf { it.isNotBlank() }?.let(::File) }

  Box(
    modifier =
      modifier
        .size(size)
        .clip(shape)
        .background(GlassRowFill)
        .border(1.dp, GlassRowBorder, shape)
        .then(
          if (onClick != null) {
            Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
          } else {
            Modifier
          }
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (model == null) {
      Icon(
        painter = painterResource(R.drawable.ic_person),
        contentDescription = null,
        tint = InkFaint,
        modifier = Modifier.size(size * 0.55f),
      )
    } else {
      AsyncImage(
        model = model,
        contentDescription = null,
        // 用户选的图长宽比不定，头像一律裁成正方形填满圆
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().clip(shape),
      )
    }
  }
}

/** 没设头像时的提示文案，压在头像下方。 */
@Composable
internal fun AvatarHint() {
  Text(
    text = stringResource(R.string.profile_avatar_hint),
    style = MaterialTheme.typography.labelSmall,
    color = InkFaint,
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
  )
}
