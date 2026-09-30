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
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 食材库本地数据库。单文件、无网络、随 App 私有目录存放。
 *
 * `exportSchema = true`：自 v2 起导出 schema 到 `app/schemas/` 并随仓库提交 ——
 * 引入 `Migration` 后必须先有可比对的 schema，迁移正确性才有据可查。
 * ⚠️ v1 的 schema 未导出（当时还是 `false`），所以 `app/schemas/` 里只有 v2 起。
 * v1 → v2 这条迁移因此**不靠 schema 比对**，靠在**装着 v1 库的真机**上升级安装来验
 * （v1 库里有用户录的食材，迁移若缺/写错，Room 会在打开时直接抛异常）。
 *
 * 注意这里**不用** `fallbackToDestructiveMigration()`：那会在版本升级时静默清库，
 * 对用户手输的食材是不可接受的代价。宁可升级时显式写迁移。
 */
@Database(entities = [Food::class], version = 2, exportSchema = true)
abstract class FoodDatabase : RoomDatabase() {

  abstract fun foodDao(): FoodDao

  companion object {
    private const val DB_NAME = "zifit.db"

    /**
     * v1 → v2：移除 `ediblePercent`（食部）与 `sodiumMg`（钠）两列。
     *
     * 走「建新表 → 搬数据 → 换名」而不是 `ALTER TABLE … DROP COLUMN`：
     * DROP COLUMN 要 SQLite **3.35+**，而本项目 minSdk 26（Android 8）自带的是 3.18，
     * 用它在旧机上会直接报语法错误。重表法在任何版本都成立。
     *
     * 版本号只能用 `RoomDatabase` 生成的 hash 校验通过的那一种建表语句 —— 也就是与
     * `FoodDatabase_Impl` 里 `createAllTables` 逐字一致的那条，否则 Room 打开后
     * 校验 schema 会失败。
     */
    val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_new` (" +
              "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
              "`name` TEXT NOT NULL, " +
              "`energyKcal` REAL, " +
              "`proteinG` REAL, " +
              "`fatG` REAL, " +
              "`carbohydrateG` REAL, " +
              "`origin` TEXT NOT NULL, " +
              "`createdAt` INTEGER NOT NULL)"
          )
          db.execSQL(
            "INSERT INTO `food_new` " +
              "(`id`, `name`, `energyKcal`, `proteinG`, `fatG`, `carbohydrateG`, `origin`, `createdAt`) " +
              "SELECT `id`, `name`, `energyKcal`, `proteinG`, `fatG`, `carbohydrateG`, `origin`, `createdAt` " +
              "FROM `food`"
          )
          db.execSQL("DROP TABLE `food`")
          db.execSQL("ALTER TABLE `food_new` RENAME TO `food`")
        }
      }

    @Volatile private var instance: FoodDatabase? = null

    /** 进程内单例。Room 的实例本身线程安全，重复创建才会真正打开多个连接。 */
    fun get(context: Context): FoodDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(context.applicationContext, FoodDatabase::class.java, DB_NAME)
              .addMigrations(MIGRATION_1_2)
              .build()
              .also { instance = it }
        }
  }
}
