/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.data.user.Goal
import com.zifit.app.data.user.UserProfileDraftResult
import com.zifit.app.data.user.ageFromBirthday
import com.zifit.app.data.user.parseBirthday
import com.zifit.app.data.user.pickerMillisToLocalDate
import com.zifit.app.data.user.toPickerMillis
import com.zifit.app.ui.common.FilterPill
import com.zifit.app.ui.common.GlassPickerField
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.GlassTextField
import com.zifit.app.ui.common.HintText
import com.zifit.app.ui.common.PageHeader
import com.zifit.app.ui.common.SectionTitle
import com.zifit.app.ui.common.dockContentInset
import java.time.LocalDate

/**
 * 个人资料设置页：头像 / 名称 / 生日 / 年龄 / 目标。
 *
 * 校验全部交给纯函数 `UserProfileDraft.toProfile()`（data 层、可单测），
 * 本页只把校验错误翻成人话 —— 与 `FoodEditorDialog`、`AiConfigScreen` 同一套做法。
 *
 * 头像选择用系统相册（`PickVisualMedia`）：**API 33 以下回退到 `ACTION_OPEN_DOCUMENT`，
 * 不需要任何权限、不依赖 GMS**，与 AGENTS.md 的 C2 约束相容。
 * 选中后立刻复制进应用私有目录（[UserProfileViewModel.pickAvatar]），
 * 因为相册给的读权限是临时的。
 *
 * 第十八轮的三处改动都在这个文件里：保存键上移到页头右上、生日改成日历选择器、
 * 头像卡片去掉两个按钮（只留「点头像」这一条路）。
 */
@Composable
internal fun ProfileEditScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: UserProfileViewModel =
    viewModel(factory = UserProfileViewModel.factory(context))
  val draft by viewModel.draft.collectAsStateWithLifecycle()

  var error by remember { mutableStateOf<UserProfileDraftResult?>(null) }
  // 日期选择器是 Dialog（另一个窗口），用一个布尔控制它开不开
  var pickingBirthday by remember { mutableStateOf(false) }

  // 每次进来都用**已保存的**资料重置草稿：上一次没保存就退出的内容不该阴魂不散。
  // ⚠️ 用 `LaunchedEffect(Unit)` 而不是初始化函数 —— 页面每次重新进入都会换一个组合实例。
  LaunchedEffect(Unit) {
    viewModel.beginEdit()
    error = null
  }

  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      // 用户直接返回没选图时 uri 为 null，保持原头像
      if (uri != null) viewModel.pickAvatar(uri)
    }

  // 保存键从页面底部搬到页头右上：底部那颗会被悬浮 dock 压住，
  // 而右上角是这块屏上唯一「怎么滚动都不会跑掉」的位置。
  val save: () -> Unit = {
    val failure = viewModel.save()
    // 保存成功就退回设置页 —— 停在原地看着表单没变化，不如让用户看到结果
    if (failure == null) onBack() else error = failure
  }

  Column(modifier = modifier.fillMaxSize()) {
    PageHeader(
      title = stringResource(R.string.profile_edit_title),
      onBack = onBack,
      trailing = {
        FilterPill(
          label = stringResource(R.string.profile_save),
          selected = true,
          onClick = save,
        )
      },
    )

    // 报错紧贴页头：保存键就在它右边一厘米，报错必须落在同一眼的范围里
    error?.let { failure ->
      Spacer(Modifier.height(10.dp))
      HintText(text = profileErrorText(failure), emphasized = true)
    }
    Spacer(Modifier.height(16.dp))

    val current = draft ?: return@Column

    Column(
      modifier =
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(bottom = dockContentInset()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AvatarCard(
        avatarPath = current.avatarPath,
        onPick = {
          picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
      )

      GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          GlassTextField(
            label = stringResource(R.string.profile_field_name),
            value = current.name,
            onValueChange = {
              viewModel.updateDraft { draft -> draft.copy(name = it) }
              error = null
            },
            keyboardType = KeyboardType.Text,
          )
          // 生日是**选择**而不是输入：ISO 日期只有一种写法，键盘只会带来格式错误
          GlassPickerField(
            label = stringResource(R.string.profile_field_birthday),
            value = current.birthday,
            placeholder = stringResource(R.string.profile_birthday_unset),
            onClick = { pickingBirthday = true },
          )
          GlassTextField(
            label = stringResource(R.string.profile_field_age),
            value = current.age,
            onValueChange = {
              viewModel.updateDraft { draft -> draft.copy(age = it) }
              error = null
            },
            keyboardType = KeyboardType.Number,
          )
          // 生日填了就顺手算一下年龄：既让用户知道填对没有，也解释了保存后年龄从哪来
          DerivedAgeHint(birthday = current.birthday)

          GoalPicker(
            selected = current.goal,
            onSelect = { goal ->
              // 再点一次已选中的那个 = 取消选择。目标本来就允许不填。
              viewModel.updateDraft { draft -> draft.copy(goal = goal.takeIf { it != draft.goal }) }
              error = null
            },
          )
        }
      }
    }

    if (pickingBirthday) {
      BirthdayPickerDialog(
        birthday = current.birthday,
        onDismiss = { pickingBirthday = false },
        onConfirm = { iso ->
          viewModel.updateDraft { draft -> draft.copy(birthday = iso) }
          error = null
          pickingBirthday = false
        },
        onClear = {
          viewModel.updateDraft { draft -> draft.copy(birthday = "") }
          error = null
          pickingBirthday = false
        },
      )
    }
  }
}

/**
 * 头像区：一颗大号头像，**没有按钮**。
 *
 * 曾经挂着「点击头像更换」与「移除头像」两个控件，用户嫌这一块太吵，两个都去掉了
 * （第十八轮）。于是整个圆就是唯一的热区 —— 这也是各家 App 的通行做法，不用教。
 *
 * 代价：头像设了**去不掉**了（原来靠「移除头像」）。这是用户明确的取舍：
 * 想换换一张就行，不提供「回到没头像」这条路径。
 */
@Composable
private fun AvatarCard(avatarPath: String, onPick: () -> Unit) {
  GlassSurface(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Avatar(path = avatarPath, size = 96.dp, onClick = onPick)
      AvatarHint()
    }
  }
}

/**
 * 生日选择器。用 Material3 的 `DatePicker`（Apache-2.0，无 GMS 依赖），
 * 不自造滚轮 —— 三列滚轮要自己处理闰年、大小月与月份天数联动，
 * 纯属把官方已经做对的事再做错一遍。
 *
 * 「清除」只在**已有生日**时才出现：生日本来就允许不填，但给一个空状态下点了
 * 没反应的按钮，不如不给。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPickerDialog(
  birthday: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
  onClear: () -> Unit,
) {
  val initialMillis =
    remember(birthday) {
      // 没填过就从「今天往前 25 年」开始：健身用户基本落在这个区间附近，
      // 而从 1900 年（选择器默认的起点）开始要往上翻几百下。
      (parseBirthday(birthday) ?: LocalDate.now().minusYears(DEFAULT_START_AGE)).toPickerMillis()
    }
  val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          // 理论上不会为 null（上面给了初始值），真为 null 就当用户没改、直接关掉
          state.selectedDateMillis?.let { onConfirm(pickerMillisToLocalDate(it).toString()) }
            ?: onDismiss()
        }
      ) {
        Text(stringResource(R.string.picker_confirm))
      }
    },
    dismissButton = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (birthday.isNotBlank()) {
          TextButton(onClick = onClear) { Text(stringResource(R.string.picker_clear)) }
        }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.picker_cancel)) }
      }
    },
  ) {
    // title 留空：Material 默认的「选择日期」标题是给不认识日历的人看的，
    // 省下的高度让日历本身更好点。headline 保留 —— 它实时显示选中的是哪天。
    DatePicker(state = state, title = null, showModeToggle = false)
  }
}

/** 生日没填时，日历从「今天往前多少年」开始。25 是常识上的中点，往上往下都只差几下。 */
private const val DEFAULT_START_AGE = 25L

/** 生日合法时给一句「按生日推算：N 岁」。生日没填或写错了就什么都不说。 */
@Composable
private fun DerivedAgeHint(birthday: String) {
  // 只依赖 birthday 重算：每敲一个字符算一次日历开销可忽略，但别每次重组都算
  val derived = remember(birthday) { ageFromBirthday(birthday, LocalDate.now()) }
  if (derived != null) {
    HintText(text = stringResource(R.string.profile_age_derived, derived))
  }
}

/** 训练目标：五选一，可取消。 */
@Composable
private fun GoalPicker(selected: Goal?, onSelect: (Goal) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
    SectionTitle(stringResource(R.string.profile_field_goal))
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Goal.entries.forEach { goal ->
        FilterPill(
          label = goalLabel(goal),
          selected = goal == selected,
          onClick = { onSelect(goal) },
        )
      }
    }
    if (selected == null) {
      HintText(text = stringResource(R.string.profile_goal_hint), modifier = Modifier.padding(top = 8.dp))
    }
  }
}

/** 校验错误的人话。与 `AiConfigScreen` 的 `errorText` 同一形态：每一档都必须说清改哪里。 */
@Composable
private fun profileErrorText(result: UserProfileDraftResult): String =
  when (result) {
    is UserProfileDraftResult.Valid -> ""
    UserProfileDraftResult.BirthdayMalformed -> stringResource(R.string.profile_error_birthday)
    UserProfileDraftResult.AgeNotANumber -> stringResource(R.string.profile_error_age_number)
    is UserProfileDraftResult.AgeOutOfRange -> stringResource(R.string.profile_error_age_range)
  }
