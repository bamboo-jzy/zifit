/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * 毛玻璃风格的色板。全部为不透明基色，透明度由使用处（GlassSurface / 背景光斑）叠加，
 * 便于单独调整通透度而不动色相。
 *
 * 调参经验：背景越浅、光斑越淡，玻璃面板越"看不见"。
 * 光斑要有足够饱和度、且尺寸小于半个屏宽，才能形成可被磨砂的色块。
 *
 * ⚠️ 2026-09-30 起这套色不再是**一组常量**，而是**四套调色板**（彩色·浅 / 浅色 /
 * 彩色·深 / 深色），外加一个可换的**主题色**。因此有两条硬规矩：
 * 1. **不要在业务代码里直接写 `Color(0x…)`**（透明黑这类纯覆盖色除外），
 *    也不要再往本文件加顶层常量 —— 那样的颜色在深色模式下不会跟着变，
 *    等于在深底上钉死一个浅色。
 * 2. 取色一律走下面的 `@Composable` getter（或 `LocalZiFitPalette.current`），
 *    它们读的是当前生效的调色板。
 */

/**
 * 主题模式（「我的 → 设置 → 用户界面 → 主题模式」）。
 *
 * 2026-09-30 第二轮改成**四档 2×2**：{彩色, 中性} × {浅, 深}。
 *
 * 起因是命名不再自洽：「彩色」与「深色」并列时，读起来像"彩色 与 深色"二选一，
 * 而实际上那两个是**同一套光斑配色的明暗两版**；真正与「浅色」对应的深色版本
 * （中性深色）当时并不存在。于是拆成两条互相垂直的轴：
 *
 * | | 浅（白天） | 深（夜间） |
 * |---|---|---|
 * | 彩色（有彩色光斑） | [COLORFUL_LIGHT] | [COLORFUL_DARK] |
 * | 中性（光斑退成钢灰） | [NEUTRAL_LIGHT] | [NEUTRAL_DARK] |
 *
 * ⚠️ **"彩色"指的是光斑有没有色相，不是底色深浅** —— 决定明暗的是 `isDark`，
 * 看调色板字段，别照着枚举名推。
 */
enum class ThemeMode {
  /**
   * 彩色·浅：冷灰蓝渐变 + 四团彩色光斑。
   * 这是 2026-09-30 之前唯一的那一版 —— 加了一堆模式之后，默认值仍是它。
   */
  COLORFUL_LIGHT,

  /**
   * 浅色：与彩色·浅同一套版式与几何，但**光斑与基底全部去饱和**，退成银灰玻璃。
   * 不是"更浅的彩色"：彩色·浅的底色已经到顶了，再浅玻璃就看不见了；
   * 浅色换的是**色相策略**（无色相），靠明暗层次撑住玻璃。
   */
  NEUTRAL_LIGHT,

  /**
   * 彩色·深：彩色·浅的深色版 —— 深底 + **同一组彩色光斑**（只是取更深沉的色号）。
   * 2026-09-30 之前它就叫「深色」；改名是因为"深色"该留给中性那一列。
   */
  COLORFUL_DARK,

  /**
   * 深色：与浅色对应的中性深色 —— 深底 + **去饱和的钢灰光斑**。
   * 整页没有任何色相，与「浅色」是同一套配色策略的夜间版。
   */
  NEUTRAL_DARK,
  ;

  companion object {
    /** 默认 = 彩色·浅，也就是"改动前看到的那个界面"。 */
    val Default = COLORFUL_LIGHT

    /**
     * 从 `SharedPreferences` 里存的字符串还原，认不出来返回 `null`（调用处回落到默认值）。
     *
     * ⚠️ 除了本枚举的现名，还要认 **2026-09-30 的三个旧名字**：枚举名就是存储键，
     * 改名不迁就等于"存过的用户一升级发现自己的选择被重置了"。
     * 旧 `DARK` 迁到 [COLORFUL_DARK] 而不是 [NEUTRAL_DARK] —— 当年的「深色」
     * 就是彩色的深色版，迁到别处会让用户看到一张没见过的脸。
     */
    fun fromStorage(name: String?): ThemeMode? =
      entries.firstOrNull { it.name == name }
        ?: when (name) {
          "COLORFUL" -> COLORFUL_LIGHT
          "LIGHT" -> NEUTRAL_LIGHT
          "DARK" -> COLORFUL_DARK
          else -> null
        }
  }
}

/**
 * 主题色。决定全站的"高亮"语义：底部栏选中图标、动作库筛选胶囊选中、
 * 食材库热量数字与「新建」、输入框光标、开关轨道、「我的」页设置图标等。
 *
 * ⚠️ **每个色号同时充当"前景强调"与"选中填充"两种角色**（胶囊选中时它是背景、
 * 热量数字里它是文字），所以取值都压在**中间调**：够深以便白字压得住，
 * 够亮以便压在深色底上还能看清。不要为了"更鲜艳"往两端跑 —— 那样总有一个角色会塌。
 *
 * 也因此**不为深色模式另配一套**：另配就意味着"色板上显示的颜色 ≠ 实际用到的颜色"，
 * 而调色板的第一职责是"所见即所得"。
 */
enum class AccentColor(val color: Color) {
  BLUE(Color(0xFF2C7BE5)),
  MINT(Color(0xFF0E9C86)),
  VIOLET(Color(0xFF7C5CE0)),
  CORAL(Color(0xFFC96A43)),
  ROSE(Color(0xFFCE3F63)),
  ;

  companion object {
    val Default = BLUE
  }
}

/**
 * 一套完整的渲染配色。
 *
 * 字段按用途分组。加字段时**三套调色板都要给值** —— 只给浅色那套填、
 * 深色那套漏了，表现是那块 UI 在深色模式下"消失"（默认值 0 = 全透明）。
 */
@Immutable
data class ZiFitPalette(
  val mode: ThemeMode,
  val isDark: Boolean,
  /** 当前主题色。见 [AccentColor]。 */
  val accent: Color,
  // ---- 文字 ----
  val ink: Color,
  val inkMuted: Color,
  val inkFaint: Color,
  // ---- 品牌薄荷（不随主题色变：它是"ZiFit 的绿"，不是用户的高亮色）----
  val brand: Color,
  val brandContainer: Color,
  val onBrandContainer: Color,
  // ---- 设置页行首图标的固定色相（只做行间扫视定位，不参与主题色）----
  val iconViolet: Color,
  val iconCoral: Color,
  val iconSky: Color,
  // ---- 背景：基底渐变 + 光斑 ----
  val backgroundTop: Color,
  val backgroundMid: Color,
  val backgroundBottom: Color,
  val blobMint: Color,
  val blobSky: Color,
  val blobLilac: Color,
  val blobCoral: Color,
  /** 光斑整体透明度系数。浅色模式靠它"退彩"，不必逐团改色。 */
  val blobAlphaScale: Float,
  // ---- 玻璃面板 ----
  val glassFillStart: Color,
  val glassFillEnd: Color,
  val dockFillStart: Color,
  val dockFillEnd: Color,
  val glassBorderStart: Color,
  val glassBorderEnd: Color,
  val rowFill: Color,
  val rowBorder: Color,
  val shadow: Color,
  /**
   * 悬浮头柔化层的颜色，**毛玻璃模糊生效时**用这一档（较透）。
   *
   * 分工：模糊负责把背后内容化成色块，它只负责把整体亮度抬到能压住文字。
   * 见 [headerScrimSolid]。
   */
  val headerScrim: Color,
  /**
   * 悬浮头柔化层的颜色，**没有模糊兜底时**用这一档（较厚）。
   *
   * ⚠️ 两份不是冗余：关掉毛玻璃（或 API < 31）时背后内容是**清晰**的，白纱必须厚到
   * 不让文字压文字；有模糊时同样厚度就会把模糊盖没，白做。挑值时照
   * 「[headerScrim] = 原值向透明方向减一档」推。
   */
  val headerScrimSolid: Color,
  // ---- Material 色板里我们真正用到的那几个槽位 ----
  val surface: Color,
  val surfaceVariant: Color,
  val outline: Color,
  val outlineVariant: Color,
)

/**
 * 彩色·浅（原浅色那一版）：冷灰蓝基底 + 四色光斑。
 *
 * 光斑配色与 `GlassBackground` 的位置表是**一起调的**：四团小而浓的色斑分散在四角，
 * 加上一大团极淡的冷调氛围底。改色相前先确认"每团色斑都小于半个屏宽"，
 * 否则会退化成整屏渐变、玻璃失去"背后有东西"的观感。
 */
private val ColorfulLightPalette =
  ZiFitPalette(
    mode = ThemeMode.COLORFUL_LIGHT,
    isDark = false,
    accent = AccentColor.Default.color,
    ink = Color(0xFF141D26),
    inkMuted = Color(0xFF56636F),
    inkFaint = Color(0xFF7C8A98),
    brand = Color(0xFF0B6B5D),
    brandContainer = Color(0xFFC2E9DF),
    onBrandContainer = Color(0xFF053B34),
    iconViolet = Color(0xFF7C5CE0),
    iconCoral = Color(0xFFC96A43),
    iconSky = Color(0xFF3F6E9C),
    backgroundTop = Color(0xFFEAF0F9),
    backgroundMid = Color(0xFFE2EAF6),
    backgroundBottom = Color(0xFFDCE5F3),
    blobMint = Color(0xFF4FCBB0),
    blobSky = Color(0xFF5F9DEA),
    blobLilac = Color(0xFF9279E4),
    blobCoral = Color(0xFFF58F6B),
    blobAlphaScale = 1f,
    /*
     * 玻璃面板底色渐变（左上偏亮、右下偏透），模拟玻璃受光，同时让背后色斑微微透出。
     *
     * 底部 dock 专用的白纱（DockFill*）比面板更实：dock 背后是**实时模糊**，
     * 磨砂感已由模糊提供。
     * ⚠️ **调参方向别搞反**：用户说「透明度小一半」= **更不透**（透明度 ↓ = 不透明度 ↑），
     * 不是更透。2026-09-29 首次改反了一次，又在下一轮改回来，最终按下式重算：
     * `α_new = α_old + (1 - α_old) / 2` —— 透明度减半、不透明度向 1 靠拢一半。
     */
    glassFillStart = Color(0xCCFFFFFF),
    glassFillEnd = Color(0x80FFFFFF),
    dockFillStart = Color(0xD6FFFFFF),
    dockFillEnd = Color(0xACFFFFFF),
    glassBorderStart = Color(0xFFFFFFFF),
    glassBorderEnd = Color(0x4DFFFFFF),
    /*
     * 列表行的轻量玻璃底：**不带投影**。
     * 动作库有 601 行，逐行投影会在滚动时反复做离屏绘制，卡顿得不偿失；
     * 行与行之间的层次交给描边和间距即可。
     */
    rowFill = Color(0xA6FFFFFF),
    rowBorder = Color(0x59FFFFFF),
    /** 玻璃投影用色：偏冷的深灰蓝，浅色玻璃下需要比纯黑阴影更明显才立得住。 */
    shadow = Color(0x2E283A55),
    /*
     * 悬浮筛选头背后的柔化层：**半透明**，不是实心。
     * 头下面是滚动的列表，完全透明会让行内容从筛选胶囊的缝隙里原样穿出、字压字看不清；
     * 全实心则又变回一块"遮挡层"。使用时做成"顶部保持、向下淡出"的渐变。
     *
     * ⚠️ 2026-09-30 二改：**降到 60%**（原 75%）。原因是这一层叠在了实时模糊之上 ——
     * 背后内容已被 [com.zifit.app.ui.common.GlassBlurLayer] 化成色块，
     * 不再有"字压字"的风险，此时白纱承担的就只剩"把整体亮度拉到能压住文字"这一件事，
     * 再糊到 75% 就**看不出来背后有东西**，模糊白做了（QQ 音乐那类悬浮栏的观感
     * 恰恰来自"能明显看见背后被糊掉的色块"）。
     * 这一层与模糊的**淡出曲线共用同一组 stop**（见 `HeaderFadeMask`），改的时候两边一起改。
     */
    headerScrim = Color(0x99FFFFFF),
    /** 关掉模糊时用的厚纱：即「加模糊之前」的原值。 */
    headerScrimSolid = Color(0xC0FFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3EAF3),
    outline = Color(0x2416202B),
    outlineVariant = Color(0x14FFFFFF),
  )

/**
 * 浅色：把四团光斑统一成**同一种钢灰**，并把整体透明度压到 0.55 —— 于是背景从
 * "冷灰蓝 + 彩色斑"变成"近白 + 银色明暗"，玻璃依旧是玻璃（还有明暗可磨砂），
 * 但整页没有任何色相。基底同步提亮一档：光斑退彩后若还留着原来的蓝底，会脏。
 */
private val NeutralLightPalette =
  ColorfulLightPalette.copy(
    mode = ThemeMode.NEUTRAL_LIGHT,
    backgroundTop = Color(0xFFF6F8FB),
    backgroundMid = Color(0xFFF0F3F8),
    backgroundBottom = Color(0xFFEAEEF5),
    blobMint = Color(0xFF8E9BAE),
    blobSky = Color(0xFF8E9BAE),
    blobLilac = Color(0xFF8E9BAE),
    blobCoral = Color(0xFF8E9BAE),
    blobAlphaScale = 0.55f,
    // 底色变白后，行底与描边要更淡才不糊：原来那套是按"冷灰蓝底"配的
    rowFill = Color(0xB3FFFFFF),
    rowBorder = Color(0x66C3CCD8),
    // 与彩色·浅同理（见那边的长注释）：柔化层要能让背后的模糊色块透出来
    headerScrim = Color(0xA6FFFFFF),
    headerScrimSolid = Color(0xC7FFFFFF),
    shadow = Color(0x24202A38),
  )

/**
 * 彩色·深（2026-09-30 之前叫「深色」）。
 *
 * 不是把浅色反相：**光斑要比彩色·浅更浓**（深底上同样的 α 几乎看不见），且取更深沉的色号，
 * 让它们像"从暗处透出的光"而不是浮在表面的色块。玻璃填充反过来 —— 不再是白纱，
 * 而是**低透明度的白**（深底上没有"白色"可叠，只能靠一点点提亮来表达玻璃的边界）。
 */
private val ColorfulDarkPalette =
  ZiFitPalette(
    mode = ThemeMode.COLORFUL_DARK,
    isDark = true,
    accent = AccentColor.Default.color,
    ink = Color(0xFFE9EEF5),
    inkMuted = Color(0xFF9CA9B8),
    inkFaint = Color(0xFF6E7B8A),
    brand = Color(0xFF45C6AE),
    brandContainer = Color(0xFF14352F),
    onBrandContainer = Color(0xFFB7E9DE),
    iconViolet = Color(0xFFA48BF2),
    iconCoral = Color(0xFFE08D63),
    iconSky = Color(0xFF6FA8D8),
    backgroundTop = Color(0xFF10151D),
    backgroundMid = Color(0xFF141A24),
    backgroundBottom = Color(0xFF18202B),
    blobMint = Color(0xFF2E9E8A),
    blobSky = Color(0xFF3B7DD8),
    blobLilac = Color(0xFF6B57C8),
    blobCoral = Color(0xFFC46A4A),
    blobAlphaScale = 1.15f,
    glassFillStart = Color(0x24FFFFFF),
    glassFillEnd = Color(0x0FFFFFFF),
    dockFillStart = Color(0x3DFFFFFF),
    dockFillEnd = Color(0x29FFFFFF),
    glassBorderStart = Color(0x42FFFFFF),
    glassBorderEnd = Color(0x0FFFFFFF),
    rowFill = Color(0x1AFFFFFF),
    rowBorder = Color(0x26FFFFFF),
    shadow = Color(0x66000000),
    headerScrim = Color(0xA6131922),
    headerScrimSolid = Color(0xE6131922),
    surface = Color(0xFF1B232E),
    surfaceVariant = Color(0xFF232D3A),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
  )

/**
 * 深色：与浅色对应的中性深色 —— 深底 + **四团钢灰光斑**（同一个色号，无色相）。
 *
 * 三处与 [ColorfulDarkPalette] 的区别，都不是随手调的：
 * 1. **基底去蓝**：彩色·深的底是带蓝调的 0x10151D 系，中性版换成近中性的冷灰。
 *    只把光斑退彩、底留着蓝，"中性"就名不副实 —— 一眼看去还是蓝的。
 * 2. **光斑统一钢灰**：取 0x5E6C7C，比底色亮约四档 —— 深底上的光斑必须靠"比底亮"
 *    才能被看见（浅色那套是靠"比底暗"），所以不能直接抄浅色的 0x8E9BAE：
 *    那个值压在深底上会亮到像贴了一块补丁。
 * 3. **α 系数回到 1.0**：彩色·深用 1.15 是为了让**色相**在暗处透出来；
 *    中性版没有色相可透，再放大只会把灰斑推成灰雾，反而糊掉玻璃的边界。
 */
private val NeutralDarkPalette =
  ColorfulDarkPalette.copy(
    mode = ThemeMode.NEUTRAL_DARK,
    backgroundTop = Color(0xFF121417),
    backgroundMid = Color(0xFF16181C),
    backgroundBottom = Color(0xFF1B1E23),
    blobMint = Color(0xFF5E6C7C),
    blobSky = Color(0xFF5E6C7C),
    blobLilac = Color(0xFF5E6C7C),
    blobCoral = Color(0xFF5E6C7C),
    blobAlphaScale = 1f,
  )

/** 取指定模式 + 主题色的调色板。纯函数，[ZiFitTheme] 里用 `remember` 兜住。 */
fun paletteOf(mode: ThemeMode, accent: AccentColor): ZiFitPalette =
  when (mode) {
      ThemeMode.COLORFUL_LIGHT -> ColorfulLightPalette
      ThemeMode.NEUTRAL_LIGHT -> NeutralLightPalette
      ThemeMode.COLORFUL_DARK -> ColorfulDarkPalette
      ThemeMode.NEUTRAL_DARK -> NeutralDarkPalette
    }
    .copy(accent = accent.color)

/**
 * 当前生效的调色板。
 *
 * 默认值取彩色·浅：预览、单测这类"没套 [ZiFitTheme]"的场景下仍有一套完整配色，
 * 不会因为拿到默认值画出一片透明。
 */
val LocalZiFitPalette = staticCompositionLocalOf { ColorfulLightPalette }

// ---------------------------------------------------------------------------
// 取色入口。名字与原顶层常量一一对应，业务代码只认这些名字，不认"哪一套调色板"。
// ---------------------------------------------------------------------------

/** 主题色（用户选的强调色）。 */
val Accent: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.accent

val Ink: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.ink

val InkMuted: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.inkMuted

val InkFaint: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.inkFaint

/** 品牌薄荷（动作详情页的强调文字）。 */
val Brand: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.brand

val BrandContainer: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.brandContainer

val OnBrandContainer: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.onBrandContainer

val IconViolet: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.iconViolet

val IconCoral: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.iconCoral

val IconSky: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.iconSky

val BackgroundTop: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.backgroundTop

val BackgroundMid: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.backgroundMid

val BackgroundBottom: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.backgroundBottom

val BlobMint: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.blobMint

val BlobSky: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.blobSky

val BlobLilac: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.blobLilac

val BlobCoral: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.blobCoral

/** 光斑整体透明度系数（浅色模式靠它退彩）。 */
val BlobAlpha: Float
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.blobAlphaScale

val GlassFillStart: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.glassFillStart

val GlassFillEnd: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.glassFillEnd

val DockFillStart: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.dockFillStart

val DockFillEnd: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.dockFillEnd

val GlassBorderStart: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.glassBorderStart

val GlassBorderEnd: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.glassBorderEnd

/** 列表行的轻量玻璃底（**不带投影**，见 `Modifier.glassRow`）。 */
val GlassRowFill: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.rowFill

val GlassRowBorder: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.rowBorder

val GlassShadow: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.shadow

/** 悬浮头柔化层色（有模糊时）。 */
val HeaderScrim: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.headerScrim

/** 悬浮头柔化层色（无模糊兜底时，更厚）。选哪一档的判据见 [headerScrimBrush]。 */
val HeaderScrimSolid: Color
  @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.headerScrimSolid
