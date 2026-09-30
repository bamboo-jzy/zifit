/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.repdb

/**
 * 动作库的**筛选分类**。
 *
 * 词表刻意不直接沿用 RepDB 的 `body_part` —— 它把腿拆成 `upper_legs` / `lower_legs`、
 * 把臂拆成 `upper_arms` / `lower_arms`，共 9 类，颗粒度与使用直觉不符：
 * 用户想找的是「练腿的」，不是「大腿还是小腿」。这里合并成 [LEGS] / [ARMS]，
 * 并按需要补上 [GLUTES]、[CARDIO] 等 RepDB 没有单列、但训练语境里独立成类的维度。
 *
 * 顺序 = 声明顺序 = 筛选栏展示顺序。[sort] 依赖这一点，不要按字母或码位重排。
 *
 * **刻意不设的三类**（参考词表里有、RepDB 免费层却无数据支撑）：
 * - `颈部`：601 条里没有任何以颈肌为主的动作；
 * - `拳击/格斗`：既无对应分类，也无对应标签；
 * - `腹部`：以腹直肌/腹斜肌为主的 78 条与 [CORE] 的 70 条重合 **92%**，
 *   单列只会得到两个几乎一样的筛选结果，故并入 [CORE]。
 *
 * 若日后数据集升级带上这些维度，补一个枚举值 + 一条 `when` 分支即可，
 * UI 与仓库层无需改动。
 */
internal enum class ExerciseCategory(val label: String) {
  CHEST("胸部"),
  BACK("背部"),
  SHOULDERS("肩部"),
  /** `upper_legs` + `lower_legs` 合并 —— 用户按「腿」找，不按大腿小腿找。 */
  LEGS("腿部"),
  /** RepDB 无 glutes 部位，用**主要发力肌**含臀大肌/臀中肌判定（159 条）。 */
  GLUTES("臀部"),
  /** `upper_arms` + `lower_arms` 合并。 */
  ARMS("手臂"),
  CORE("核心"),
  /** RepDB 无「有氧」部位，用 `category = cardio` 判定（16 条）。 */
  CARDIO("有氧"),
  FULL_BODY("全身"),
  ;

  /**
   * 一条动作是否属于本分类。
   *
   * 传入的是**原始枚举 key**（不是中文标签）—— 判定必须建立在数据集的事实上，
   * 而不是建立在译名上，否则改一个中文译名就会静默改变筛选结果。
   */
  fun matches(
    bodyPartKey: String?,
    categoryKey: String?,
    primaryMuscleKeys: Collection<String>,
  ): Boolean =
    when (this) {
      CHEST -> bodyPartKey == "chest"
      BACK -> bodyPartKey == "back"
      SHOULDERS -> bodyPartKey == "shoulders"
      LEGS -> bodyPartKey == "upper_legs" || bodyPartKey == "lower_legs"
      GLUTES -> primaryMuscleKeys.any { it in GLUTE_MUSCLES }
      ARMS -> bodyPartKey == "upper_arms" || bodyPartKey == "lower_arms"
      CORE -> bodyPartKey == "core"
      CARDIO -> categoryKey == "cardio"
      FULL_BODY -> bodyPartKey == "full_body"
    }

  companion object {
    private val GLUTE_MUSCLES = setOf("gluteus_maximus", "gluteus_medius")

    /** 全部分类，按展示顺序。 */
    val palette: List<String> = entries.map { it.label }

    /** 一条动作所属的全部分类（中文标签，已按展示顺序排好）。 */
    fun of(
      bodyPartKey: String?,
      categoryKey: String?,
      primaryMuscleKeys: Collection<String>,
    ): List<String> =
      entries
        .filter { it.matches(bodyPartKey, categoryKey, primaryMuscleKeys) }
        .map { it.label }

    /**
     * 按 [palette] 顺序排列给定标签。
     *
     * 不能直接对中文 `sorted()` —— 那会排成按 Unicode 码位的乱序
     * （「全身、有氧、胸部」）。
     */
    fun sort(labels: Collection<String>): List<String> =
      labels.sortedBy { label ->
        palette.indexOf(label).takeIf { it >= 0 } ?: Int.MAX_VALUE
      }
  }
}
