/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 手动录入表单的解析约定。重点在「留空 ≠ 0」与「非法输入要报得出是哪一栏」。 */
class FoodDraftTest {

  private val now = 1_700_000_000_000L

  private fun valid(draft: FoodDraft): Food {
    val result = draft.toFood(now = now)
    assertTrue("期望解析成功，实际 $result", result is FoodDraftResult.Valid)
    return (result as FoodDraftResult.Valid).food
  }

  @Test
  fun blankName_isRejected() {
    assertEquals(FoodDraftResult.NameMissing, FoodDraft().toFood(now = now))
    // 只有空白也算没填
    assertEquals(FoodDraftResult.NameMissing, FoodDraft(name = "   ").toFood(now = now))
  }

  @Test
  fun nameIsTrimmed() {
    assertEquals("小麦粉", valid(FoodDraft(name = "  小麦粉 ")).name)
  }

  /** **本条是本轮最关键的约定**：营养栏留空 = 未测 = null，绝不能悄悄变成 0。 */
  @Test
  fun blankNutrients_becomeNull_notZero() {
    val food = valid(FoodDraft(name = "自来水"))
    assertEquals(null, food.energyKcal)
    assertEquals(null, food.proteinG)
    assertEquals(null, food.fatG)
    assertEquals(null, food.carbohydrateG)
  }

  @Test
  fun parsesRealValues() {
    val food =
      valid(
        FoodDraft(
          name = "小麦",
          energyKcal = "338",
          proteinG = "11.9",
          fatG = "1.3",
          carbohydrateG = "75.2",
        )
      )
    assertEquals(338.0, food.energyKcal!!, 1e-9)
    assertEquals(11.9, food.proteinG!!, 1e-9)
    assertEquals(1.3, food.fatG!!, 1e-9)
    assertEquals(75.2, food.carbohydrateG!!, 1e-9)
    assertEquals(FoodOrigin.MANUAL, food.origin)
    assertEquals(now, food.createdAt)
    assertEquals(0L, food.id)
  }

  @Test
  fun nonNumericValue_reportsTheOffendingField() {
    val result = FoodDraft(name = "米", proteinG = "abc").toFood(now = now)
    assertEquals(FoodDraftResult.InvalidNumber(FoodField.PROTEIN), result)
  }

  /**
   * 负的营养值没有意义 —— 放进去只会污染后续的能量计算，所以一律拒绝。
   * `-` 也走这条：它不是合法输入，不要因为长得像破折号就当成缺测。
   */
  @Test
  fun negativeAndDash_areRejected() {
    assertEquals(
      FoodDraftResult.InvalidNumber(FoodField.CARBOHYDRATE),
      FoodDraft(name = "米", carbohydrateG = "-1").toFood(now = now),
    )
    assertEquals(
      FoodDraftResult.InvalidNumber(FoodField.CARBOHYDRATE),
      FoodDraft(name = "米", carbohydrateG = "—").toFood(now = now),
    )
  }

  /** 小数位数不进模型：`11.90` 与 `11.9` 是同一个数。 */
  @Test
  fun trailingZerosDoNotChangeValue() {
    assertEquals(11.9, valid(FoodDraft(name = "米", proteinG = "11.90")).proteinG!!, 1e-9)
  }

  @Test
  fun editingKeepsIdOriginAndCreatedAt() {
    val original = Food(id = 7L, name = "旧名", energyKcal = 1.0, origin = FoodOrigin.AI, createdAt = 42L)
    val result = original.toDraft().copy(name = "新名", energyKcal = "2").toFood(
      existingId = original.id,
      origin = original.origin,
      now = original.createdAt,
    )
    val edited = (result as FoodDraftResult.Valid).food
    assertEquals(7L, edited.id)
    assertEquals("新名", edited.name)
    assertEquals(2.0, edited.energyKcal!!, 1e-9)
    // 改的是数值，不是这条记录的出身
    assertEquals(FoodOrigin.AI, edited.origin)
    assertEquals(42L, edited.createdAt)
  }

  @Test
  fun draftRoundTripPreservesValues() {
    val original =
      Food(
        id = 3L,
        name = "鸡蛋",
        energyKcal = 144.0,
        proteinG = 13.3,
        fatG = 8.8,
        carbohydrateG = 2.8,
      )
    val roundTripped =
      (original.toDraft().toFood(existingId = 3L, now = now) as FoodDraftResult.Valid).food
    assertEquals(original.copy(createdAt = now), roundTripped)
  }

  /** 0 是合法值（确实不含某成分），与「未测」是两回事，别把它拦成缺测。 */
  @Test
  fun zeroIsAValueNotAMissing() {
    val food = valid(FoodDraft(name = "水", proteinG = "0"))
    assertEquals(0.0, food.proteinG!!, 1e-9)
  }
}
