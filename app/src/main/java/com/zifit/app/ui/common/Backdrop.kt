/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.common

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** 背景模糊半径。全站统一，改这里就改了所有悬浮层的磨砂颗粒感。 */
val BackdropBlurRadius = 28.dp

/**
 * 是否有任意一个 [Backdrop] 正在录制。
 *
 * 页面级录制器**嵌在**根录制器之内（页面就在根内容里），嵌套时内层必须整个让位：
 * 不让位的话同一块内容会被多录一遍，纯属浪费。
 *
 * 绘制只在主线程发生，普通变量即可，不需要 `@Volatile`。
 */
private var recordingInProgress = false

/**
 * 是否有**任意**录制正在进行。悬浮层（[GlassBlurLayer]）据此让位：true 就什么都不画。
 *
 * 两个理由，缺一不可：
 * 1. **语义**：背景模糊该糊的是**背后**的内容，不含悬浮层自己。不跳过的话白纱会被模糊
 *    一遍再叠一遍，比标称值厚一档，"能透出背后色块"就没了。
 * 2. **跨录制器**：只看"自己那个录制器是不是在录"不够 —— 根录制器开始录制时，页内悬浮头
 *    用的是另一个录制器，它并不知道，会照画不误。那一遍画出来的东西会被烘进根录制层，
 *    既是白费一次绘制，也会在根录制层里残留一份"上一帧的悬浮头"。
 *
 * 绘制只在主线程发生，普通变量即可，不需要 `@Volatile`。只在**绘制阶段**被读写，
 * 不参与快照与重组，所以不必（也不该）是 `State`。
 */
internal val isBackdropRecording: Boolean
  get() = recordingInProgress

/**
 * 每帧录制的「背景画面」，供悬浮层做**实时背景模糊**。
 *
 * 悬浮层采样 [layer] / [drawBlurredTo] 就能拿到"背后真实画面的模糊版" —— 这就是毛玻璃：
 * 模糊的是**背后滚动过去的内容**，而不是叠一层半透明白。
 *
 * ## 为什么内容必须用 `GraphicsLayer.record`，不能自己往 `RenderNode` 里画
 *
 * 一开始是用原生 `RenderNode` + `beginRecording()` 自绘的，并且为了"让 `drawContent()`
 * 画到 node 的画布上"，临时把 `owner.drawContext.canvas` 换成 node 的录制画布。
 * **这条路是错的，而且错得很隐蔽**（2026-09-30 用自绘红块探针判明）：
 *
 * - `drawIntoCanvas { }` 会直接读 `drawContext.canvas`，所以"换画布"对它有效 —— 探针红块
 *   确实出现在模糊层里，看着像成了；
 * - 但 `drawContent()` **不走** `drawContext.canvas`，它由框架在内部拿着自己的画布去画子树。
 *   于是录出来的是"一片空白 + 红块"，真内容一个像素都没进去。表现为：模糊层位置、白纱、
 *   几何全对，就是背后内容**依旧清晰**（像没开模糊）。
 *
 * `GraphicsLayer.record(density, layoutDirection, size) { … }` 是 Compose 官方为此提供的入口，
 * 它内部正确地完成了这次画布切换，内容才真的录进去。所以**不要再回头去手写 RenderNode 录制**。
 *
 * ## ⚠️ 谁可以引用本层：**绝对不允许成环**
 *
 * 录制本质上是一份"指向子节点的引用"。HWUI 在 `prepareTree` 时按引用设置 `mParent`；
 * "谁画了本层"与"本层里画了谁"一旦撞上就成环，算变换时无限递归
 * → **RenderThread 栈溢出、进程当场被杀**（tombstone:
 * `Cause: stack pointer is not in a rw map; likely due to stack overflow`，512 帧全是
 * `hwui::computeTransformImpl`）。踩过一次，环是这样长出来的：
 *
 * ```
 * 根录制层 ──含──▶ NavDisplay 的图形层 N        （N 是被录下来的内容的一部分）
 * N       ──含──▶ 悬浮筛选头的绘制             （悬浮头在页面里，页面在 N 里）
 * 悬浮头   ──含──▶ 采样根录制层                （→ 回到第一行）
 * ```
 *
 * 所以要**把录制范围落到 N 之下** —— 这就是页面级录制器
 * （[GlassBackdropSource] + [rememberPageBackdrop]）存在的原因，它录的是"悬浮头下方那块内容"。
 *
 * 同理，**悬浮层自己不能带 `Modifier.graphicsLayer`**（含 `clipToBounds`）：那会平白多出
 * 一个 RenderNode 去引用本层，而它自己又在本层里 —— 又是一个环。
 * 遮罩的离屏合成因此改用原生 `Canvas.saveLayer`（Skia 的图层栈，不进 HWUI 的 RenderNode 树）。
 *
 * ## 每帧的绘制顺序
 *
 * ```
 * ① record()      → 内容录进 layer（本遍里悬浮层自己让位，见 [isBackdropRecording]）
 * ② drawContent() → 内容再画一遍到屏幕；此刻悬浮层采样 layer，拿到的是**本帧**的模糊
 * ```
 *
 * 顺序不能反：layer 必须先录满本帧内容，悬浮层才取得到本帧的模糊。
 *
 * ## 成本
 *
 * 每个录制器每帧多画 **1 遍**内容 + 一次离屏栅格化（模糊本身）。这是背景模糊的固有代价，
 * 也因此录制器**不要随手加**，只在真有悬浮层要用时才建。
 */
@RequiresApi(Build.VERSION_CODES.S)
class Backdrop(
  /**
   * 录制内容的层，由 [rememberBackdrop] 通过 `rememberGraphicsLayer()` 提供（构造器不公开，
   * 只能这样拿到；直接 `GraphicsLayer()` 需要 `GraphicsLayerImpl`，那是内部 API）。
   *
   * 悬浮层用 `drawLayer(layer)` 采样它 —— 采样时记得按 [nodeOrigin] 反向平移。
   */
  val layer: GraphicsLayer,
) {

  private var effectApplied = false

  /**
   * 本录制块的左上角在**根**坐标系里的位置。
   *
   * 录制内容是相对本块自己左上角存的，而悬浮层按自己在屏幕上的位置反向平移来对齐，
   * 所以要记下这个原点才能换算（见 [drawBlurredTo]）。
   */
  var nodeOrigin = Offset.Zero
    private set

  /**
   * 录制一帧，返回是否真的录了。
   *
   * 必须在 `Modifier.drawWithContent` 里调用 —— [owner] 就是那个 [ContentDrawScope]，
   * 由它来画内容（`drawContent()`）。
   *
   * 返回 `false` 的两种情况，调用处的处理都一样：**照常 `drawContent()` 即可**
   * （内容由外层录制器捕获，或者这一帧本来就不需要模糊）。
   *
   * @param origin 本录制块左上角在根坐标系里的位置，见 [nodeOrigin]。
   */
  fun record(owner: ContentDrawScope, layoutDirection: LayoutDirection, origin: Offset): Boolean {
    // 嵌套：外层正在录，本层让位（否则同一块内容会被多录一遍，且录的是别人的画布）
    if (recordingInProgress) return false

    val width = owner.size.width.toInt()
    val height = owner.size.height.toInt()
    if (width <= 0 || height <= 0) return false

    if (!effectApplied) {
      // 半径只跟屏幕密度有关，整套 App 生命周期内不变 —— 设一次就够
      val blurPx = with(owner) { BackdropBlurRadius.toPx() }
      layer.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
      effectApplied = true
    }

    nodeOrigin = origin
    recordingInProgress = true
    try {
      layer.record(owner, layoutDirection, IntSize(width, height)) { owner.drawContent() }
    } finally {
      recordingInProgress = false
    }
    return true
  }
}

/**
 * 当前生效的录制器。`null` 表示**这一帧没有可采样的背景**，两种来源：
 * 1. API < 31 没有 `RenderEffect`（[backdropBlurSupported] 为 false）；
 * 2. 用户在「用户界面」里关掉了毛玻璃模糊（根节点连录制都省了）。
 *
 * 悬浮层不必区分这两者 —— 拿到 `null` 就退回"半透明实底"（这正是关掉开关后的样子）。
 */
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 建一个录制器。API < 31 上直接返回 `null`（没有 `BlurEffect`，录了也没人能用）。
 *
 * ⚠️ 每次调用都会新建一个**独立**的 `GraphicsLayer`。同一个 `GraphicsLayer` 被两处录制会
 * 互相覆盖，所以"根一个、页内一个"必须是两个不同的 [Backdrop]。
 */
@Composable
fun rememberBackdrop(): Backdrop? {
  if (!backdropBlurSupported) return null
  val layer = rememberGraphicsLayer()
  // 以 layer 为 key：层被回收（离开组合）时连带丢掉录制器，不会留着一份悬空的引用
  return remember(layer) { Backdrop(layer) }
}

/**
 * 实时模糊是否可用：`BlurEffect` / `RenderEffect` 自 **API 31 (S)** 起才有。
 *
 * 不可用时悬浮层自动退回"高不透明度填充"方案，因此 minSdk 26 上观感会略实一点，
 * 但不会出现空白或崩溃。
 *
 * 标了 [ChecksSdkIntAtLeast]，调用处才能写 `if (backdropBlurSupported) Backdrop()` 而
 * 不被 lint 判成"在低版本上调用 API 31+ 的 `GraphicsLayer.record` / `renderEffect`"。
 *
 * ⚠️ 注解必须写成 `@get:` 用点目标：只写 `@ChecksSdkIntAtLeast` 的话 Kotlin 会把它落到
 * **字段**上，lint 读不到 getter，守卫就白写了（实测踩过，表现为 lint 连报 4 个 NewApi）。
 */
@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
val backdropBlurSupported: Boolean
  get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
