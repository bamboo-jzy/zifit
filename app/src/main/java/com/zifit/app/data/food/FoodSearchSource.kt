/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

/**
 * 一种「查到的、但还没入库」的候选食材。
 *
 * 与 [Food] 的区别只有两点：**没有 `id`**（数据库还没分配），
 * 以及来源是外部数据源而非用户手输。字段含义与单位完全同 [Food]（每 100g 可食部）。
 */
data class FoodSuggestion(
  val name: String,
  val energyKcal: Double? = null,
  val proteinG: Double? = null,
  val fatG: Double? = null,
  val carbohydrateG: Double? = null,
  val origin: FoodOrigin = FoodOrigin.AI,
) {
  /** 「一键加入食材库」：补上主键与时间戳，产出可直接 `insert` 的 [Food]。 */
  fun toFood(now: Long): Food =
    Food(
      id = 0L,
      name = name,
      energyKcal = energyKcal,
      proteinG = proteinG,
      fatG = fatG,
      carbohydrateG = carbohydrateG,
      origin = origin,
      createdAt = now,
    )
}

/**
 * 一次外部查询的结果。
 *
 * ⚠️ [Unavailable] 必须与「查到 0 条」分开：[Hit] 的空列表表示**确实没这个食材**，
 * 而 [Unavailable] 表示**压根没查成**。二者在界面上是完全不同的话术
 * （「没有匹配的食材」vs「营养库查询尚未接入」），混成一个空列表就会对用户说谎。
 */
sealed interface FoodSearchOutcome {
  /** 查到候选。`suggestions` 可能为空 —— 那是「搜到了，但这个库确实没有」。 */
  data class Hit(val suggestions: List<FoodSuggestion>) : FoodSearchOutcome

  /** 查询通道尚未接上（本轮就是这种状态）。 */
  data object Unavailable : FoodSearchOutcome

  /** 查了但失败（网络、解析、配额……）。`reason` 直接面向用户。 */
  data class Failed(val reason: String) : FoodSearchOutcome
}

/**
 * 食材查询来源。**这是给下一轮 AI 接入预留的唯一接缝** ——
 * 换实现即可，UI 与 ViewModel 都不用动。
 *
 * 之所以做成 `suspend` + 返回 [FoodSearchOutcome] 而不是抛异常：
 * 「没接入」「没搜到」「查失败」三种情形都要如实区分并展示，异常表达不了前两种。
 */
fun interface FoodSearchSource {
  suspend fun search(keyword: String): FoodSearchOutcome
}

/**
 * 本轮使用的占位实现：什么都不查，直接如实回答「尚未接入」。
 *
 * 刻意**不返回空列表**、也不编造假数据 —— 界面会因此显示「营养库查询尚未接入」，
 * 用户看到的是实情，而不是一个假装在工作的空搜索。
 */
object UnavailableFoodSearchSource : FoodSearchSource {
  override suspend fun search(keyword: String): FoodSearchOutcome = FoodSearchOutcome.Unavailable
}
