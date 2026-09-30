/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.data.food.Food
import com.zifit.app.data.food.FoodSuggestion
import com.zifit.app.data.food.formatNutrient
import com.zifit.app.theme.Accent
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.common.FilterPill
import com.zifit.app.ui.common.GlassBackdropSource
import com.zifit.app.ui.common.GlassBlurLayer
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.HeaderFadeMask
import com.zifit.app.ui.common.LocalBackdrop
import com.zifit.app.ui.common.SectionTitle
import com.zifit.app.ui.common.dockContentInset
import com.zifit.app.ui.common.glassRow
import com.zifit.app.ui.common.headerScrimBrush
import com.zifit.app.ui.common.rememberPageBackdrop

/** 悬浮头与列表之间的呼吸间距。 */
private val HeaderGap = 12.dp

/**
 * 食材库：本地食材列表 + 搜索 + 手动录入。
 *
 * 版式与动作库一致 —— **无页面标题**、搜索与操作**悬浮**在列表之上，
 * 列表从下方穿过（透过就对了，遮死就成了「遮挡层」，见 AGENTS.md §5.3）。
 * 无标题也是**硬性要求**：dock 的无障碍描述里已有「食材库」，页面上再出现一次，
 * `BottomNavigationTest` 的「标签文案不得渲染为正文」会命中两个节点。
 *
 * 数据分两路，界面上也分成两段：
 *
 * - **我的食材**：Room 里的本地数据，可增删改（点一行即编辑）。
 * - **营养库**：外部查询结果，查到后「加入」落库。
 *   本轮该查询**尚未接入**（[com.zifit.app.data.food.UnavailableFoodSearchSource]），
 *   界面如实说明，而不是假装搜过、显示一个空的「没有匹配」。
 */
@Composable
fun FoodLibraryScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: FoodLibraryViewModel = viewModel(factory = FoodLibraryViewModel.factory(context))
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  val density = LocalDensity.current
  val bottomInset = dockContentInset()

  // 未测量前为 0：首帧列表从顶部开始，量到高度后立即归位（同一帧完成，不会闪）
  var headerHeight by remember { mutableStateOf(0.dp) }
  // null = 没有弹窗
  var editing by remember { mutableStateOf<FoodEditorTarget?>(null) }

  // 页内录制器：悬浮头的实时模糊要采样它，而不是根上那个（原因见 rememberPageBackdrop）
  val pageBackdrop = rememberPageBackdrop()

  CompositionLocalProvider(LocalBackdrop provides pageBackdrop) {
    Box(modifier = modifier.fillMaxSize()) {
      /*
       * 会被悬浮头压住的内容 —— 只有这块进录像。悬浮头必须留在它外面（兄弟而非子节点）：
       * 录进去就会"自己糊自己"，并且会和录制层成环把进程打挂。
       */
      GlassBackdropSource(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
          // 本页不用 screenFrame（与动作库同理：柔化层要铺满整屏），
          // 安全区与左右边距自己兜：左右安全区加在容器上，16dp 呼吸边距加在 contentPadding 上。
          modifier =
            Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          // 顶部让开悬浮头、底部让开悬浮 dock：两端留给内容「经过」，而不是把可滚动区截短
          contentPadding =
            PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = headerHeight,
              bottom = bottomInset + 8.dp,
            ),
        ) {
          if (state.isFiltered) {
            item(key = "remote-title") {
              SectionTitle(
                text = stringResource(R.string.food_section_remote),
                modifier = Modifier.padding(bottom = 2.dp),
              )
            }
            when (val search = state.search) {
              // Idle 只在防抖窗口内短暂出现（查询还没发出去），按「正在查」呈现即可
              FoodSearchUiState.Idle,
              FoodSearchUiState.Searching ->
                item(key = "remote-searching") { Hint(stringResource(R.string.food_searching)) }

              FoodSearchUiState.Unavailable ->
                item(key = "remote-unavailable") {
                  Hint(stringResource(R.string.food_search_unavailable))
                }

              is FoodSearchUiState.Failed ->
                item(key = "remote-failed") {
                  Hint(stringResource(R.string.food_search_failed, search.reason))
                }

              is FoodSearchUiState.Hit ->
                if (search.suggestions.isEmpty()) {
                  item(key = "remote-empty") {
                    Hint(stringResource(R.string.food_search_no_hit, state.query))
                  }
                } else {
                  itemsIndexed(
                    search.suggestions,
                    key = { i, s -> "sug-$i-${s.name}" },
                  ) { _, suggestion ->
                    SuggestionRow(
                      suggestion = suggestion,
                      onAdd = { viewModel.addSuggestion(suggestion) },
                    )
                  }
                }
            }

            item(key = "local-title") {
              SectionTitle(
                text = stringResource(R.string.food_section_library),
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
              )
            }
          }

          when {
            state.loading -> item(key = "loading") { Hint(stringResource(R.string.food_loading)) }

            state.foods.isEmpty() ->
              item(key = "empty") {
                // 空态分两句：库里本来就空 vs 有关键词但没匹配上。
                // 复用一句「没有匹配」会让新用户在空库上看到莫名其妙的提示。
                Hint(
                  if (state.isFiltered) {
                    stringResource(R.string.food_empty_filtered, state.query)
                  } else {
                    stringResource(R.string.food_empty)
                  }
                )
              }

            else ->
              items(items = state.foods, key = { it.id }) { food ->
                FoodRow(food = food, onClick = { editing = FoodEditorTarget.Existing(food) })
              }
          }
        }
      }

      FoodHeader(
        query = state.query,
        onQueryChange = viewModel::onQueryChange,
        onNew = { editing = FoodEditorTarget.New },
        onMeasured = { headerHeight = with(density) { it.height.toDp() } },
        modifier = Modifier.align(Alignment.TopCenter),
      )
    }
  }

  val target = editing
  if (target != null) {
    FoodEditorDialog(
      target = target,
      onDismiss = { editing = null },
      onSave = {
        viewModel.save(it)
        editing = null
      },
      onDelete = {
        viewModel.delete(it)
        editing = null
      },
    )
  }
}

/**
 * 悬浮头：搜索框 + 「新建」。
 *
 * 与动作库的筛选头同款：**实时模糊**（[GlassBlurLayer]）+ 半透明柔化层，
 * 列表从下面滚过时被真正虚化，而不是只被压淡。
 *
 * 柔化层必须**铺满屏幕宽度并顶到最上沿**：它画在最外层，
 * 之后的 `windowInsetsPadding` / `padding` 只把**内容**推进来，不会缩窄这层底。
 * 反过来先套外层边距，左右各差 16dp、顶部差一个状态栏，屏幕边缘就露出可见直边。
 */
@Composable
private fun FoodHeader(
  query: String,
  onQueryChange: (String) -> Unit,
  onNew: () -> Unit,
  onMeasured: (IntSize) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth()) {
    // 层级：模糊（内含白纱）→ 内容。模糊层是不透明的屏幕副本，必须在最底且自身无子内容。
    // 白纱并进同一层而不是另开 `Box`：它与模糊必须**要么一起在背景里、要么一起不在**，
    // 拆成两个节点就会有"其中一个被录进背景"的错配（见 `isBackdropRecording`）。
    GlassBlurLayer(
      modifier = Modifier.matchParentSize(),
      fadeMask = HeaderFadeMask,
      overlay = headerScrimBrush(),
    )

    Column(
      modifier =
        Modifier.fillMaxWidth()
          // 头部区域**吞掉点击**：它下面是滚动的列表，不吞的话点在头部空白处会命中
          // 下方列表行、误开编辑弹窗。只拦点击不拦拖拽，从头部按下再纵拖仍会滚列表。
          //
          // ⚠️ 必须排在 padding **之前**：指针命中区就是该位置节点的尺寸，
          // 排在内边距之后只能盖住内容区、盖不住内边距。
          .pointerInput(Unit) { detectTapGestures(onTap = {}) }
          // 量的是**含下述全部内边距**的高度：`onSizeChanged` 报的是其右侧修饰符撑出的尺寸，
          // 所以必须在这些 padding 之前，否则少算一个状态栏、首行被压在搜索框下。
          .onSizeChanged(onMeasured)
          .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
          )
          .padding(top = 8.dp, bottom = HeaderGap),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SearchField(
          value = query,
          onValueChange = onQueryChange,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        NewButton(onClick = onNew)
      }
    }
  }
}

@Composable
private fun NewButton(onClick: () -> Unit) {
  // 与搜索框同为 GlassSurface，纵向内边距对齐（14dp）→ 两者等高
  GlassSurface(
    modifier = Modifier.clickable(interactionSource = null, indication = null, onClick = onClick),
    shape = MaterialTheme.shapes.large,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    elevated = false,
  ) {
    Text(
      text = stringResource(R.string.food_action_new),
      style = MaterialTheme.typography.bodyMedium,
      color = Accent,
    )
  }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current

  // elevated = false：这块面板下面会有列表滚过，投影会在内容上压出一条暗带
  GlassSurface(
    modifier =
      modifier
        // **整块面板**都可点：点在图标、内边距这类空白处也把焦点交给输入框并拉起键盘。
        // 输入框在命中链更内层，点它仍由它自己处理，这里只兜住它没接住的区域。
        .clickable(interactionSource = null, indication = null) {
          focusRequester.requestFocus()
          // 仅 requestFocus 在部分机型上不弹输入法，显式 show 一次
          keyboard?.show()
        },
    contentPadding = PaddingValues(14.dp),
    elevated = false,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        painter = painterResource(R.drawable.ic_search),
        contentDescription = null,
        tint = InkFaint,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(10.dp))
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle =
          MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(Accent),
        modifier = Modifier.weight(1f).focusRequester(focusRequester),
        // 占位提示画在 `decorationBox` 里并用 `matchParentSize` —— **不参与测量**，
        // 框高因此恒等于输入区那一行（即「以输入文字后的高度为准」）。
        //
        // ⚠️ 别退回「提示 Text 与输入框上下堆叠」：那时空态两行、输入态一行，
        // 框会随是否输入而变高变矮。见 tasks/lessons.md 第 31 条。
        decorationBox = { innerTextField ->
          Box {
            if (value.isEmpty()) {
              Text(
                text = stringResource(R.string.food_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.matchParentSize(),
              )
            }
            innerTextField()
          }
        },
      )
    }
  }
}

/** 营养库查到的候选：左侧名称 + 营养摘要，右侧「加入」。 */
@Composable
private fun SuggestionRow(suggestion: FoodSuggestion, onAdd: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .glassRow()
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = suggestion.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text =
          nutrientLine(
            suggestion.proteinG,
            suggestion.fatG,
            suggestion.carbohydrateG,
          ),
        style = MaterialTheme.typography.labelSmall,
        color = InkMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
    Spacer(Modifier.width(10.dp))
    FilterPill(label = stringResource(R.string.food_add), selected = true, onClick = onAdd)
  }
}

/** 我的食材：点整行进入编辑。 */
@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .glassRow()
        .clickable(interactionSource = null, indication = null, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = food.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = nutrientLine(food.proteinG, food.fatG, food.carbohydrateG),
        style = MaterialTheme.typography.labelSmall,
        color = InkMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
    Spacer(Modifier.width(10.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = formatNutrient(food.energyKcal, decimals = 0),
        style = MaterialTheme.typography.titleSmall,
        color = Accent,
      )
      Text(
        text = stringResource(R.string.food_energy_unit),
        style = MaterialTheme.typography.labelSmall,
        color = InkFaint,
      )
    }
  }
}

/**
 * 一行的营养摘要：只列蛋白质 / 脂肪 / 碳水三项，能量单独在右侧以主色显示，不重复。
 *
 * ⚠️ 缺测值显示 `—`（见 `data/food/FoodFormat.kt` 的 `MISSING_VALUE`）而不是 0 ——
 * `脂肪 0` 与 `脂肪未测` 是两件事，把后者写成前者会让「这份食材不含脂肪」凭空成立。
 * 所以这里**不做 `?: "0"`**。
 */
@Composable
private fun nutrientLine(proteinG: Double?, fatG: Double?, carbohydrateG: Double?): String =
  listOf(
      stringResource(R.string.food_row_protein, formatNutrient(proteinG)),
      stringResource(R.string.food_row_fat, formatNutrient(fatG)),
      stringResource(R.string.food_row_carbohydrate, formatNutrient(carbohydrateG)),
    )
    .joinToString(" · ")

/** 提示条：玻璃面板，与列表同一视觉语言。 */
@Composable
private fun Hint(text: String) {
  GlassSurface(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
