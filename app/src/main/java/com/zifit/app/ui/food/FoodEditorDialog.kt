/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zifit.app.R
import com.zifit.app.data.food.Food
import com.zifit.app.data.food.FoodDraft
import com.zifit.app.data.food.FoodDraftResult
import com.zifit.app.data.food.FoodField
import com.zifit.app.data.food.FoodOrigin
import com.zifit.app.data.food.toDraft
import com.zifit.app.data.food.toFood
import com.zifit.app.theme.Accent
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.GlassTextField

/** 编辑弹窗的打开对象：新建，或编辑某条已有记录。 */
internal sealed interface FoodEditorTarget {
  data object New : FoodEditorTarget

  data class Existing(val food: Food) : FoodEditorTarget
}

/**
 * 手动录入 / 编辑食材的弹窗。
 *
 * 用自绘 [Dialog] 而不是 `AlertDialog`：全项目的视觉语言是毛玻璃，
 * Material 的实心弹窗与背景割裂，这里直接复用 [GlassSurface]。
 *
 * 校验走 [FoodDraft.toFood] 这个**纯函数**（在 data 层，可单测），
 * 界面只负责把错误映射成文案。校验刻意**不放在 ViewModel**：
 * 输入框要即时反馈，绕一趟 Flow 只会让报错慢半拍。
 */
@Composable
internal fun FoodEditorDialog(
  target: FoodEditorTarget,
  onDismiss: () -> Unit,
  onSave: (Food) -> Unit,
  onDelete: (Food) -> Unit,
) {
  val existing = (target as? FoodEditorTarget.Existing)?.food
  var draft by remember(target) { mutableStateOf(existing?.toDraft() ?: FoodDraft()) }
  var error by remember(target) { mutableStateOf<FoodDraftResult?>(null) }

  // 弹窗**必须整体限高**，否则栏位在小屏/键盘弹起时会溢出：
  // 真机实测（2688px 高、density 3.3）7 栏时的表现是「最后一栏被压成 21dp 并与底部按钮重叠」。
  // 只给滚动区 `heightIn(max=…)` 是不够的 —— 外层无上界时按钮仍会被顶到滚动区上。
  // 现在只剩 5 栏、常规情况下放得下，但键盘弹起时仍会超，限高不能撤。
  //
  // ⚠️ 尺寸要在 **Dialog 之外**取：`Dialog {}` 内部的 `LocalWindowInfo` / `LocalConfiguration`
  // 描述的是**弹窗自己的窗口**（wrap-content，拿不到真实可用高度）。这里取的是宿主窗口。
  // 用 `WindowInfo.containerSize` 而非 `Configuration.screenHeightDp` —— 后者按 targetSdk 不同
  // 对 insets 的处理不一致且会四舍五入到整 dp，Compose lint 也会报 `ConfigurationScreenWidthHeight`。
  val hostHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
  val maxDialogHeight = hostHeight * 0.88f

  Dialog(onDismissRequest = onDismiss) {
    GlassSurface(
      modifier = Modifier.fillMaxWidth().heightIn(max = maxDialogHeight),
      contentPadding = PaddingValues(18.dp),
    ) {
      Text(
        text =
          stringResource(
            if (existing == null) R.string.food_editor_new else R.string.food_editor_edit
          ),
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = stringResource(R.string.food_editor_scale),
        style = MaterialTheme.typography.bodySmall,
        color = InkFaint,
        modifier = Modifier.padding(top = 4.dp),
      )
      if (existing != null) {
        Text(
          text = stringResource(R.string.food_origin_label, originLabel(existing.origin)),
          style = MaterialTheme.typography.bodySmall,
          color = InkFaint,
          modifier = Modifier.padding(top = 2.dp),
        )
      }

      Column(
        // 栏位 + 键盘弹起时仍可能超出，故本区可滚。
        // `weight(1f, fill = false)`：**吃剩余空间但不多占** —— 空间够时按内容高度显示（不撑开留白），
        // 不够时收缩并内部滚动，底部按钮因此永远不会被顶出或与输入框重叠。
        // fill = true 会在空间充足时把弹窗硬撑到限高，编辑态（多一行来源文案）看着发虚。
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassTextField(
          label = stringResource(R.string.food_editor_name),
          value = draft.name,
          onValueChange = { draft = draft.copy(name = it); error = null },
          keyboardType = KeyboardType.Text,
        )
        GlassTextField(
          label = stringResource(R.string.food_editor_energy),
          value = draft.energyKcal,
          onValueChange = { draft = draft.copy(energyKcal = it); error = null },
        )
        GlassTextField(
          label = stringResource(R.string.food_editor_protein),
          value = draft.proteinG,
          onValueChange = { draft = draft.copy(proteinG = it); error = null },
        )
        GlassTextField(
          label = stringResource(R.string.food_editor_fat),
          value = draft.fatG,
          onValueChange = { draft = draft.copy(fatG = it); error = null },
        )
        GlassTextField(
          label = stringResource(R.string.food_editor_carbohydrate),
          value = draft.carbohydrateG,
          onValueChange = { draft = draft.copy(carbohydrateG = it); error = null },
        )
      }

      error?.let { result ->
        Text(
          text = errorText(result, draft),
          style = MaterialTheme.typography.bodySmall,
          color = Accent,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (existing != null) {
          ActionButton(
            text = stringResource(R.string.food_editor_delete),
            onClick = { onDelete(existing) },
          )
        }
        Spacer(Modifier.weight(1f))
        ActionButton(text = stringResource(R.string.food_editor_cancel), onClick = onDismiss)
        ActionButton(
          text = stringResource(R.string.food_editor_save),
          primary = true,
          onClick = {
            // 编辑时保留原 id / 来源 / 录入时间：改的是数值，不是这条记录的出身。
            val result =
              draft.toFood(
                existingId = existing?.id ?: 0L,
                origin = existing?.origin ?: FoodOrigin.MANUAL,
                now = existing?.createdAt ?: System.currentTimeMillis(),
              )
            when (result) {
              is FoodDraftResult.Valid -> onSave(result.food)
              else -> error = result
            }
          },
        )
      }
    }
  }
}

@Composable
private fun errorText(result: FoodDraftResult, draft: FoodDraft): String =
  when (result) {
    is FoodDraftResult.Valid -> ""
    FoodDraftResult.NameMissing -> stringResource(R.string.food_error_name_missing)
    is FoodDraftResult.InvalidNumber ->
      stringResource(R.string.food_error_invalid_number, fieldValue(result.field, draft))
  }

/** 报错里回显用户填的原值，比只报栏位名更容易定位是哪一栏。 */
private fun fieldValue(field: FoodField, draft: FoodDraft): String =
  when (field) {
    FoodField.NAME -> draft.name
    FoodField.ENERGY -> draft.energyKcal
    FoodField.PROTEIN -> draft.proteinG
    FoodField.FAT -> draft.fatG
    FoodField.CARBOHYDRATE -> draft.carbohydrateG
  }

@Composable
private fun originLabel(origin: FoodOrigin): String =
  stringResource(
    when (origin) {
      FoodOrigin.MANUAL -> R.string.food_origin_manual
      FoodOrigin.AI -> R.string.food_origin_ai
      FoodOrigin.NUTRIDATA -> R.string.food_origin_nutridata
    }
  )

/** 弹窗底部的玻璃胶囊按钮。[primary] 时用主色实底。 */
@Composable
private fun ActionButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
  val shape = RoundedCornerShape(percent = 50)
  Box(
    modifier =
      Modifier.clip(shape)
        .background(if (primary) Accent else GlassRowFill)
        .clickable(interactionSource = null, indication = null, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      color = if (primary) Color.White else InkMuted,
    )
  }
}
