/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zifit.app.R
import com.zifit.app.theme.Accent
import com.zifit.app.theme.GlassRowBorder
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted

/*
 * 二级页面的共用骨架：页头（返回 + 标题）、可点的行、圆形玻璃图标按钮。
 * 放在 common 而不是某个 feature 包里 —— 我的标签下的四个页面都要用，
 * 动作库与食材库将来加设二级页面时同样能用，业务包之间**不得互相 import**。
 */

/**
 * 二级页面的页头：左侧返回图标 + 标题，右侧可挂一个尾部动作。
 *
 * 标题字号由 `typography.titleLarge`（20sp，见 `theme/Type.kt`）统一决定 ——
 * **全站二级页的页头标题只有这一处**，改字号不必去四个页面里各改一遍。
 *
 * 与 dock 一致**关掉水波纹**（`interactionSource = null, indication = null`）：
 * Material 默认的灰方块与整站的手绘毛玻璃不是一套语言。
 *
 * [titleMaxLines] 默认 1（中文标题都短）；动作详情页那种「动作名本身就是标题」
 * 的页面传 2 —— 英文动作名（如 `Barbell Bench Press - Medium Grip`）一行放不下，
 * 截断成一个省略号等于把标题废掉。
 */
@Composable
fun PageHeader(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  titleMaxLines: Int = 1,
  trailing: @Composable () -> Unit = {},
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconAction(
      iconRes = R.drawable.ic_back,
      contentDescription = stringResource(R.string.action_back),
      onClick = onBack,
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = titleMaxLines,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    trailing()
  }
}

/**
 * 页面上「孤零零一个图标」的动作按钮：返回、设置入口、页头右上角的保存键都用它。
 *
 * **不画底**（2026-09-30 用户要求"取消背景色"）：早期版本给它套了一层半透明白圆 +
 * 描边，本意是提示"这里能点"，实际读起来像贴了块膏药 —— 页头左侧那一片本来就是
 * 渐变玻璃底，再叠一个白圆只会把图标的对比度拉低。参考图（iOS 设置）的返回也是裸箭头。
 *
 * 触摸区仍是 [size]（40dp，≥48 的推荐值以下但已是 Material 图标按钮的常用档），
 * **去掉的只是底色** —— 别因为看不见就把它缩成图标本身那么大。
 */
@Composable
fun IconAction(
  iconRes: Int,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  /** 触摸区边长。默认 40dp。 */
  size: Int = 40,
  /** 图标本身的大小，默认 20dp（比触摸区小 20dp，视觉上才有"留白感"）。 */
  iconSize: Int = 20,
  tint: Color = InkMuted,
) {
  Box(
    modifier =
      modifier
        .size(size.dp)
        .clickable(interactionSource = null, indication = null, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(iconSize.dp),
    )
  }
}

/**
 * 设置页的**分组卡片**：一个 [GlassSurface] 里装若干 [NavRow]，整组共用一张圆角卡。
 *
 * 为什么要分组（2026-09-30 按用户给的参考图定稿）：原来每行各画一张卡，
 * 三行读起来是三个互不相干的胶囊，**分组信息全丢**——「个人资料 + AI 接入」是一类
 * （都要填、都影响 AI 生成），「关于」是另一类（只读的法律与出处信息）。
 * 参考图（iOS 设置）的骨架就是"白底圆角分组 + 组间留白"，照它来。
 *
 * 组内**不画分隔线**：参考图里行与行只靠留白分开，我们跟它一致——靠行高划分比靠 1dp 线条
 * 更干净，也不会在毛玻璃的半透明底上出现"线比底还实"的怪观感。
 *
 * 内容区**零内边距**：行自带的 16dp 横向内边距已经等同卡内留白，
 * 若再叠加 18dp（[GlassSurface] 的默认值）会让行文字比页头标题还靠内，整块缩进去一截。
 */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  GlassSurface(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
    content()
  }
}

/**
 * 设置页里的一行：行首图标 + 标题 +（可选）副标题 + 右侧箭头。
 *
 * ⚠️ **本行不画自己的底**，必须放进 [SettingsGroup] 里用——单拎出来会看不见边界。
 * （这是与「每行一张玻璃卡」那版最大的区别：参考图的卡是**组**，不是行。）
 *
 * 副标题用来**当场显示当前值**（「个人资料 · 未填写」），省得点进去才知道填没填。
 * [iconRes] 不传则整行只有文字；**同一组内要么都带图标、要么都不带**——
 * 混着放会让文字左边界参差不齐。
 */
@Composable
fun NavRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  /** 行首图标（彩色着色，见调用处）。参考图的行首图标是**裸字形**，不套彩色方底。 */
  @DrawableRes iconRes: Int? = null,
  iconTint: Color = InkMuted,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(interactionSource = null, indication = null, onClick = onClick)
        // 竖向内缩 12dp：单行行高 = 24 + 24 = 48dp，与参考图的行高同一档，
        // 同时踩住 48dp 的最小触摸目标，不再靠外框凑数
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (iconRes != null) {
      Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(22.dp),
      )
      Spacer(Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Icon(
      painter = painterResource(R.drawable.ic_chevron_right),
      contentDescription = null,
      tint = InkFaint,
      modifier = Modifier.size(20.dp),
    )
  }
}

/**
 * 开关行：与 [NavRow] 同一套内边距与图标槽位，右侧把箭头换成一个 [Switch]。
 *
 * **整行可点**，开关自己不再单独接手势（[Switch] 的 `onCheckedChange` 传 null）——
 * 两处都能点会让"点空白算不算点开关"变成一个没人能预判的问题。
 *
 * 只用于**立刻生效**的开/关项；需要确认或要跳页的动作请用 [NavRow] ——
 * 混用会让"点哪儿立刻变、点哪儿还要再确认"变得不可预测。
 */
@Composable
fun SwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  enabled: Boolean = true,
  @DrawableRes iconRes: Int? = null,
  iconTint: Color = InkMuted,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(enabled = enabled, interactionSource = null, indication = null) {
          onCheckedChange(!checked)
        }
        // 竖向 8dp：开关自身高 32dp，8 + 32 + 8 = 48dp，与 NavRow 的单行行高踩齐
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (iconRes != null) {
      Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(22.dp),
      )
      Spacer(Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          modifier = Modifier.padding(top = 2.dp),
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled,
      colors =
        SwitchDefaults.colors(
          // 关掉选中态的对勾与小圆：在玻璃底上多两个白色细节，反而让"开/关"读不干脆
          checkedThumbColor = Color.White,
          checkedTrackColor = Accent,
          checkedBorderColor = Color.Transparent,
          checkedIconColor = Color.Transparent,
          uncheckedThumbColor = Color.White,
          uncheckedTrackColor = Color(0x1A56636F),
          uncheckedBorderColor = InkFaint,
          uncheckedIconColor = Color.Transparent,
        ),
    )
  }
}

/**
 * 取色点：一个颜色 + 它的名字。
 *
 * 名字不是装饰：色号本身对读屏软件毫无意义，[SwatchRow] 靠它把
 * "选中的是哪个颜色"读出来（无障碍语义里挂的就是这个名字）。
 */
@Immutable
data class Swatch(val color: Color, val label: String)

/**
 * 单选行：标题 +（可选）副标题 + 若干**选项胶囊**，整行不可点、只有胶囊可点。
 *
 * 用于"选项少且都值得一眼看完"的偏好（主题模式：彩色·浅 / 浅色 / 彩色·深 / 深色）——
 * 这种场合做成 [NavRow] 进二级页或弹选择器都是多一步：几个选项一共才十来个字，
 * 摆在原地比藏在后面更好。
 *
 * 胶囊容器用 [FlowRow] 而**不是** `Row`：选项数量与文字宽度都会长（主题模式从三档
 * 加到四档、其中两个还是「彩色·浅」这样的四字标签），写死一行迟早溢出 ——
 * 溢出的表现是最后一个胶囊被卡片裁掉，而不是自动换行。给 FlowRow 一个宽度上限，
 * 它自己会把放不下的挪到下一行。横纵间距都取 8dp：换行后"行间距"与"胶囊间距"一致。
 *
 * [maxPerRow] 用来**主动**指定每行几个，而不是听天由命地等它自己换行：
 * 按宽度自动换行的断点会随字体、字号、机型漂移（同一组选项在窄屏是 2+2、
 * 在宽屏是 4+0），而这类选项通常自带分组语义 —— 主题模式的四档就是"浅组一行、
 * 深组一行"，硬压在 4 列一行反而看不出分组。传 `null`（默认）才是"能放几个放几个"。
 *
 * 胶囊复用 [FilterPill]（与动作库的分类筛选同一个组件），所以**选中态自动跟随主题色**，
 * 不必在这里再定一套"选中长什么样"。
 *
 * 图标**顶部对齐**（不是 [NavRow] 那种垂直居中）：本行是"标题在上、控件在下"的两段式，
 * 图标居中会飘在两段之间，反而看不出它属于哪一行。
 */
@Composable
fun ChoiceRow(
  title: String,
  options: List<String>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  /** 每行最多几个胶囊；`null` = 按宽度自动换行。见上方 KDoc。 */
  maxPerRow: Int? = null,
  @DrawableRes iconRes: Int? = null,
  iconTint: Color = InkMuted,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    if (iconRes != null) {
      Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(22.dp),
      )
      Spacer(Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      RowTitle(title = title, subtitle = subtitle)
      Spacer(Modifier.height(10.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = maxPerRow ?: Int.MAX_VALUE,
      ) {
        options.forEachIndexed { index, label ->
          FilterPill(
            label = label,
            selected = index == selectedIndex,
            onClick = { onSelect(index) },
          )
        }
      }
    }
  }
}

/**
 * 取色行：标题 +（可选）副标题 + 一行取色点。
 *
 * 与 [ChoiceRow] 的区别是**选项用颜色本身说话**：主题色不需要文字标签就能认出来，
 * 摆一排色点在原地点选，比"点进去看列表"快得多。
 *
 * 选中态是**外圈一道环**而不是"打个勾"：环把颜色本身完整留出来，用户看到的是
 * "我选的这个颜色"，而不是"一个被勾盖住的颜色"。环色取 `onSurface`（当前主题的文字色），
 * 于是浅色主题下是深环、深色主题下是浅环，两种底上都立得住。
 */
@Composable
fun SwatchRow(
  title: String,
  swatches: List<Swatch>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  @DrawableRes iconRes: Int? = null,
  iconTint: Color = InkMuted,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    if (iconRes != null) {
      Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(22.dp),
      )
      Spacer(Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      RowTitle(title = title, subtitle = subtitle)
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEachIndexed { index, swatch ->
          val selected = index == selectedIndex
          Box(
            modifier =
              Modifier.size(32.dp)
                .clip(CircleShape)
                .border(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.onSurface else GlassRowBorder,
                  shape = CircleShape,
                )
                .selectable(
                  selected = selected,
                  interactionSource = null,
                  indication = null,
                  role = Role.RadioButton,
                  onClick = { onSelect(index) },
                )
                .semantics { contentDescription = swatch.label },
            contentAlignment = Alignment.Center,
          ) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(swatch.color))
          }
        }
      }
    }
  }
}

/** [ChoiceRow] / [SwatchRow] 共用的标题块（与 [NavRow] 的标题/subtitle 同一套字号）。 */
@Composable
private fun RowTitle(title: String, subtitle: String?) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
  )
  if (!subtitle.isNullOrBlank()) {
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = InkMuted,
      modifier = Modifier.padding(top = 2.dp),
    )
  }
}

/**
 * 页面级的提示文字（错误、空态说明）。多处复用，跟 [FilterPill] 一样属于整站语言的一部分。
 */
@Composable
fun HintText(text: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = if (emphasized) Accent else InkMuted,
    modifier = modifier,
  )
}
