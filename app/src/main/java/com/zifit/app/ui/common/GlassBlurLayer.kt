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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.zifit.app.theme.HeaderScrim
import com.zifit.app.theme.HeaderScrimSolid

/**
 * 实时背景模糊层：把 [Backdrop] 录下的**整屏画面**，按本节点在屏幕上的位置反向平移、
 * 裁进本节点范围后画出来 —— 于是"从本层下面滚过去的内容"就变成了糊掉的色块。
 *
 * 这就是"毛玻璃"的物理含义，也是它和"叠一层半透明白"的分水岭：
 * 白纱只是把背后的内容**压淡**（内容仍然清晰可辨），模糊是把它**化成色块**。
 * 凡是"内容会从它下面滚过去"的悬浮层都该用它：底部 dock、页面内的悬浮筛选头/操作栏。
 *
 * ⚠️ **模糊不是在本地做的**。它由 [Backdrop] 在录制整屏时烘进模糊副本里，
 * 本组件只负责**平移对齐 + 裁剪**。这一点必须记住，否则很容易"顺手加个
 * `Modifier.blur` / `graphicsLayer { renderEffect }` 来调半径"—— 那条路已经被证明不生效
 * （原因见 [Backdrop] 的类注释），只会得到"什么都没发生"。
 *
 * ## ⚠️ 本组件**绝对不能**加 `Modifier.graphicsLayer`（含 `clipToBounds`）
 *
 * 这是本文件最贵的一条教训，2026-09-30 真机踩实过一次（**整机 ANR 前的 RenderThread 栈溢出**，
 * tombstone：`Cause: stack pointer is not in a rw map; likely due to stack overflow`，
 * 512 帧全是 `hwui::computeTransformImpl`）。
 *
 * 原因：本组件**在页面内容里**（不像 dock 在录制层之外），于是它自己是 backdrop 节点的一棵子树。
 * 若它带 `graphicsLayer`，就产生一对互相引用的 RenderNode：
 *
 * ```
 * backdrop 节点的显示列表 ──含──▶ 本层的 RenderNode
 * 本层的显示列表     ──含──▶ drawRenderNode(backdrop 节点)      ← 采样背景
 * ```
 *
 * HWUI 在 `prepareTree` 时按这对引用设父子指针，于是 `A.parent = B` 且 `B.parent = A`，
 * 算变换时无限递归 → 栈溢出、进程被杀。
 *
 * 关键点：**这个环与「录制遍里跳过自己」无关**。那一遍只是让本层内容为空，
 * 但 `graphicsLayer` 节点本身仍会被录进 backdrop 的显示列表，"引用"照样成立。
 * 唯一彻底的办法是**不产生第二个 RenderNode**，所以：
 *
 * - 裁剪改用 `DrawScope` 的**画布裁剪**（`saveLayer(bounds, …)` 自带裁剪），不用 `clipToBounds`；
 * - 遮罩的离屏合成改用原生 `Canvas.saveLayer` —— 它是 Skia 的图层栈，**不是 RenderNode**，
 *   因此不会进入 HWUI 的 RenderNode 父子链。
 *
 * 另外两个必须一起满足的条件（缺一个就会看到"没生效"，`GlassDock` 的注释里有同样的清单）：
 *
 * 1. **层级顺序：本层在底、内容在顶。** 本层画的是整屏副本，
 *    排在它下面的东西会被整块盖住（白纱用 [overlay] 并进来，不另开节点，正是为了少踩这个坑）。
 * 2. **本层自身不能有子内容。** 它是"背景"，任何子节点都会被自己盖住；
 *    内容要画在它**后面**（先声明本层，再声明内容）。
 *
 * @param fadeMask 可选的 alpha 渐隐遮罩，用 [BlendMode.DstIn] 抠掉本层对应位置的透明度，
 *   让模糊**柔和地淡出**而不是切出一条硬边。悬浮筛选头必须传它：
 *   头部底缘要与列表自然衔接，一块矩形的模糊区会在那里留下一条肉眼可见的直线。
 *   `null` = 整块均匀模糊（dock 就是这种，它的形状由外层的 `clip` 决定），且**不开离屏层**。
 * @param overlay 叠在模糊**之上**的半透明纱（悬浮头就是 [headerScrimBrush]）。传 `null`
 *   表示不放纱（dock 用 `DockFill*` 作填充，属于"内容"而不是这里的"底"）。
 */
@Composable
fun GlassBlurLayer(modifier: Modifier = Modifier, fadeMask: Brush? = null, overlay: Brush? = null) {
  val backdrop = LocalBackdrop.current

  // 本节点在屏幕（= 录制层坐标系）中的位置。录制的整屏副本原点在屏幕左上，
  // 而本节点的绘制原点在自己左上，所以要按这个位移**反向**平移才能对齐。
  var origin by remember { mutableStateOf(Offset.Zero) }

  // 离屏层用空 Paint：只为隔离 blend，不需要任何绘制属性。
  val layerPaint = remember { Paint() }

  Box(
    modifier =
      modifier
        .onGloballyPositioned { origin = it.positionInRoot() }
        .drawBehind {
          /*
           * ⚠️ 录制遍里整层让位（连白纱也不画）：那一遍画的画面是**给悬浮层当背景**用的。
           * 把悬浮层自己画进去，模糊会把自己的白纱也糊一遍、再叠一遍，柔化层凭空厚一档，
           * "能看见背后色块"就没了。见 [isBackdropRecording]。
           *
           * 注意：这里的"让位"只解决**观感**，不解决上面说的环 ——
           * 解环靠的是本组件不带 `graphicsLayer`，两者都要有。
           */
          val recorder = backdrop
          if (recorder == null || isBackdropRecording) {
            // 录制遍整层不画；没有模糊时（API < 31 / 用户关掉毛玻璃）白纱就是全部的"底"
            if (recorder == null && overlay != null) drawRect(brush = overlay)
            return@drawBehind
          }

          val mask = fadeMask
          if (mask == null) {
            // dock：形状由外层圆角 clip 兜，不需要离屏层
            drawBackdrop(recorder, origin)
            return@drawBehind
          }

          /*
           * 悬浮头：必须离屏合成，否则 `DstIn` 会去抠**父级**（整屏）已经画好的像素，
           * 结果是本层没淡出、列表被抠出一片窟窿。
           * `saveLayer` 的 bounds 同时就是本层的裁剪框 —— 录制内容通常是一整屏，不裁会拖到列表上。
           */
          val bounds = Rect(Offset.Zero, size)
          drawIntoCanvas { canvas -> canvas.saveLayer(bounds, layerPaint) }
          drawBackdrop(recorder, origin)
          // 抠出渐隐：必须在模糊**之后**、白纱**之前**
          drawRect(brush = mask, blendMode = BlendMode.DstIn)
          if (overlay != null) drawRect(brush = overlay)
          drawIntoCanvas { canvas -> canvas.restore() }
        }
  )
}

/**
 * 把录制层按「录制块原点 − 悬浮层原点」平移后画出来，于是背后的内容与屏幕上严丝合缝。
 *
 * 换算：内容像素 `p` 在录制层里的坐标是 `p - nodeOrigin`，而它该落在悬浮层的
 * `p - layerOrigin`，两者差 `nodeOrigin - layerOrigin`。
 */
private fun DrawScope.drawBackdrop(backdrop: Backdrop, layerOrigin: Offset) {
  // 版本守卫不能省：lint 只看类型，认不出"这里的 Backdrop 一定来自 API 31+ 的录制器"
  if (!backdropBlurSupported) return
  translate(
    left = backdrop.nodeOrigin.x - layerOrigin.x,
    top = backdrop.nodeOrigin.y - layerOrigin.y,
  ) {
    drawLayer(backdrop.layer)
  }
}

// ---------------------------------------------------------------------------
// 悬浮筛选头 / 操作栏的柔化层
// ---------------------------------------------------------------------------

/**
 * 柔化层满强度段的终点：顶部 78% 保持满强度，最后 22% 淡出。
 *
 * ⚠️ **白纱与模糊遮罩必须共用这一组位置**。各写一套迟早会错位，而错位的表现很隐蔽 ——
 * 底缘出现一条"模糊已经断了、白纱还在"（或反过来）的带子，看着像渲染 bug。
 */
private const val ScrimSolidEnd = 0.78f

/**
 * 悬浮头的**白纱**：半透明玻璃底 + 底部淡出。
 *
 * 它与 [GlassBlurLayer] 是叠加关系而不是二选一：模糊把背后的内容化成色块，
 * 白纱再把整体压淡一档，胶囊上的文字才压得住。只有模糊没有白纱，深色背景下文字会浮。
 *
 * **取色分两档**（见 `ZiFitPalette.headerScrim` / `headerScrimSolid`）：
 * 有模糊时用较透的一档 —— 背后已是色块，纱再厚就把模糊盖没了；没有模糊时用较厚的一档，
 * 因为那时背后内容是**清晰**的，纱不够厚就会文字压文字。
 */
@Composable
fun headerScrimBrush(): Brush {
  val scrim = if (LocalBackdrop.current != null) HeaderScrim else HeaderScrimSolid
  return remember(scrim) {
    Brush.verticalGradient(0f to scrim, ScrimSolidEnd to scrim, 1f to Color.Transparent)
  }
}

/**
 * [GlassBlurLayer] 的**渐隐遮罩**：与 [headerScrimBrush] 同一组 stop。
 *
 * `DstIn` 只看 alpha、不看颜色，所以固定用白 —— 这也让它成了不依赖主题的顶层常量，
 * 不必每次重组新建 Brush。
 */
val HeaderFadeMask: Brush =
  Brush.verticalGradient(0f to Color.White, ScrimSolidEnd to Color.White, 1f to Color.Transparent)
