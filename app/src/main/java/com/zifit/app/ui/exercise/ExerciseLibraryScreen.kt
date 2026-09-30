/*
 * Copyright (C) 2026 bamboo-jzy <bamboomail_j@163.com>
 *
 * This file is part of ZiFit.
 * ZiFit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License v3.0 or later, with ABSOLUTELY NO
 * WARRANTY. See <https://www.gnu.org/licenses/gpl-3.0.txt> for full terms.
 */
package com.zifit.app.ui.exercise

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zifit.app.R
import com.zifit.app.data.repdb.Exercise
import com.zifit.app.theme.Accent
import com.zifit.app.theme.InkFaint
import com.zifit.app.theme.InkMuted
import com.zifit.app.ui.common.FilterPill
import com.zifit.app.ui.common.GlassBackdropSource
import com.zifit.app.ui.common.GlassBlurLayer
import com.zifit.app.ui.common.GlassSurface
import com.zifit.app.ui.common.HeaderFadeMask
import com.zifit.app.ui.common.LocalBackdrop
import com.zifit.app.ui.common.dockContentInset
import com.zifit.app.ui.common.glassRow
import com.zifit.app.ui.common.headerScrimBrush
import com.zifit.app.ui.common.rememberPageBackdrop

/** 悬浮筛选头与列表之间的呼吸间距。 */
private val HeaderGap = 12.dp

/**
 * 动作库：可搜索、可按部位筛选的动作列表，点击进入详情。
 *
 * 数据来自随包的 RepDB 免费层数据集（601 条），启动时一次性解析后内存缓存。
 *
 * 版式刻意做成**无标题页**：屏幕空间全留给动作本身，搜索框与部位筛选
 * **悬浮**在列表之上而非把列表截停在自己下方 —— 列表滚动时从它们下面经过，
 * 被**实时虚化成色块**（见 [GlassBlurLayer]），整页因此没有"内容到某处就被挡住"的断层。
 *
 * 悬浮头的做法是 [Box] 叠层 + 实测高度回填为列表的 `contentPadding.top`：
 * 头部高度随字体缩放与胶囊换行变化，**写死数值必然错位**，只能量。
 */
@Composable
fun ExerciseLibraryScreen(onExerciseClick: (String) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: ExerciseLibraryViewModel =
    viewModel(factory = ExerciseLibraryViewModel.factory(context))
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  val density = LocalDensity.current
  val bottomInset = dockContentInset()

  // 未测量前为 0：首帧列表会从顶部开始，一旦量到高度立即归位（同一帧内完成，不会闪）
  var headerHeight by remember { mutableStateOf(0.dp) }
  // 头部自身的 padding 已含 [HeaderGap]，这里不再重复加
  val listTopInset = headerHeight

  // 页内录制器：悬浮头的实时模糊要采样它，而不是根上那个（原因见 rememberPageBackdrop）
  val pageBackdrop = rememberPageBackdrop()

  CompositionLocalProvider(LocalBackdrop provides pageBackdrop) {
    Box(modifier = modifier.fillMaxSize()) {
      /*
       * 会被悬浮头压住的内容 —— 只有这块进录像。悬浮头必须留在它外面（兄弟而非子节点）：
       * 录进去就会"自己糊自己"，并且会和录制层成环把进程打挂。
       */
      GlassBackdropSource(modifier = Modifier.fillMaxSize()) {
        when {
          state.dataMissing -> Notice(stringResource(R.string.exercise_data_missing), listTopInset)
          state.loading -> Notice(stringResource(R.string.exercise_loading), listTopInset)
          state.exercises.isEmpty() -> Notice(stringResource(R.string.exercise_empty), listTopInset)
          else ->
            LazyColumn(
              // 本页不用 [screenFrame]，安全区与左右边距只能自己兜：
              // 左右安全区（挖孔 / 圆角）加在容器上，16dp 呼吸边距加在 contentPadding 上，
              // 二者叠加才与其余页面视觉一致。
              modifier =
                Modifier.fillMaxSize()
                  .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              // 顶部让开悬浮筛选头、底部让开悬浮 dock：两端都留给内容"经过"，
              // 而不是把可滚动区截短（截短就滚不进去了）
              contentPadding =
                PaddingValues(
                  start = 16.dp,
                  end = 16.dp,
                  top = listTopInset,
                  bottom = bottomInset + 8.dp,
                ),
            ) {
              items(items = state.exercises, key = { it.id }) { exercise ->
                ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
              }
            }
        }
      }

      // 悬浮筛选头：**实时模糊** + 半透明柔化层 + 搜索框 + 部位胶囊。
      //
      // 列表从下方经过时会被真正虚化（与底部 dock 同一套机制，见 [GlassBlurLayer]）——
      // 这是"毛玻璃"的物理含义；只叠半透明白纱的话，经过的行仍然清晰可辨，只是被压淡了，
      // 观感上像"蒙了层磨砂纸"，而 QQ 音乐那类悬浮栏要的是背后内容**化成色块**。
      // 柔化层在底部淡出，因此头部与列表之间没有硬边。
      //
      // ⚠️ 柔化层必须**铺满屏幕宽度并顶到屏幕最上沿**：
      // 它以背景层画在最外层，之后的 windowInsetsPadding / padding 只会把**内容**推进来，
      // 不会缩窄这层底。若反过来先套外层边距，柔化层左右各差 16dp、顶部差一个状态栏，
      // 屏幕边缘就会露出可见直边 —— 就是"贴上去一块"的突兀感来源。
      Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
        // 层级固定为：模糊（内含白纱）→ 内容。模糊层是不透明的录制副本，
        // 排在它下面的东西全被盖住，所以它必须在最底、且自身不能有子内容。
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
              // 头部区域**吞掉点击**：它下面是滚动的列表，不吞的话点在头部空白处
              // （图标旁的留白、胶囊两端的内缩、行间空隙）会直接命中下方列表行、误进详情页。
              // 只拦点击，不拦拖拽 —— 从头部按下再纵向拖动仍会落到列表上滚动，这是想要的手感。
              //
              // ⚠️ 必须排在 padding **之前**：修饰符链上指针命中区就是该位置节点的尺寸，
              // 排在内边距之后只能盖住内容区、盖不住内边距（与下面 onSizeChanged 同一个道理）。
              .pointerInput(Unit) { detectTapGestures(onTap = {}) }
              // 量的是**含下述全部内边距**的高度：`onSizeChanged` 报的是它内侧（其右侧所有修饰符）
              // 撑出来的尺寸，所以必须放在这些 padding **之前**，否则量到的是不含状态栏的净内容高，
              // 列表会少让位一个状态栏、首行被压在搜索框下。
              .onSizeChanged { headerHeight = with(density) { it.height.toDp() } }
              .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
              )
              .padding(top = 8.dp, bottom = HeaderGap),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            SearchField(value = state.query, onValueChange = viewModel::onQueryChange)
            if (state.categories.isNotEmpty()) {
              CategoryFilters(
                options = state.categories,
                selected = state.selectedCategory,
                onSelect = viewModel::onCategorySelect,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
  val focusRequester = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current

  // elevated = false：这块面板下面会有列表滚过，投影会在内容上压出一条暗带
  GlassSurface(
    modifier =
      Modifier.fillMaxWidth()
        // **整块面板**都可点：点在图标、内边距这类空白处，也把焦点交给输入框并拉起键盘。
        // `clickable` 加在最外层（含内边距），输入框在命中链更内层，点它仍由它自己处理，
        // 这里只兜住它没接住的区域 —— 在此之前只有输入框那一条窄缝能点中。
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
        // 占位提示画在 `decorationBox` 里，且用 `matchParentSize` 让它**不参与测量**：
        // 高度于是永远等于输入区本身那一行 —— 即「以输入文字后的高度为准」。
        //
        // ⚠️ 别退回「提示 Text 与输入框上下堆叠」的写法：那时空态是**两行**、输入态一行，
        // 搜索框会随是否输入而变高变矮（提示与输入框的字体上下留白还不一样，差一点也会跳）。
        decorationBox = { innerTextField ->
          Box {
            if (value.isEmpty()) {
              Text(
                text = stringResource(R.string.exercise_search_hint),
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

@Composable
private fun CategoryFilters(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    item {
      FilterPill(
        label = stringResource(R.string.exercise_filter_all),
        selected = selected == null,
        onClick = { onSelect(null) },
      )
    }
    items(items = options, key = { it }) { option ->
      FilterPill(
        label = option,
        selected = option == selected,
        onClick = { onSelect(option) },
      )
    }
  }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .glassRow()
        // 与 dock 一致：关掉水波纹，按压反馈交给滚动与页面切换本身
        .clickable(interactionSource = null, indication = null, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ExerciseImage(
      relativePath = exercise.primaryImage,
      contentDescription = null,
      modifier =
        Modifier.size(56.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color.White.copy(alpha = 0.45f)),
    )
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = exercise.name,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (exercise.name != exercise.nameEn) {
        Text(
          text = exercise.nameEn,
          style = MaterialTheme.typography.bodySmall,
          color = InkFaint,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(2.dp))
      Text(
        text =
          listOfNotNull(exercise.bodyPart, exercise.equipment, exercise.difficulty)
            .joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = InkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** 空态 / 加载态提示：同样要让开悬浮筛选头，否则会被压在搜索框下面。 */
@Composable
private fun Notice(text: String, topInset: Dp) {
  Column(modifier = Modifier.fillMaxSize().padding(top = topInset, start = 16.dp, end = 16.dp)) {
    Hint(text)
  }
}

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
