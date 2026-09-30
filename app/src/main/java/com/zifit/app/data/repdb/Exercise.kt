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
 * 动作库的领域模型 —— UI 只认这个类型，不接触 RepDB 的原始字段名与英文枚举。
 *
 * [name] 为中文名（有译文时），[nameEn] 始终保留原文：英文名是动作的通用检索键，
 * 很多动作在中文语境下没有稳定译名，双语并列更实用。
 *
 * [isTranslated] 标记该动作的**长文本**（描述/步骤/要点）是否已有中文。
 * 中文覆盖层分批产出，未覆盖的动作在 UI 上明确标注「原文」，不假装已本地化。
 */
data class Exercise(
  val id: String,
  val name: String,
  val nameEn: String,
  val description: String,
  val category: String,
  val forceType: String?,
  val mechanic: String?,
  val difficulty: String,
  val equipment: String?,
  val bodyPart: String,
  /**
   * 所属的**筛选分类**（见 [ExerciseCategory]），一条动作可归多类（如深蹲 = 腿部 + 臀部）。
   *
   * 与 [bodyPart] 的分工：`bodyPart` 是数据集的原样部位，用于列表与详情页展示；
   * `categories` 是面向筛选的、已合并与补全过的词表。两者不是同一个东西，不要互相替换。
   */
  val categories: List<String> = emptyList(),
  val primaryMuscles: List<String>,
  val secondaryMuscles: List<String>,
  val goals: List<String>,
  val tags: List<String>,
  val isBodyweight: Boolean,
  val isUnilateral: Boolean,
  val instructions: List<String>,
  val tips: List<String>,
  val met: Double?,
  /** assets 内相对路径（`images/flat/xxx.webp`），可能为空。 */
  val imageStart: String?,
  val imagePeak: String?,
  val isTranslated: Boolean,
) {
  /** 列表与详情优先展示峰值帧（动作到位瞬间），没有则退回起始帧。 */
  val primaryImage: String?
    get() = imagePeak ?: imageStart

  /** 供搜索匹配的全部文本：中英文名 + 肌群 + 器械 + 部位 + 分类。命中面越宽越好用。 */
  internal val searchHaystack: String =
    listOf(name, nameEn, bodyPart, equipment.orEmpty())
      .plus(categories)
      .plus(primaryMuscles)
      .plus(secondaryMuscles)
      .plus(tags)
      .joinToString(" ")
      .lowercase()
}
