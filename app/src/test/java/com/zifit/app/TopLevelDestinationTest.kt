/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

/** 底部导航栏的结构约定：五个一级入口，顺序为 动作库 → 训练 → 饮食 → 食材库 → 我的。 */
class TopLevelDestinationTest {

  @Test
  fun hasFiveDestinations() {
    assertEquals(5, TopLevelDestination.entries.size)
  }

  @Test
  fun destinationsAreInExpectedOrder() {
    assertEquals(
      listOf(ExerciseLibrary, Training, Diet, FoodLibrary, Profile),
      TopLevelDestination.entries.map { it.key },
    )
  }

  @Test
  fun eachDestinationHasLabelAndIcon() {
    TopLevelDestination.entries.forEach { destination ->
      // labelRes 不再显示在 dock 上，仅作无障碍描述 —— 但仍是必需的
      assertTrue("${destination.name} 缺少标签资源", destination.labelRes != 0)
      assertTrue("${destination.name} 缺少图标资源", destination.iconRes != 0)
    }
  }

  @Test
  fun labelsAndIconsAreUnique() {
    val labels = TopLevelDestination.entries.map { it.labelRes }
    val icons = TopLevelDestination.entries.map { it.iconRes }
    assertEquals("标签资源有重复", labels.size, labels.toSet().size)
    assertEquals("图标资源有重复", icons.size, icons.toSet().size)
  }

  @Test
  fun startDestinationIsDiet() {
    assertEquals(TopLevelDestination.DIET, TopLevelDestination.START)
  }
}
