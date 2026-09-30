/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * 食材库数据访问的**唯一出口**（AGENTS.md §4 分层约束）。
 *
 * 与 `ExerciseRepository` 不同，这里的数据是**用户产生、可增删改**的，因此不再缓存整表，
 * 直接透传 Room 的 `Flow` —— 数据库一有写入，界面自动收到新列表，不需要手动刷新。
 */
class FoodRepository(private val dao: FoodDao) {

  fun observeAll(): Flow<List<Food>> = dao.observeAll()

  suspend fun byId(id: Long): Food? = dao.byId(id)

  /** 新增。id 传 0，返回数据库分配的行号。 */
  suspend fun add(food: Food): Long = dao.insert(food)

  /** 编辑已有记录（必须带正确 id）。 */
  suspend fun update(food: Food) = dao.update(food)

  suspend fun delete(food: Food) = dao.delete(food)

  companion object {
    fun get(context: Context): FoodRepository = FoodRepository(FoodDatabase.get(context).foodDao())
  }
}
