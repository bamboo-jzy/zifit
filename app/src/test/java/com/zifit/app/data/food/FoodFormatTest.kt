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
import org.junit.Test

/** 营养数值的展示格式。重点是**缺测不能显示成 0**、整数不带小数点。 */
class FoodFormatTest {

  @Test
  fun missingValueShowsDash_notZero() {
    assertEquals(MISSING_VALUE, formatNutrient(null))
    assertEquals("—", formatNutrient(null))
  }

  @Test
  fun wholeNumbersLoseTheDecimalPoint() {
    assertEquals("338", formatNutrient(338.0))
    assertEquals("0", formatNutrient(0.0))
    assertEquals("100", formatNutrient(100.0))
  }

  @Test
  fun decimalsAreKeptToTheRequestedPrecision() {
    assertEquals("11.9", formatNutrient(11.9))
    assertEquals("1.3", formatNutrient(1.3))
    assertEquals("6.9", formatNutrient(6.85))
  }

  @Test
  fun decimalsZeroRoundsToInteger() {
    assertEquals("338", formatNutrient(338.4, decimals = 0))
    assertEquals("339", formatNutrient(338.6, decimals = 0))
  }

  @Test
  fun smallFractionsDoNotBecomeZero() {
    // 0.03 g 的维生素之类：不能因为默认只留一位小数就显示成 0
    assertEquals("0", formatNutrient(0.03))
    assertEquals("0.03", formatNutrient(0.03, decimals = 2))
  }

  @Test
  fun alwaysUsesDotAsDecimalSeparator() {
    // 固定 Locale.ROOT：在德语等区域，默认格式化会把 11.9 写成 "11,9"
    assertEquals("11.9", formatNutrient(11.9))
  }
}
