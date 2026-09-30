/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

/** 手动录入表单里的一栏，用于把「哪一栏填错了」精确报给用户。 */
enum class FoodField {
  NAME,
  ENERGY,
  PROTEIN,
  FAT,
  CARBOHYDRATE,
}

/**
 * 手动录入表单的**原始文本**。整份都是字符串 —— 用户在输入过程中随时可能停在
 * `-`、`1.`、空串这类中间状态，过早转成数字会边打字边报错。
 * 校验与转换集中在 [toFood]，那是个纯函数，可单测、与 Compose 无关。
 */
data class FoodDraft(
  val name: String = "",
  val energyKcal: String = "",
  val proteinG: String = "",
  val fatG: String = "",
  val carbohydrateG: String = "",
)

/** [FoodDraft.toFood] 的结果。 */
sealed interface FoodDraftResult {
  data class Valid(val food: Food) : FoodDraftResult

  /** 名称为空（去掉首尾空白后）。名称是唯一必填项。 */
  data object NameMissing : FoodDraftResult

  /** [field] 填的**不是不小于 0 的数字**（非数字，或负数）。留空 = 未填，合法，不算错。 */
  data class InvalidNumber(val field: FoodField) : FoodDraftResult
}

/**
 * 校验并转成可入库的 [Food]。
 *
 * 规则：
 * - `name` 去首尾空白后不得为空；
 * - 营养字段**留空 = 缺测 = `null`**（不是 0）—— 与 [Food] 的口径一致；
 * - 营养字段必须是不小于 0 的数字，否则报 [FoodDraftResult.InvalidNumber]。
 *
 * @param existingId 编辑已有记录时传它的 id；新建传 0，由数据库分配。
 * @param now 录入时间戳，由调用方注入以便测试。
 */
fun FoodDraft.toFood(
  existingId: Long = 0L,
  origin: FoodOrigin = FoodOrigin.MANUAL,
  now: Long = System.currentTimeMillis(),
): FoodDraftResult {
  val trimmedName = name.trim()
  if (trimmedName.isEmpty()) return FoodDraftResult.NameMissing

  val values =
    listOf(
      FoodField.ENERGY to energyKcal,
      FoodField.PROTEIN to proteinG,
      FoodField.FAT to fatG,
      FoodField.CARBOHYDRATE to carbohydrateG,
    )

  val parsed = mutableMapOf<FoodField, Double?>()
  for ((field, raw) in values) {
    val parsedValue =
      when (val result = parseNutrient(raw)) {
        is NutrientParse.Missing -> null
        is NutrientParse.Invalid -> return FoodDraftResult.InvalidNumber(field)
        is NutrientParse.Ok -> result.value
      }
    parsed[field] = parsedValue
  }

  return FoodDraftResult.Valid(
    Food(
      id = existingId,
      name = trimmedName,
      energyKcal = parsed[FoodField.ENERGY],
      proteinG = parsed[FoodField.PROTEIN],
      fatG = parsed[FoodField.FAT],
      carbohydrateG = parsed[FoodField.CARBOHYDRATE],
      origin = origin,
      createdAt = now,
    )
  )
}

/** 把 [Food] 回填成表单初值，供编辑已有食材时使用。 */
fun Food.toDraft(): FoodDraft =
  FoodDraft(
    name = name,
    energyKcal = energyKcal.toNumberField(),
    proteinG = proteinG.toNumberField(),
    fatG = fatG.toNumberField(),
    carbohydrateG = carbohydrateG.toNumberField(),
  )

private sealed interface NutrientParse {
  /** 留空 —— 用户没填，等价于「缺测」。 */
  data object Missing : NutrientParse

  data object Invalid : NutrientParse

  data class Ok(val value: Double) : NutrientParse
}

private fun parseNutrient(raw: String): NutrientParse {
  val text = raw.trim()
  if (text.isEmpty()) return NutrientParse.Missing
  val value = text.toDoubleOrNull() ?: return NutrientParse.Invalid
  // 允许「1.5」也允许「.5」；但不接受 NaN / Infinity / 负数 ——
  // 营养成分没有负值，把 `-5` 当成合法数字存下来只会污染后续的能量计算。
  if (!value.isFinite() || value < 0.0) return NutrientParse.Invalid
  return NutrientParse.Ok(value)
}

/** 编辑态回填：整数不带 `.0`，缺测留空。 */
private fun Double?.toNumberField(): String = if (this == null) "" else formatNutrient(this)
