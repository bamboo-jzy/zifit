/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 动作库：健身动作条目、目标肌群与动作要领。 */
@Serializable data object ExerciseLibrary : NavKey

/** 训练：训练计划与训练记录。 */
@Serializable data object Training : NavKey

/** 饮食：饮食计划与每日饮食记录。 */
@Serializable data object Diet : NavKey

/** 食材库：食材营养数据，供饮食计划计算使用。 */
@Serializable data object FoodLibrary : NavKey

/** 我的：头像 + 用户名 + 设置入口，下面是身体数据。 */
@Serializable data object Profile : NavKey

/**
 * 设置页（我的标签内的二级页面）：个人资料 / AI 接入 / 关于三行。
 *
 * 命名带 `Profile` 前缀是因为 `Settings` 太通用 —— 将来饮食、训练各自有设置项时，
 * 一眼能看出这是「我的」下面的那一份。
 */
@Serializable data object ProfileSettings : NavKey

/** 个人资料设置页：名称 / 生日 / 年龄 / 目标 / 头像。 */
@Serializable data object ProfileEdit : NavKey

/** AI 接入设置页：服务地址 / 模型 / 密钥 + 连通性测试。 */
@Serializable data object AiSettings : NavKey

/**
 * 用户界面设置页：毛玻璃模糊 / 界面动效两个开关。
 *
 * 与 [ProfileEdit]、[AiSettings] 同挂在设置页下，共用「我的」标签那条返回栈。
 */
@Serializable data object UiSettings : NavKey

/** 关于页：许可声明与数据出处。 */
@Serializable data object About : NavKey

/**
 * 动作详情（动作库标签内的二级页面）。
 *
 * 只带 id 而不带整个动作对象：路由需可序列化，且详情页应能独立取数
 * （将来从训练计划里直接跳某个动作时，同样只需一个 id）。
 */
@Serializable data class ExerciseDetail(val exerciseId: String) : NavKey
