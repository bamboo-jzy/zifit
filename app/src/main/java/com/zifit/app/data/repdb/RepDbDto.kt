/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.repdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * RepDB 免费层数据集（`assets/repdb/exercises.json`，schema v3）的传输对象。
 *
 * 只映射本项目实际用到的字段 —— 数据集里另有德/西语全套文本，不解析以减少内存占用。
 * 所有集合类字段都给默认值：数据集版本升级时新增/缺失字段不应让解析整体失败。
 *
 * 数据来源与许可：见 `assets/repdb/LICENSE-DATA.md`（RepDB Free Tier License v1.0）。
 */
@Serializable
internal data class RepDbDatasetDto(
  val count: Int = 0,
  val exercises: List<RepDbExerciseDto> = emptyList(),
)

@Serializable
internal data class RepDbExerciseDto(
  val id: String,
  @SerialName("name_en") val nameEn: String,
  @SerialName("description_en") val descriptionEn: String = "",
  val category: String = "",
  @SerialName("force_type") val forceType: String? = null,
  val mechanic: String? = null,
  val difficulty: String = "",
  /** 徒手动作为 null。 */
  val equipment: String? = null,
  @SerialName("body_part") val bodyPart: String = "",
  @SerialName("primary_muscles") val primaryMuscles: List<String> = emptyList(),
  @SerialName("secondary_muscles") val secondaryMuscles: List<String> = emptyList(),
  val goals: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  @SerialName("is_unilateral") val isUnilateral: Boolean = false,
  @SerialName("is_bodyweight") val isBodyweight: Boolean = false,
  @SerialName("instructions_en") val instructionsEn: List<String> = emptyList(),
  @SerialName("tips_en") val tipsEn: List<String> = emptyList(),
  /** 代谢当量，决定该动作的能量消耗强度；范围约 1.3–11.8。 */
  val met: Double? = null,
  val images: RepDbImagesDto = RepDbImagesDto(),
)

/** `flat` 是图片变体名 → assets 内相对路径，如 `{"start": "images/flat/xxx-start.webp"}`。 */
@Serializable
internal data class RepDbImagesDto(val flat: Map<String, String> = emptyMap())

/**
 * 中文覆盖层（由本项目自行产出的译文，放在 `assets/repdb/zh/`）。
 *
 * 之所以与数据集分离：RepDB 许可 Term 3 禁止把「派生的数据集」再分发，
 * 译文即派生内容 —— 故与原始数据同等对待，不入版本库、随 APK 分发。
 */
@Serializable
internal data class ZhDetailsDto(
  val description: String? = null,
  val instructions: List<String> = emptyList(),
  val tips: List<String> = emptyList(),
)
