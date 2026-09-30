/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 缺测值的展示符号。
 *
 * 刻意用破折号而不是数字 0：`0 mg 钠` 和 `未测钠` 是两件事，
 * 把后者显示成前者会让「钠 0」这种结论凭空成立。
 */
const val MISSING_VALUE = "—"

/**
 * 营养数值的人类可读形式。
 *
 * - `null` → [MISSING_VALUE]；
 * - 数值按 [decimals] 位四舍五入，**整数不带小数点**（`338.0` → `338`，不是 `338.0`）；
 * - 固定 `Locale.ROOT`：某些地区的小数点是逗号，会让 `11.9` 变成 `11,9`。
 *
 * ⚠️ 走 `BigDecimal` 而非 `round(x * 10) / 10`：后者在二进制浮点上会错。
 * `6.85` 的实际值是 `6.849999999999999644…`，乘 10 得 `68.4999…`，`round` 后是 `68`
 * —— 用户看到的 `6.85` 被舍成 `6.8`，而手算明明是 `6.9`。
 * [BigDecimal.valueOf] 取的是 `Double.toString` 那个「最短能往返的十进制」，
 * 也就是用户在输入框里敲下的那个 `6.85`，再按 [RoundingMode.HALF_UP] 才是直觉结果。
 */
fun formatNutrient(value: Double?, decimals: Int = 1): String {
  if (value == null) return MISSING_VALUE
  val rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP)
  // 整数不带小数点：338.0 → 338。stripTrailingZeros 后的 scale ≤ 0 就说明没有小数部分。
  return if (rounded.stripTrailingZeros().scale() <= 0) {
    rounded.toBigInteger().toString()
  } else {
    rounded.toPlainString()
  }
}
