/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.data.repdb.Exercise
import com.zifit.app.theme.GlassRowFill
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted
import com.zifit.app.theme.Brand
import com.zifit.app.ui.common.FilterPill
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.PageHeader
import com.zifit.app.ui.common.SectionTitle
import com.zifit.app.ui.common.dockContentInset

/**
 * 动作详情：插画（起始/到位两帧）+ 结构化要领。
 *
 * [viewModel] 用 `key = exerciseId` 隔离：Navigation 3 的默认 ViewModel 宿主是 Activity，
 * 不指定 key 的话不同动作会共用同一个实例、串数据。
 */
@Composable
fun ExerciseDetailScreen(exerciseId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: ExerciseDetailViewModel =
    viewModel(key = exerciseId, factory = ExerciseDetailViewModel.factory(context, exerciseId))
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  val exercise = (state as? ExerciseDetailUiState.Ready)?.exercise

  Column(modifier = modifier.fillMaxSize()) {
    /*
     * 页头用共用的 PageHeader（不要再手写一遍返回按钮 + 标题）：
     * 2026-09-30 用户要求全站页头统一 —— 返回键无底、标题同一字号，
     * 手写的那版会立刻与其它四个二级页分叉（原先它是 40dp 白圆底 + 24sp 标题）。
     * 标题允许 2 行：动作名本身就是标题，英文名（Barbell Bench Press - Medium Grip）一行放不下。
     */
    PageHeader(
      title = exercise?.name ?: stringResource(R.string.exercise_title),
      onBack = onBack,
      titleMaxLines = 2,
    )

    Spacer(Modifier.height(12.dp))

    when (state) {
      ExerciseDetailUiState.Loading ->
        Hint(stringResource(R.string.exercise_loading))
      ExerciseDetailUiState.NotFound ->
        Hint(stringResource(R.string.exercise_empty))
      is ExerciseDetailUiState.Ready -> DetailContent(exercise = exercise ?: return)
    }
  }
}

@Composable
private fun DetailContent(exercise: Exercise) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        // 让开悬浮 dock。加在 verticalScroll **之后**，这份 padding 才属于滚动内容，
        // 最后一行才能被滚出来看清；加在前面则等于把可滚动区截短。
        .padding(bottom = dockContentInset()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PoseCard(exercise)
    SummaryCard(exercise)
    if (exercise.instructions.isNotEmpty()) {
      StepsCard(
        title = stringResource(R.string.exercise_section_instructions),
        lines = exercise.instructions,
        numbered = true,
      )
    }
    if (exercise.tips.isNotEmpty()) {
      StepsCard(
        title = stringResource(R.string.exercise_section_tips),
        lines = exercise.tips,
        numbered = false,
      )
    }
    CreditNote()
    Spacer(Modifier.height(4.dp))
  }
}

@Composable
private fun PoseCard(exercise: Exercise) {
  val hasBothPoses = exercise.imageStart != null && exercise.imagePeak != null
  // 默认展示到位帧：动作幅度的重点在峰值姿态
  var showPeak by rememberSaveable(exercise.id) { mutableStateOf(true) }
  val path =
    if (hasBothPoses) {
      if (showPeak) exercise.imagePeak else exercise.imageStart
    } else {
      exercise.primaryImage
    }

  GlassSurface(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
    ExerciseImage(
      relativePath = path,
      contentDescription = exercise.name,
      modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
    )
    if (hasBothPoses) {
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill(
          label = stringResource(R.string.exercise_pose_start),
          selected = !showPeak,
          onClick = { showPeak = false },
        )
        FilterPill(
          label = stringResource(R.string.exercise_pose_peak),
          selected = showPeak,
          onClick = { showPeak = true },
        )
      }
    }
  }
}

@Composable
private fun SummaryCard(exercise: Exercise) {
  GlassSurface(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = exercise.name,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    if (exercise.name != exercise.nameEn) {
      Text(
        text = exercise.nameEn,
        style = MaterialTheme.typography.bodyMedium,
        color = InkFaint,
      )
    }
    if (exercise.description.isNotBlank()) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = exercise.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = GlassRowFill)
    Spacer(Modifier.height(12.dp))

    // 去重：category=strength 与 goals 里的 strength 都会译成「力量」，不去重会同屏出现两遍
    val metLabel =
      exercise.met?.let { stringResource(R.string.exercise_met, formatMet(it)) }
        ?: stringResource(R.string.exercise_met_unknown)
    val unilateralLabel = stringResource(R.string.exercise_unilateral)
    val chips =
      listOfNotNull(
          metLabel,
          exercise.bodyPart.takeIf { it.isNotBlank() },
          exercise.equipment,
          exercise.difficulty.takeIf { it.isNotBlank() },
          exercise.category.takeIf { it.isNotBlank() },
          exercise.mechanic,
          exercise.forceType,
          unilateralLabel.takeIf { exercise.isUnilateral },
        )
        .plus(exercise.goals)
        .distinct()

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      chips.forEachIndexed { index, text ->
        // 第一枚是 MET，唯一带强调色
        TagChip(text = text, emphasized = index == 0)
      }
    }

    if (exercise.primaryMuscles.isNotEmpty()) {
      MuscleSection(stringResource(R.string.exercise_section_muscles), exercise.primaryMuscles)
    }
    if (exercise.secondaryMuscles.isNotEmpty()) {
      MuscleSection(stringResource(R.string.exercise_section_synergists), exercise.secondaryMuscles)
    }
    if (exercise.tags.isNotEmpty()) {
      MuscleSection(stringResource(R.string.exercise_section_tags), exercise.tags)
    }
  }
}

@Composable
private fun MuscleSection(title: String, values: List<String>) {
  Spacer(Modifier.height(12.dp))
  SectionTitle(title)
  Spacer(Modifier.height(6.dp))
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    values.forEach { TagChip(it) }
  }
}

/**
 * 只读标签。用最轻的表现形式（淡底 + 小字），避免与可点击的 [FilterPill] 混淆 ——
 * 这里的标签点不动，长得像按钮会误导。
 */
@Composable
private fun TagChip(text: String, emphasized: Boolean = false) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(percent = 50))
        .background(GlassRowFill)
        .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = if (emphasized) Brand else InkMuted,
      fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
    )
  }
}

@Composable
private fun StepsCard(title: String, lines: List<String>, numbered: Boolean) {
  GlassSurface(modifier = Modifier.fillMaxWidth()) {
    SectionTitle(title)
    Spacer(Modifier.height(8.dp))
    lines.forEachIndexed { index, line ->
      Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
          text = if (numbered) "${index + 1}." else "·",
          style = MaterialTheme.typography.bodyMedium,
          color = Brand,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.width(22.dp),
        )
        Text(
          text = line,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

/** RepDB 许可 Term 2 要求的署名 —— 凡是能看到动作数据的地方都要有出处。 */
@Composable
private fun CreditNote() {
  Text(
    text = stringResource(R.string.exercise_credit_short),
    style = MaterialTheme.typography.labelSmall,
    color = InkFaint,
  )
}

@Composable
private fun Hint(text: String) {
  GlassSurface(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** MET 保留一位小数即可，整数时去掉尾随 .0 更清爽。 */
private fun formatMet(value: Double): String =
  if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
