/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一种食材的营养成分。**所有营养值一律为「每 100g 可食部」的含量** ——
 * 这与《中国食物成分表》的表头口径一致（表头下方那句「以 100g 可食部计」），
 * 也是后续做饮食计划时唯一需要知道的换算前提。
 *
 * 刻意只留「配餐真正要用到」的四项：能量 / 蛋白质 / 脂肪 / 碳水。
 * 已移除的两项 —— 食部（%）与钠 —— 只用于把「市售重量」折算成可食部、或做钠摄入统计，
 * 对当前的配餐计算没有用；留着等于要求每条数据多录两栏，收益却是零。
 *
 * ⚠️ **缺测值必须是 `null`，不是 `0`**。原始数据里未知值写作 `—` / `…` / `-`，
 * 若一律降成 0，「脂肪 0 g」会被读成「确实不含脂肪」，实际上只是没测。
 * 因此这里全部可空，UI 遇到 null 显示占位符而不是 0。
 */
@Entity(tableName = "food")
data class Food(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  /** 食材名，如「小麦粉（特制）」。唯一必填项。 */
  val name: String,
  /** 能量，kcal。 */
  val energyKcal: Double? = null,
  /** 蛋白质，g。 */
  val proteinG: Double? = null,
  /** 脂肪，g。 */
  val fatG: Double? = null,
  /** 碳水化合物，g。 */
  val carbohydrateG: Double? = null,
  /** 这条记录从哪来，见 [FoodOrigin]。 */
  val origin: FoodOrigin = FoodOrigin.MANUAL,
  /** 录入时间戳（毫秒）。列表按它倒序，「刚加进来的」排最前。 */
  val createdAt: Long = 0L,
)

/** 食材记录的来源，用于在界面上如实标注数据出处。 */
enum class FoodOrigin {
  /** 手动录入。 */
  MANUAL,

  /** 下一轮接入的 AI 查询。 */
  AI,

  /**
   * NutriData（nutridata.cn）。
   *
   * 预留给将来使用 —— 当前**没有任何代码路径**会产出它：nutridata 无免费公开接口，
   * 直连需复刻其请求加密（见 `tasks/todo.md` 第十三轮）。保留该值是为了让来源字段
   * 在数据模型上就区分开，将来接入时不必改表结构。
   */
  NUTRIDATA,
}
