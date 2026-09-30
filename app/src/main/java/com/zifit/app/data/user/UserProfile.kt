/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.user

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * 用户本人的资料。**唯一的用户就是开发者自己**，所以这里不需要多用户、不需要 id。
 *
 * 为什么要有这些字段：训练与饮食计划都要按人来定 —— 年龄与目标决定训练量与热量缺口，
 * 名称与头像只是让「我的」页像个自己的 App。出生日期与年龄**都留着**：
 * 生日能算出更准的年龄（还会随年份自己变），但很多人只记得自己「大概多少岁」，
 * 强行要求填生日会让人卡住。
 *
 * ⚠️ 所有字段都可空/可空串：这是**从零开始的**一份资料，首启时什么都不该有。
 * 界面据此显示「未设置」，而不是显示默认的假数据（假名字比空更糟）。
 */
data class UserProfile(
  /** 名称。空 = 没填。 */
  val name: String = "",
  /**
   * 生日，ISO `YYYY-MM-DD`。空 = 没填。
   *
   * 刻意存**字符串**而不是 `LocalDate`：它有「没填」这一态，而 `LocalDate` 没有；
   * 每次读出来再 parse 一遍也不值得。格式合法性由 [toProfile] 把关。
   */
  val birthday: String = "",
  /** 年龄，周岁。null = 没填（且生日也推算不出来）。 */
  val age: Int? = null,
  /** 训练目标。null = 没选。 */
  val goal: Goal? = null,
  /**
   * 头像文件的**绝对路径**，位于应用私有目录。空 = 没设头像。
   *
   * 存路径而不是 `Uri`：用户从相册选的 `content://` URI 读权限是**临时**的
   * （进程重启后可能失效，原图被删更是直接读不到）。选中时就把文件复制进私有目录，
   * 见 `AvatarStore`。
   */
  val avatarPath: String = "",
) {
  /** 一个字段都没填。界面据此决定首页显示什么。 */
  val isEmpty: Boolean
    get() = name.isBlank() && birthday.isBlank() && age == null && goal == null && avatarPath.isBlank()
}

/**
 * 训练目标。写成枚举而不是自由文本：这个值会被喂给 AI 生成训练与饮食计划，
 * 自由文本会让「减脂」「减肥」「刷脂」变成三件事。
 */
enum class Goal {
  /** 增肌：热量盈余 + 抗阻训练。 */
  MUSCLE_GAIN,

  /** 减脂：热量缺口 + 保肌训练。 */
  FAT_LOSS,

  /** 保持：维持现状。 */
  MAINTAIN,

  /** 增力：以力量表现为主，热量大致持平或略盈。 */
  STRENGTH,

  /** 健康：以活动量与体态为主，不做严格的热量控制。 */
  HEALTH,
}

/** 年龄上限。超出这个范围基本可以断定是填错了（多打一位数字）。 */
const val MAX_AGE: Int = 120

/**
 * 解析生日。允许 `YYYY-MM-DD`（ISO 标准写法，也是 `LocalDate.parse` 的默认格式）。
 * 非法或不完整一律返回 null —— 输入途中会停在 `1998-0` 这种中间状态，
 * 那时候不该报错，只有**保存时**才需要报错（见 [toProfile]）。
 */
fun parseBirthday(text: String): LocalDate? {
  val trimmed = text.trim()
  if (trimmed.isEmpty()) return null
  return try {
    LocalDate.parse(trimmed)
  } catch (e: DateTimeParseException) {
    null
  }
}

/**
 * 由生日推算周岁。**纯函数**，`today` 注入 —— 否则单测只能测「今天恰好是对的那条」。
 *
 * 生日不合法、或填了一个还没到的日期（比如手滑打成明年）一律返回 null：算出来的
 * 年龄会是负数或荒谬的大数，不如说「算不出来」。
 */
fun ageFromBirthday(birthday: String, today: LocalDate): Int? {
  val date = parseBirthday(birthday) ?: return null
  if (date.isAfter(today)) return null
  val years = Period.between(date, today).years
  return years.takeIf { it in 0..MAX_AGE }
}

/**
 * `LocalDate` ↔ 日期选择器用的 UTC 毫秒（Material3 `DatePickerState.selectedDateMillis`）。
 *
 * 选择器用「UTC 零点毫秒」表示**某一天**、而不是某个时刻，所以两个方向都必须钉死 UTC。
 * 顺手用 `ZoneId.systemDefault()` 转会在东八区差 8 小时、在西半球差整整一天 ——
 * 选 6 月 1 日存成 5 月 31 日，而这种 bug 只在别的时区才现形，本机测不出来。
 * 放在 data 层就是为了让单测能把它钉住（见 `UserProfileTest`）。
 */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** [toPickerMillis] 的逆运算。两个函数必须成对使用，混用系统时区就会错日期。 */
fun pickerMillisToLocalDate(millis: Long): LocalDate =
  Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * 个人资料表单的**原始文本**。与 `FoodDraft` / `AiConfigDraft` 同构：
 * 用户输入过程中会停在 `19`、`1998-`、空串这类中间状态，过早校验只会边打字边报错。
 */
data class UserProfileDraft(
  val name: String = "",
  val birthday: String = "",
  /** 年龄的原始文本。空串 = 没填。 */
  val age: String = "",
  val goal: Goal? = null,
  val avatarPath: String = "",
)

/** [UserProfileDraft.toProfile] 的结果。 */
sealed interface UserProfileDraftResult {
  data class Valid(val profile: UserProfile) : UserProfileDraftResult

  /** 生日填了，但不是 `YYYY-MM-DD`。 */
  data object BirthdayMalformed : UserProfileDraftResult

  /** 年龄那栏不是整数。 */
  data object AgeNotANumber : UserProfileDraftResult

  /** 年龄是整数但不在 `0..MAX_AGE`。 */
  data class AgeOutOfRange(val value: Int) : UserProfileDraftResult
}

/**
 * 校验并规整成可保存的 [UserProfile]。纯函数，可单测。
 *
 * 规则：
 * - **名称可以留空**（不想填名字也能用），其余同；
 * - 生日要么空、要么是合法的 `YYYY-MM-DD`；
 * - 年龄要么空、要么是 `0..120` 的整数；
 * - **年龄留空但生日合法时，用生日推算出来填上** —— 否则「按生日推算」这件事
 *   只活在界面上，后续生成计划时还得再算一遍，两处逻辑迟早走偏。
 */
fun UserProfileDraft.toProfile(today: LocalDate = LocalDate.now()): UserProfileDraftResult {
  val trimmedName = name.trim()
  val trimmedBirthday = birthday.trim()

  if (trimmedBirthday.isNotEmpty() && parseBirthday(trimmedBirthday) == null) {
    return UserProfileDraftResult.BirthdayMalformed
  }

  val trimmedAge = age.trim()
  val parsedAge =
    if (trimmedAge.isEmpty()) {
      null
    } else {
      val value = trimmedAge.toIntOrNull() ?: return UserProfileDraftResult.AgeNotANumber
      if (value !in 0..MAX_AGE) return UserProfileDraftResult.AgeOutOfRange(value)
      value
    }

  return UserProfileDraftResult.Valid(
    UserProfile(
      name = trimmedName,
      birthday = trimmedBirthday,
      age = parsedAge ?: ageFromBirthday(trimmedBirthday, today),
      goal = goal,
      avatarPath = avatarPath,
    )
  )
}

/** 把已保存的资料回填成表单初值。 */
fun UserProfile.toDraft(): UserProfileDraft =
  UserProfileDraft(
    name = name,
    birthday = birthday,
    age = age?.toString().orEmpty(),
    goal = goal,
    avatarPath = avatarPath,
  )
