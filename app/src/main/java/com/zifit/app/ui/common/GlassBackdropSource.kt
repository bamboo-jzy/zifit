/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * 给**本页**建一个录制器，供页内悬浮层（悬浮筛选头这类"压在内容之上、内容从它下面滚过去"的
 * 东西）做实时背景模糊。
 *
 * ## 为什么页内要单独有一个，根节点那个不能直接用
 *
 * 详见 [Backdrop] 类注释里的"不允许成环"一节 —— 一句话：根录制节点会把 NavDisplay 的图形层
 * 一起录进去，而悬浮头在那个图形层**里面**，于是 `根节点 → 图形层 → 根节点` 成环，
 * RenderThread 会栈溢出把进程打掉。页内录制器录的范围在那个图形层**之下**，环就不存在。
 *
 * ## 用法（两个都是必须的，缺一个就等于没建）
 *
 * ```kotlin
 * val pageBackdrop = rememberPageBackdrop()
 * CompositionLocalProvider(LocalBackdrop provides pageBackdrop) {
 *   Box(...) {
 *     GlassBackdropSource(Modifier.fillMaxSize()) { /* 会被模糊的内容 */ }
 *     Box(Modifier.align(Alignment.TopCenter)) { /* 悬浮头：内含 GlassBlurLayer */ }
 *   }
 * }
 * ```
 *
 * ⚠️ 悬浮头必须落在 [GlassBackdropSource] **之外**（是它的兄弟而不是子节点）：
 * 录进去的话就会"自己糊自己"，而且同样会成环。
 *
 * @return 根上没有录制器（关掉了毛玻璃 / API < 31）时返回 `null`，此时
 *   [GlassBackdropSource] 只当普通容器、[GlassBlurLayer] 退回半透明实底 —— 调用处不需要写分支。
 */
@Composable
fun rememberPageBackdrop(): Backdrop? {
  // 根上没有录制器（关掉了毛玻璃 / API < 31）时整页都不必录
  if (LocalBackdrop.current == null) return null
  // 以 ambient 变化为准：开关一拨就换一个新录制器，不会留着上一份的画面
  return rememberBackdrop()
}

/**
 * 把 [content] 额外录一份进当前 [LocalBackdrop]，供本页悬浮层采样。
 *
 * 它自己不画任何东西 —— 内容照常渲染到屏幕上（`drawContent()`），录像是"顺手"做的。
 * 根上没有录制器时它就是一层普通 [Box]，零开销。
 *
 * ⚠️ 只包**会被悬浮层压住的内容**（滚动列表、页面主体）。悬浮头自己不能包进来，见
 * [rememberPageBackdrop]。
 */
@Composable
fun GlassBackdropSource(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val backdrop = LocalBackdrop.current

  // 本块左上角在根坐标系里的位置：录制内容相对本块存，悬浮层按屏幕位置取样，
  // 两者差一个原点，见 [Backdrop.nodeOrigin]。
  var origin by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier =
      modifier
        .onGloballyPositioned { origin = it.positionInRoot() }
        .then(
          /*
           * `backdropBlurSupported` 不能省：lint 只看类型，`LocalBackdrop` 里是 null 还是
           * 实例它推断不出来，必须由这个显式版本守卫来放行 `Backdrop` 上的 API 31 成员。
           */
          if (backdrop != null && backdropBlurSupported) {
            Modifier.drawWithContent {
              // 先录（嵌套在别的录制里时它会自己让位），再正常画到屏幕
              backdrop.record(this, layoutDirection, origin)
              drawContent()
            }
          } else {
            Modifier
          }
        )
  ) {
    content()
  }
}
