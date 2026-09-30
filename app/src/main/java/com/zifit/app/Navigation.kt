/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.zifit.app.theme.Accent
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.appearance.UiSettingsScreen
import com.zifit.app.ui.appearance.UiSettingsViewModel
import com.zifit.app.ui.common.Backdrop
import com.zifit.app.ui.common.DockItemHeight
import com.zifit.app.ui.common.DockItemWidth
import com.zifit.app.ui.common.DockMargin
import com.zifit.app.ui.common.GlassBackground
import com.zifit.app.ui.common.GlassDock
import com.zifit.app.ui.common.LocalBackdrop
import com.zifit.app.ui.common.backdropBlurSupported
import com.zifit.app.ui.common.rememberBackdrop
import com.zifit.app.ui.diet.DietScreen
import com.zifit.app.ui.exercise.ExerciseDetailScreen
import com.zifit.app.ui.exercise.ExerciseLibraryScreen
import com.zifit.app.ui.food.FoodLibraryScreen
import com.zifit.app.ui.profile.AboutScreen
import com.zifit.app.ui.profile.AiConfigScreen
import com.zifit.app.ui.profile.ProfileEditScreen
import com.zifit.app.ui.profile.ProfileScreen
import com.zifit.app.ui.profile.SettingsScreen
import com.zifit.app.ui.training.TrainingScreen

/** 选中图标的放大幅度（24dp → 28.3dp），幅度刻意小，避免挤压相邻项。 */
private const val DockSelectedScale = 1.18f

/**
 * App 的根导航：当前主题的毛玻璃背景之上悬浮一条玻璃 dock，切换标签即切换 [NavDisplay] 展示的页面。
 *
 * 整个"背景 + 页面内容"每帧被录制进一个整屏层（[Backdrop]，`GraphicsLayer` + `BlurEffect`），
 * 下传给悬浮层做**实时背景模糊**；因此页面内容若滚动到 dock / 悬浮筛选头之下，
 * 也会被一并模糊。
 *
 * **每个一级标签各自维护返回栈** —— 在动作库里点进某个动作、切到饮食再切回来，
 * 仍停在那个动作的详情页，而不是被弹回列表。切标签只是换一条栈来渲染。
 */
@Composable
fun MainNavigation() {
  val stacks = TopLevelDestination.entries.map { rememberNavBackStack(it.key) }
  // 刻意用 val 持有 State 对象而非 `by` 委托：事件回调里需要读到**调用时刻**的索引，
  // 用委托变量会被闭包捕获成创建时的旧值。
  val currentIndex =
    rememberSaveable { mutableIntStateOf(TopLevelDestination.entries.indexOf(TopLevelDestination.START)) }

  /*
   * 整屏录制层（见 [Backdrop] 的类注释：为什么是 `GraphicsLayer` + `BlurEffect`）。
   * **API < 31 上根本不创建** —— 没有 `BlurEffect`，录了也没人能用。
   *
   * 它服务于**根这一层的悬浮层**（底部 dock）。页面内的悬浮头不能用它：
   * 根录制层会把 NavDisplay 的图形层一起录进去，而悬浮头在那个图形层里面，
   * 于是 `根层 → 图形层 → 根层` 成环，RenderThread 会栈溢出把进程打掉。
   * 页面内的悬浮头改用页内录制器（`rememberPageBackdrop`）。
   */
  val backdrop = rememberBackdrop()

  /*
   * 界面偏好在这里读：毛玻璃开关决定**是否录制整屏图形层**。
   * 它是"渲染路径的开关"而非颜色微调，所以真正的分支落在代码里（见下），
   * 而不是把某个常量调淡。**主题模式与主题色不在这里读** —— 那两个在 `MainActivity`
   * 就交给 `ZiFitTheme` 了，本文件只消费已生效的调色板（如 [Accent]/[InkMuted]）。
   */
  val context = LocalContext.current
  val uiViewModel: UiSettingsViewModel = viewModel(factory = UiSettingsViewModel.factory(context))
  val uiPreferences by uiViewModel.preferences.collectAsStateWithLifecycle()
  val glassBlur = uiPreferences.glassBlur

  // 页面外框：状态栏 / 挖孔让位 + 16dp 边距。做成变量由各 entry 自行附带，
  // 个别页面（动作库）借此可以不要外框，把柔化层铺到屏幕边缘。
  val screenFrame =
    Modifier
      .windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
      )
      .padding(horizontal = 16.dp, vertical = 8.dp)

  CompositionLocalProvider(LocalBackdrop provides if (glassBlur) backdrop else null) {
    Box(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier =
          Modifier.fillMaxSize().drawWithContent {
            val recorder = backdrop
            // 同时判 backdropBlurSupported：lint 只看类型，认不出"这里的 recorder 一定来自 API 31+"
            if (glassBlur && recorder != null && backdropBlurSupported) {
              /*
               * 先**录**（内容录进层），再**画屏幕**（`drawContent()` 自己那一遍）。
               *
               * 顺序不能反：悬浮层（dock）在 `drawContent()` 里采样那一层，
               * 必须取到**本帧**的画面。先录后画，采样到的是新鲜的；反过来会慢一帧，
               * 快速滑动时能在模糊里看出内容"追不上"。
               *
               * ⚠️ 内容因此**每帧画两遍**（录制一遍、屏幕一遍）。这是 `BlurEffect` 只能作用于
               * "本层自绘像素"的必然代价（详见 [Backdrop] 类注释）。
               *
               * 关掉毛玻璃时**连录制也省掉**：`LocalBackdrop` 传 null 已让悬浮层走实底兜底，
               * 再录一份没人看的整屏副本就是纯开销。这也是"毛玻璃开关"真正对应的代码分支。
               */
              recorder.record(this, layoutDirection, Offset.Zero)
            }
            drawContent()
          }
      ) {
        GlassBackground()

        NavDisplay(
          backStack = stacks[currentIndex.intValue],
          // 底部**刻意不避让** dock：加了就会把可滚动区截在 dock 之上，
          // 内容从此滚不到 dock 下面，毛玻璃也就无内容可"透"。
          // 避让改由各页面用 DockClearance 作为 contentPadding 完成。
          //
          // 页面外框（安全区 + 16dp 边距）**也刻意不在这里加**：加了就等于给所有页面
          // 套上同一副模具，动作库那种需要"柔化层铺满整屏"的页面就永远铺不满
          // （左右会各留一条可见直边）。改由下面各 entry 自己附带 [screenFrame]。
          modifier = Modifier.fillMaxSize(),
          onBack = {
            // 只在标签内有二级页面时消费返回；已到根页面则不动作，
            // 否则会把栈清空、NavDisplay 无从渲染。
            val stack = stacks[currentIndex.intValue]
            if (stack.size > 1) stack.removeLastOrNull()
          },
          entryProvider =
            entryProvider {
              entry<ExerciseLibrary> {
                // 刻意不给外框：动作库要全宽（见 ExerciseLibraryScreen 的柔化层）
                ExerciseLibraryScreen(
                  onExerciseClick = { exerciseId ->
                    stacks[currentIndex.intValue].add(ExerciseDetail(exerciseId))
                  }
                )
              }
              entry<Training> { TrainingScreen(modifier = screenFrame) }
              entry<Diet> { DietScreen(modifier = screenFrame) }
              entry<FoodLibrary> {
                // 与动作库同理，**刻意不给 screenFrame**：食材库的卡片宽度必须与动作库逐像素一致。
                // 套上 screenFrame 会在这里多加一层 16dp 水平内边距，再叠加页面自身
                // contentPadding 的 16dp，卡片实测会窄掉 32px（106..1118 vs 53..1171）。
                // 安全区与 16dp 边距由 FoodLibraryScreen 自己兜（与 ExerciseLibraryScreen 同款）。
                FoodLibraryScreen()
              }
              entry<Profile> {
                ProfileScreen(
                  onOpenSettings = { stacks[currentIndex.intValue].add(ProfileSettings) },
                  modifier = screenFrame,
                )
              }
              /*
               * 「我的」下的二级页面。它们共用同一条返回栈（就是「我的」标签那条），
               * 所以「设置 → 个人资料 → 返回」会回到设置页，而不是一路弹回首页。
               */
              entry<ProfileSettings> {
                SettingsScreen(
                  onBack = { stacks[currentIndex.intValue].removeLastOrNull() },
                  onOpenProfile = { stacks[currentIndex.intValue].add(ProfileEdit) },
                  onOpenAi = { stacks[currentIndex.intValue].add(AiSettings) },
                  onOpenUi = { stacks[currentIndex.intValue].add(UiSettings) },
                  onOpenAbout = { stacks[currentIndex.intValue].add(About) },
                  modifier = screenFrame,
                )
              }
              entry<ProfileEdit> {
                ProfileEditScreen(
                  onBack = { stacks[currentIndex.intValue].removeLastOrNull() },
                  modifier = screenFrame,
                )
              }
              entry<AiSettings> {
                AiConfigScreen(
                  onBack = { stacks[currentIndex.intValue].removeLastOrNull() },
                  modifier = screenFrame,
                )
              }
              entry<UiSettings> {
                UiSettingsScreen(
                  onBack = { stacks[currentIndex.intValue].removeLastOrNull() },
                  modifier = screenFrame,
                )
              }
              entry<About> {
                AboutScreen(
                  onBack = { stacks[currentIndex.intValue].removeLastOrNull() },
                  modifier = screenFrame,
                )
              }
              entry<ExerciseDetail> { key ->
                ExerciseDetailScreen(
                  exerciseId = key.exerciseId,
                  onBack = { stacks[currentIndex.intValue].removeLastOrNull() },
                  modifier = screenFrame,
                )
              }
            },
        )
      }

      GlassBottomBar(
        selectedIndex = currentIndex.intValue,
        onSelect = { index -> currentIndex.intValue = index },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}

/**
 * 底部悬浮玻璃 dock：**只有图标，无文字**（宽度随内容收缩并居中）。
 *
 * 中文标签改为无障碍描述而非可见文本 —— 五个图标可在窄屏上均分排布且不换行；
 * 若将来需要显示文字，把 `labelRes` 摆到图标下方即可，无需改 [TopLevelDestination]。
 */
@Composable
private fun GlassBottomBar(selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
  GlassDock(
    modifier =
      modifier
        // 浮在系统手势/导航区之上，四周留边以形成"悬浮"观感
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(vertical = DockMargin)
  ) {
    TopLevelDestination.entries.forEachIndexed { index, destination ->
      val selected = index == selectedIndex
      val label = stringResource(destination.labelRes)
      val tint by
        animateColorAsState(
          targetValue = if (selected) Accent else InkMuted,
          label = "dockTint",
        )
      val iconScale by
        animateFloatAsState(
          targetValue = if (selected) DockSelectedScale else 1f,
          animationSpec =
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
          label = "dockScale",
        )

      Box(
        modifier =
          Modifier.size(DockItemWidth, DockItemHeight)
            // 显式传 null：去掉 Material 默认的水波纹指示（点击时从图标处扩散的灰方块）
            .selectable(
              selected = selected,
              interactionSource = null,
              indication = null,
              role = Role.Tab,
              onClick = { onSelect(index) },
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(destination.iconRes),
          contentDescription = null,
          tint = tint,
          modifier =
            Modifier.size(24.dp)
              // 用缩放而非改 size：不影响布局，放大时不会推挤相邻图标
              .graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
              },
        )
      }
    }
  }
}
