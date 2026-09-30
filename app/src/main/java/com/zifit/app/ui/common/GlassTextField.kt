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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zifit.app.theme.Accent
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkMuted

/**
 * 弹窗里的单行输入框：标签在上、输入区在下。
 *
 * 放在 common 而不是某个 feature 包里：食材录入与 AI 配置两处都要用，
 * 业务包之间**不得互相 import**（AGENTS.md §5.3 同一套约定）。
 *
 * 刻意不用 `OutlinedTextField`：整站的视觉语言是手绘毛玻璃，
 * Material 的描边+浮动标签会在这里显得像另一个 App 的控件。
 */
@Composable
fun GlassTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Decimal,
  /** 密钥这类敏感内容传 [VisualTransformation] 的密码变换，别让明文显示在屏幕上。 */
  visualTransformation: VisualTransformation = VisualTransformation.None,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .padding(top = 4.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(GlassRowFill)
          .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        // 数字键盘：营养值都是数字，默认全键盘要切一次。
        // 非法字符不在这里拦 —— 解析层会兜住并给出可读的报错，少一处重复规则。
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
