/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.data.repdb

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 分类枚举的中文映射。
 *
 * 刻意不依赖随包数据集 —— 数据集在 .gitignore 里，CI 与干净 clone 下并不存在，
 * 测试若依赖它就会在别人机器上红。
 */
class ExerciseTaxonomyTest {

  @Test
  fun bodyPartIsTranslated() {
    assertEquals("胸部", ExerciseTaxonomy.bodyPart("chest"))
    assertEquals("大腿", ExerciseTaxonomy.bodyPart("upper_legs"))
  }

  @Test
  fun equipmentNullMeansBodyweight() {
    assertEquals("徒手", ExerciseTaxonomy.equipment(null))
  }

  @Test
  fun muscleAndTagAndGoalAreTranslated() {
    assertEquals("胸大肌", ExerciseTaxonomy.muscle("pectoralis_major"))
    assertEquals(listOf("增肌", "力量"), ExerciseTaxonomy.goals(listOf("hypertrophy", "strength")))
    assertEquals(listOf("推日", "膝友好"), ExerciseTaxonomy.tags(listOf("push_day", "knee_safe")))
    assertEquals("复合动作", ExerciseTaxonomy.mechanic("compound"))
  }

  /**
   * 数据集升级可能带来新枚举值。此时应降级为「下划线换空格」的可读英文，
   * 而不是抛异常或露出空白 —— 界面绝不因缺一个译名而坏掉。
   */
  @Test
  fun unknownKeyFallsBackToReadableForm() {
    assertEquals("some new thing", ExerciseTaxonomy.bodyPart("some_new_thing"))
    assertEquals("brand new gear", ExerciseTaxonomy.equipment("brand_new_gear"))
  }

  /**
   * 筛选分类 = 面向直觉的词表，不是数据集的原样部位：
   * 腿与臂各自合并成一类（用户按「腿」找，不按大腿小腿找）。
   */
  @Test
  fun legsAndArmsAreMerged() {
    assertEquals(listOf("腿部"), ExerciseCategory.of("upper_legs", "strength", emptyList()))
    assertEquals(listOf("腿部"), ExerciseCategory.of("lower_legs", "strength", emptyList()))
    assertEquals(listOf("手臂"), ExerciseCategory.of("upper_arms", "strength", emptyList()))
    assertEquals(listOf("手臂"), ExerciseCategory.of("lower_arms", "strength", emptyList()))
  }

  /** 臀部分类不来自 `body_part`（数据集里没有这一项），而是由主要发力肌推出。 */
  @Test
  fun glutesComeFromPrimaryMuscles() {
    assertEquals(
      listOf("腿部", "臀部"),
      ExerciseCategory.of("upper_legs", "strength", listOf("gluteus_maximus", "quadriceps")),
    )
    // 只含次要肌不算 —— 判定只看主要发力肌，否则「臀部」会膨胀到几乎整个下肢
    assertEquals(
      listOf("腿部"),
      ExerciseCategory.of("upper_legs", "strength", listOf("quadriceps")),
    )
  }

  /** 有氧同理：来自 `category`，一条动作可同时属于「有氧」与「全身」。 */
  @Test
  fun cardioComesFromCategory() {
    assertEquals(listOf("有氧", "全身"), ExerciseCategory.of("full_body", "cardio", emptyList()))
  }

  /**
   * 数据集没有的维度不硬造分类：颈肌在 601 条里一条都没有。
   * 这条断言是**决策留痕** —— 将来若以此质疑「为什么没有颈部」，答案在这里。
   */
  @Test
  fun unsupportedBodyPartYieldsNoCategory() {
    assertEquals(emptyList<String>(), ExerciseCategory.of("neck", "strength", listOf("neck_flexors")))
  }

  /** 顺序取词表声明序，而不是中文的 Unicode 码位（后者会排成「全身、有氧、胸部」）。 */
  @Test
  fun categoriesFollowPaletteOrder() {
    val sorted = ExerciseCategory.sort(listOf("全身", "有氧", "胸部", "腿部"))
    assertEquals(listOf("胸部", "腿部", "有氧", "全身"), sorted)
  }

  /** 词表之外的标签排到最后，不能被丢掉。 */
  @Test
  fun sortingKeepsUnknownLabelsLast() {
    val sorted = ExerciseCategory.sort(listOf("未知分类", "胸部"))
    assertEquals(listOf("胸部", "未知分类"), sorted)
  }

  /** 词表本身不能有重复或先后次序上的意外。 */
  @Test
  fun paletteIsOrderedAndUnique() {
    assertEquals(listOf("胸部", "背部", "肩部", "腿部", "臀部", "手臂", "核心", "有氧", "全身"),
      ExerciseCategory.palette)
  }

  /** 难度由易到难：入门 < 进阶 < 高阶，权重取词表声明序。 */
  @Test
  fun difficultyRanksFromEasyToHard() {
    assertEquals(
      listOf("入门", "进阶", "高阶"),
      listOf("beginner", "intermediate", "advanced").map(ExerciseTaxonomy::difficulty),
    )
    val labels = listOf("beginner", "intermediate", "advanced").map(ExerciseTaxonomy::difficulty)
    val ranks = labels.map(ExerciseTaxonomy::difficultyRank)
    assertEquals(3, ranks.distinct().size)
    assertEquals(ranks.sorted(), ranks)
  }

  /**
   * 排序按词表声明序。**不要改成对中文标签直接 `sorted()`**：那排的是 Unicode 码位，
   * 当前这组「入门 / 进阶 / 高阶」恰与码位序一致纯属巧合，换一组词（如 新手 / 熟练 / 专家）立刻乱。
   */
  @Test
  fun sortingByDifficultyUsesDeclarationOrder() {
    val labels = listOf("高阶", "入门", "进阶")
    assertEquals(
      listOf("入门", "进阶", "高阶"),
      labels.sortedBy(ExerciseTaxonomy::difficultyRank),
    )
  }

  /** 未知难度排到最后，而不是混进「入门」里。 */
  @Test
  fun unknownDifficultyRanksLast() {
    assertEquals(Int.MAX_VALUE, ExerciseTaxonomy.difficultyRank("专家"))
    assertEquals(Int.MAX_VALUE, ExerciseTaxonomy.difficultyRank(null))
  }
}
