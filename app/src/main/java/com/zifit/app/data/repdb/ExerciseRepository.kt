/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.repdb

import android.content.Context
import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 动作库数据访问的唯一出口。
 *
 * 数据源是**随包携带**的 assets（`repdb/exercises.json` + 1056 张 WebP），
 * 不走网络 —— 无需 GMS、无请求配额、离线可用。
 *
 * 中文译文是可选的**覆盖层**（`repdb/zh/`）：文件缺失或只译了一部分时，
 * 逐个字段回退到英文原文，而不是整体失败。这样译文可以分批产出、随时补齐。
 */
internal class ExerciseRepository(private val assets: AssetManager) {

  private val json = Json { ignoreUnknownKeys = true }

  /** 601 条动作解析一次约数百毫秒，缓存整表；数据集随包不可变，无需失效逻辑。 */
  private val loadLock = Mutex()

  @Volatile private var cached: List<Exercise>? = null

  /** 读取全部动作。首次调用解析 JSON，之后命中缓存。 */
  suspend fun all(): List<Exercise> {
    cached?.let { return it }
    return loadLock.withLock {
      cached ?: load().also { cached = it }
    }
  }

  suspend fun byId(id: String): Exercise? = all().firstOrNull { it.id == id }

  private suspend fun load(): List<Exercise> = withContext(Dispatchers.IO) {
    val raw = assets.open(ASSET_DATASET).bufferedReader().use { it.readText() }
    val dataset = json.decodeFromString<RepDbDatasetDto>(raw)

    val names = readJsonMap<String>(ASSET_ZH_NAMES)
    val details = readJsonMap<ZhDetailsDto>(ASSET_ZH_DETAILS)

    dataset.exercises.map { dto ->
      dto.toExercise(nameZh = names?.get(dto.id), detailsZh = details?.get(dto.id))
    }
  }

  /** 覆盖层文件可能不存在（译文未产出），一律降级为 null 而不是抛错。 */
  private inline fun <reified T> readJsonMap(path: String): Map<String, T>? = runCatching {
      assets.open(path).bufferedReader().use { json.decodeFromString<Map<String, T>>(it.readText()) }
    }
    .getOrNull()

  private fun RepDbExerciseDto.toExercise(nameZh: String?, detailsZh: ZhDetailsDto?): Exercise {
    val instructionsZh = detailsZh?.instructions?.takeIf { it.isNotEmpty() }
    val tipsZh = detailsZh?.tips?.takeIf { it.isNotEmpty() }
    val descriptionZh = detailsZh?.description?.takeIf { it.isNotBlank() }

    return Exercise(
      id = id,
      name = nameZh?.takeIf { it.isNotBlank() } ?: nameEn,
      nameEn = nameEn,
      description = descriptionZh ?: descriptionEn,
      category = ExerciseTaxonomy.category(category),
      forceType = ExerciseTaxonomy.forceType(forceType),
      mechanic = ExerciseTaxonomy.mechanic(mechanic),
      difficulty = ExerciseTaxonomy.difficulty(difficulty),
      equipment = ExerciseTaxonomy.equipment(equipment),
      bodyPart = ExerciseTaxonomy.bodyPart(bodyPart),
      // 筛选分类在**数据层**算好：判定要读原始枚举 key，而 key 到 UI 层就只剩中文标签了
      categories =
        ExerciseCategory.of(
          bodyPartKey = bodyPart,
          categoryKey = category,
          primaryMuscleKeys = primaryMuscles,
        ),
      primaryMuscles = ExerciseTaxonomy.muscles(primaryMuscles),
      secondaryMuscles = ExerciseTaxonomy.muscles(secondaryMuscles),
      goals = ExerciseTaxonomy.goals(goals),
      tags = ExerciseTaxonomy.tags(tags),
      isBodyweight = isBodyweight,
      isUnilateral = isUnilateral,
      instructions = instructionsZh ?: instructionsEn,
      tips = tipsZh ?: tipsEn,
      met = met,
      imageStart = images.flat["start"],
      imagePeak = images.flat["peak"] ?: images.flat["main"],
      isTranslated = descriptionZh != null || instructionsZh != null || tipsZh != null,
    )
  }

  companion object {
    private const val ASSET_DATASET = "repdb/exercises.json"
    private const val ASSET_ZH_NAMES = "repdb/zh/names.json"
    private const val ASSET_ZH_DETAILS = "repdb/zh/details.json"

    /** 数据集随包携带且不可变，进程内单例即可，无需引入 DI 框架。 */
    @Volatile private var instance: ExerciseRepository? = null

    fun get(context: Context): ExerciseRepository =
      instance
        ?: synchronized(this) {
          instance ?: ExerciseRepository(context.applicationContext.assets).also { instance = it }
        }
  }
}
