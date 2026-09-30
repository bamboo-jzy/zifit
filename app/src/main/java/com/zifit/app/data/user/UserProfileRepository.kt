/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.user

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 个人资料的**唯一存取出口**（AGENTS.md §4 分层约束）。
 *
 * 做成接口的理由与 `AiConfigRepository` 相同：真实实现依赖 `SharedPreferences`，
 * JVM 单测里拿不到；接口化之后 `UserProfileViewModel` 可以喂内存版，
 * 「校验不过就不许落库」这类**逻辑**才测得到。
 */
interface UserProfileRepository {

  /** 当前资料。首页头部与编辑页的唯一数据源。 */
  val profile: StateFlow<UserProfile>

  fun save(profile: UserProfile)

  companion object {
    fun get(context: Context): UserProfileRepository =
      SharedPrefsUserProfileRepository(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      )

    private const val PREFS_NAME = "user_profile"
  }
}

/**
 * 用 `SharedPreferences` 而不是 Room：这是五个键值，不是表。
 *
 * 头像**只存路径**，图片本体在应用私有目录里（见 [AvatarStore]）——
 * 把图片塞进 SQLite 的 BLOB 只会让数据库变大、备份变慢，没有任何好处。
 */
class SharedPrefsUserProfileRepository(private val prefs: SharedPreferences) :
  UserProfileRepository {

  private val state = MutableStateFlow(read())

  override val profile: StateFlow<UserProfile> = state.asStateFlow()

  override fun save(profile: UserProfile) {
    prefs.edit {
      putString(KEY_NAME, profile.name)
      putString(KEY_BIRTHDAY, profile.birthday)
      // 年龄可能「没填」：`putInt` 存不了 null，用一个显式的存在性判断兜住，
      // 别拿 0 当「没填」——「0 岁」是个合法但错误的年龄。
      if (profile.age == null) remove(KEY_AGE) else putInt(KEY_AGE, profile.age)
      if (profile.goal == null) remove(KEY_GOAL) else putString(KEY_GOAL, profile.goal.name)
      putString(KEY_AVATAR, profile.avatarPath)
    }
    state.value = profile
  }

  private fun read(): UserProfile =
    UserProfile(
      name = prefs.getString(KEY_NAME, null).orEmpty(),
      birthday = prefs.getString(KEY_BIRTHDAY, null).orEmpty(),
      age = if (prefs.contains(KEY_AGE)) prefs.getInt(KEY_AGE, 0) else null,
      // 枚举名存盘：将来枚举增删时，读到不认识的值降级为「没选」，而不是抛异常打不开页面
      goal =
        prefs.getString(KEY_GOAL, null)?.let { name ->
          Goal.entries.firstOrNull { it.name == name }
        },
      avatarPath = prefs.getString(KEY_AVATAR, null).orEmpty(),
    )

  private companion object {
    const val KEY_NAME = "name"
    const val KEY_BIRTHDAY = "birthday"
    const val KEY_AGE = "age"
    const val KEY_GOAL = "goal"
    const val KEY_AVATAR = "avatar_path"
  }
}
