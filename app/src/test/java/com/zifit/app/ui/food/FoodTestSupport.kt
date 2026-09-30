/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.food

import com.zifit.app.data.food.Food
import com.zifit.app.data.food.FoodDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

/**
 * 内存版 [FoodDao]。行为**对齐真实 SQL**：`observeAll` 按 `createdAt DESC, id DESC` 排序
 * —— 假实现若不排序，ViewModel 依赖的排序假设就永远测不到，测试会给出虚假的绿。
 */
internal class FakeFoodDao : FoodDao {

  val rows = MutableStateFlow<List<Food>>(emptyList())

  private var nextId = 1L

  /**
   * 直接铺一批数据，**绕过自增 id**，但**仍然过 [sorted]**。
   *
   * ⚠️ 不要把测试退回成 `dao.rows.value = listOf(...)`：那样赋值绕开了排序，
   * 于是「最新在前」这类断言测的是入参顺序、不是 DAO 的顺序，会给出虚假的绿
   * （或者像这次一样假红）。fake 的不变量必须是「rows 恒为排序后」。
   */
  fun setAll(list: List<Food>) {
    rows.value = sorted(list)
  }

  override fun observeAll(): Flow<List<Food>> = rows

  override suspend fun byId(id: Long): Food? = rows.value.firstOrNull { it.id == id }

  override suspend fun insert(food: Food): Long {
    val id = nextId++
    rows.value = sorted(rows.value + food.copy(id = id))
    return id
  }

  override suspend fun update(food: Food) {
    rows.value = sorted(rows.value.map { if (it.id == food.id) food else it })
  }

  override suspend fun delete(food: Food) {
    rows.value = rows.value.filterNot { it.id == food.id }
  }

  private fun sorted(list: List<Food>): List<Food> =
    list.sortedWith(compareByDescending<Food> { it.createdAt }.thenByDescending { it.id })
}

/**
 * 在测试的**后台作用域**里起一个收集者，并返回该 StateFlow 以便直接断言。
 *
 * 两处都不能省：
 * - 必须真的收集 —— `uiState` 用 `SharingStarted.WhileSubscribed` 起流，**没有订阅者就不工作**，
 *   不收集的话断言的永远是初始值；
 * - 必须用 `backgroundScope` —— 普通 `launch` 会让 `runTest` 一直等它结束，
 *   而 StateFlow 的 `collect` 永不结束，测试直接挂死。
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> StateFlow<T>.collectForTest(scope: TestScope): StateFlow<T> {
  scope.backgroundScope.launch { collect {} }
  return this
}
