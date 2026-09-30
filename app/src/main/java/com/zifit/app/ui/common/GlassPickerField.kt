/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zifit.app.R
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted

/**
 * 「只能从选择器里取值」的字段，与 [GlassTextField] 同一套框型（标签在上、值在下）。
 *
 * 为什么不让用户打字：生日这种值有**唯一正确写法**（ISO `YYYY-MM-DD`），
 * 让键盘去猜「1998.6.1」「98/6/1」既折磨人也给校验层添了一堆没必要的分支 ——
 * 直接给日历，就没有格式错误这条路径（第十八轮用户明确要求）。
 *
 * 右端固定一个箭头而不是清除叉：值本身要占满剩余宽度，多塞一个叉会让
 * 「点这一行」和「点那个叉」两个热区贴在一起，误触率比省下的那一次点击更高。
 * 清除放在选择器对话框里（见 `ProfileEditScreen.BirthdayPickerDialog`）。
 */
@Composable
fun GlassPickerField(
  label: String,
  value: String,
  /** 值为空时显示的占位文案（如「未设置」）。灰一档，与真值区分开。 */
  placeholder: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(top = 4.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(GlassRowFill)
          .clickable(interactionSource = null, indication = null, onClick = onClick)
          .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = value.ifBlank { placeholder },
        style = MaterialTheme.typography.bodyMedium,
        color = if (value.isBlank()) InkFaint else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = InkFaint,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
