/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.food

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 食材库的数据库入口。 */
@Dao
interface FoodDao {

  /**
   * 全部食材，**按录入时间倒序**（最近添加的在最前）。
   *
   * ⚠️ 刻意不按 `name` 排序：中文名走 SQLite 的 `ORDER BY` 就是 **UTF-8 码位序**，
   * 「面粉」会排到「米饭」前面这种结果对用户毫无意义（与 `tasks/lessons.md` 第 32 条同因）。
   * 要按名字找，用搜索框；列表本身给「刚加的」。
   */
  @Query("SELECT * FROM food ORDER BY createdAt DESC, id DESC")
  fun observeAll(): Flow<List<Food>>

  @Query("SELECT * FROM food WHERE id = :id")
  suspend fun byId(id: Long): Food?

  /** 返回新行的 rowId；[Food.id] 由数据库分配，调用方传 0 即可。 */
  @Insert
  suspend fun insert(food: Food): Long

  @Update
  suspend fun update(food: Food)

  @Delete
  suspend fun delete(food: Food)
}
