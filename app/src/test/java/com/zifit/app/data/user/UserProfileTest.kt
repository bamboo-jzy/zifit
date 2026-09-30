/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.user

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 个人资料草稿的校验与规整。**纯函数**，不碰 Android 框架。
 *
 * `today` 一律显式注入：写死"今天"的断言会随着时间推移自己翻脸。
 */
class UserProfileTest {

  private val today = LocalDate.of(2026, 9, 30)

  private fun profile(draft: UserProfileDraft): UserProfile =
    (draft.toProfile(today) as UserProfileDraftResult.Valid).profile

  /** 一份从零开始的资料：**什么都不填也能保存**，所有字段都该是空的。 */
  @Test
  fun blankDraft_isValid_andEverythingStaysEmpty() {
    val result = profile(UserProfileDraft())
    assertEquals("", result.name)
    assertEquals("", result.birthday)
    assertNull(result.age)
    assertNull(result.goal)
    assertEquals("", result.avatarPath)
  }

  @Test
  fun nameIsTrimmed() {
    assertEquals("JZY", profile(UserProfileDraft(name = "  JZY  ")).name)
  }

  @Test
  fun birthdayValid_ageIsDerived() {
    val result = profile(UserProfileDraft(birthday = "1998-06-01"))
    assertEquals("1998-06-01", result.birthday)
    // 2026-09-30 时 1998-06-01 生的人已满 28 周岁
    assertEquals(28, result.age)
  }

  /** 手填的年龄优先：生日只是用来推算，不该反过来覆盖用户明确写下的值。 */
  @Test
  fun explicitAge_winsOverBirthday() {
    val result = profile(UserProfileDraft(birthday = "1998-06-01", age = "30"))
    assertEquals(30, result.age)
  }

  @Test
  fun birthayMalformed_isRejected() {
    assertEquals(
      UserProfileDraftResult.BirthdayMalformed,
      UserProfileDraft(name = "米", birthday = "1998/06/01").toProfile(today),
    )
    assertEquals(
      UserProfileDraftResult.BirthdayMalformed,
      UserProfileDraft(name = "米", birthday = "1998-13-40").toProfile(today),
    )
  }

  /** 生日留空是**允许**的 —— 很多人只记得自己多大，不想被生日卡住。 */
  @Test
  fun blankBirthday_isAllowed() {
    val result = profile(UserProfileDraft(name = "米", birthday = "   "))
    assertEquals("", result.birthday)
    assertNull(result.age)
  }

  @Test
  fun ageNotANumber_isRejected() {
    assertEquals(
      UserProfileDraftResult.AgeNotANumber,
      UserProfileDraft(age = "二十八").toProfile(today),
    )
  }

  @Test
  fun ageOutOfRange_isRejected() {
    assertEquals(
      UserProfileDraftResult.AgeOutOfRange(121),
      UserProfileDraft(age = "121").toProfile(today),
    )
    assertEquals(
      UserProfileDraftResult.AgeOutOfRange(-1),
      UserProfileDraft(age = "-1").toProfile(today),
    )
  }

  @Test
  fun ageZero_isAllowed() {
    // 0 岁是个合法值（婴儿），别把它当成"没填"
    assertEquals(0, profile(UserProfileDraft(age = "0")).age)
  }

  @Test
  fun goalAndAvatarPassThrough() {
    val result =
      profile(UserProfileDraft(goal = Goal.FAT_LOSS, avatarPath = "/data/avatar/a.jpg"))
    assertEquals(Goal.FAT_LOSS, result.goal)
    assertEquals("/data/avatar/a.jpg", result.avatarPath)
  }

  @Test
  fun draftRoundTripPreservesValues() {
    val original =
      UserProfile(name = "JZY", birthday = "1998-06-01", age = 28, goal = Goal.STRENGTH)
    val roundTripped = original.toDraft().toProfile(today)
    assertEquals(original, (roundTripped as UserProfileDraftResult.Valid).profile)
  }

  @Test
  fun ageFromBirthday_handlesBoundaries() {
    assertEquals(0, ageFromBirthday("2026-09-30", today))
    assertEquals(1, ageFromBirthday("2025-09-30", today))
    // 明天的生日还"没到" → 算不出来，不许返回负数或 0 岁
    assertNull(ageFromBirthday("2030-01-01", today))
    assertNull(ageFromBirthday("不是日期", today))
    assertNull(ageFromBirthday("", today))
  }

  /**
   * 选择器毫秒 ↔ `LocalDate` 必须**钉死在 UTC**。
   *
   * 这是本轮唯一一处「本机自己抓不出来」的坑：若把实现改成
   * `atStartOfDay(ZoneId.systemDefault())`，在 UTC+8 的机器上做往返测试
   * **照样全绿** —— 两个方向都偏 8 小时，恰好还是同一天；
   * 真正现形的是 UTC-5 那侧的机器，选 6 月 1 日会存成 5 月 31 日。
   * 所以下面三个断言各有分工：
   * - **固定毫秒常量**：唯一能在东八区抓住「偷用系统时区」的断言，
   *   系统时区算出来是 `896630400000`（差 8 小时），而不论机器在哪都该是 `896659200000`；
   * - 往返：只保证两个函数**成对**使用时不丢日期（一边 UTC 一边系统时区才会挂）；
   * - 端到端：选择器 → ISO 字符串 → 读回来，仍是用手点的那一天。
   */
  @Test
  fun pickerMillis_isUtcMidnight_notSystemZoneMidnight() {
    val date = LocalDate.of(1998, 6, 1)
    assertEquals(896659200000L, date.toPickerMillis())
    assertEquals(date, pickerMillisToLocalDate(896659200000L))
  }

  @Test
  fun pickerMillis_roundTripsAcrossLeapDayAndEpoch() {
    // 闰日与 1970 前后各取一个：负毫秒是除法取整最容易写错的地方
    listOf(LocalDate.of(2000, 2, 29), LocalDate.of(1969, 12, 31), LocalDate.of(1998, 6, 1))
      .forEach { date -> assertEquals(date, pickerMillisToLocalDate(date.toPickerMillis())) }
  }

  @Test
  fun birthdaySavedFromPicker_isTheSameDayAsPicked() {
    // 端到端形状：选择器给毫秒 → 存 ISO → 再读回来仍然是用户点的那一天
    val picked = LocalDate.of(1998, 6, 1)
    val saved = profile(UserProfileDraft(birthday = pickerMillisToLocalDate(picked.toPickerMillis()).toString()))
    assertEquals("1998-06-01", saved.birthday)
    assertEquals(picked, parseBirthday(saved.birthday))
  }
}
