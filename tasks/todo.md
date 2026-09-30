# 待办：底部导航栏（饮食 / 训练 / 库）

## 目标

App 底部提供三个一级导航项 —— **饮食**、**训练**、**库** —— 点击即切换页面。

## 计划

1. `NavigationKeys.kt`：定义三个一级路由 key（`Diet` / `Training` / `Library`），移除模板路由 `Main`。
2. 新增 `TopLevelDestination.kt`：以 enum 声明三个导航项的 key、标签字符串、图标，顺序即展示顺序。
3. `Navigation.kt`：改用 `Scaffold` + `NavigationBar` 承载底部栏，`NavDisplay` 按 key 分发页面。
4. 新增三个页面：`ui/diet/DietScreen.kt`、`ui/training/TrainingScreen.kt`、`ui/library/LibraryScreen.kt`。
5. `strings.xml`：新增标签与页面文案（不硬编码进 Composable）。
6. 新增 3 个底部导航图标 drawable（矢量 XML，不引入 `material-icons-extended` 依赖）。
7. 删除模板遗留：`ui/main/`（`MainScreen` / `MainScreenViewModel` / `MainScreenUiState`）及其两个测试。
8. 测试：`TopLevelDestinationTest`（JVM，锁定"三个导航项 + 顺序"）、`BottomNavigationTest`（真机，点击切换）。
9. 同步文档：`AGENTS.md` §4.1 目录结构、README 功能规划无需改；`tasks/lessons.md` 录入种子教训。

## 设计取舍

- **图标**：手写矢量 XML（restaurant / fitness_center / library_books），避免为 3 个图标引入
  `material-icons-extended`（依赖体积大，且项目约定"不新增依赖，除非确有必要"）。
- **返回栈**：当前每个标签页只有根页面，切换标签即整体替换返回栈；
  待标签页内出现二级页面时，改为**每个标签按标签各自维护返回栈**（已在代码注释中标注）。
- **状态持有**：三个页面暂无数据，故不建 ViewModel；引入数据时按 AGENTS.md §4.2 补齐。

## 验收标准（AGENTS.md §6.4）

- [ ] `./gradlew lint test assembleDebug` 全绿
- [ ] `adb install -r` 成功，App 能启动且不崩溃
- [ ] `adb logcat` 无 `FATAL EXCEPTION`
- [ ] 真机截屏取证：底部三个导航项可见，切换正常（UI 改动）

## 进度

- [x] 1–8 项代码与测试落地
- [x] 构建 / 安装 / 截屏验证

## Review（2026-09-29）

| 验证项 | 结果 |
|---|---|
| `./gradlew lint test assembleDebug` | ✅ BUILD SUCCESSFUL（lint 6 warnings，均为模板遗留，无 error） |
| JVM 单元测试 | ✅ `TopLevelDestinationTest` 4 项通过 |
| `connectedDebugAndroidTest`（真机 PGT-AN00） | ✅ **4/4 passed, 0 failed** |
| `adb install -r` | ✅ Success；`pm list packages` 可见 `com.zifit.app` |
| 启动与日志 | ✅ `topResumedActivity=com.zifit.app/.MainActivity`，logcat 无 `FATAL EXCEPTION` |
| 截屏取证 | ✅ `D:\tool\_dl\tabs\{1_diet,2_training,3_library,4_diet_again}.png`，切换正常（4 号与 1 号一致） |

**踩坑与修正**：
- 初次手写的 `fitness_center` 路径是残缺版，24dp 下渲染成「斜向箭头」而非哑铃。
  改用官方 `fitness_center` 原文路径后恢复正常 —— 校验方式见下条。
- 图标路径无法凭记忆保证正确，已固化离线校验工具：
  `D:\tool\_dl\icon_check.py`（纯标准库光栅化器，3 路径并排渲染成 PNG 供比对）
  与 `D:\tool\_dl\png_crop.py`（截图局部放大，用于判读小尺寸图标真机效果）。
- `NavKey` 与 enum 常量同名会解析到枚举项自身，故枚举常量用大写（`DIET`）。
- 页面标题若与底部标签同文案，`onNodeWithText` 会匹配到多个节点；
  故标签用短词（饮食/训练/库），页面标题用更具体的中文（今日饮食/今日训练/数据管理）。

**遗留**：切标签当前是整体替换返回栈；标签页内出现二级页面时，需改为**每个标签各自维护返回栈**。

---

# 待办：整体视觉改为毛玻璃（2026-09-29）

## 目标

App 整体视觉风格改为**浅色毛玻璃**：半透明玻璃面板 + 柔和彩色光斑背景 + 底部悬浮玻璃导航条。

## 设计决策（用户已确认）

| 决策点 | 选定 |
|---|---|
| 底色取向 | **浅色毛玻璃**（不做深色，不跟随系统深色） |
| 底部导航形态 | **悬浮玻璃条**（圆角胶囊、四周留边） |
| 动态取色 | **关闭** —— 系统壁纸色会破坏玻璃基调，改用固定色板 |
| 是否用 `Modifier.blur` | **不用** —— 需 API 31+；背景光斑改用径向渐变收边，全版本一致且无额外开销 |

## 计划

1. `theme/Color.kt`：重写为玻璃色板（基底渐变 / 光斑 / 玻璃填充与描边 / 投影 / 文字）。
2. `theme/Theme.kt`：固定浅色 `lightColorScheme` + 更圆的 `Shapes`（卡片 24dp）。
3. `theme/Type.kt`：覆盖实际用到的层级（标题半粗、正文收字距）。
4. 新增 `ui/common/GlassBackground.kt`：渐变基底 + 六团径向渐变光斑（玻璃"磨砂"的唯一来源）。
5. 新增 `ui/common/GlassSurface.kt`：玻璃面板（半透明渐变填充 + 高光描边 + 淡投影）。
6. `Navigation.kt`：`Scaffold` 改为 `Box` 叠层（背景 → 内容 → 悬浮玻璃底栏），内容避让底栏与系统导航区。
7. `MainActivity.kt`：`enableEdgeToEdge` 固定浅色系统栏图标。
8. 三个页面：标题压在背景上、内容装入玻璃卡片。
9. 真机截屏取证并迭代透明度参数。

## 验收标准（AGENTS.md §6.4）

- [ ] `./gradlew lint test assembleDebug` 全绿
- [ ] 真机 `connectedAndroidTest` 4/4 通过
- [ ] 安装启动无崩溃，logcat 无 `FATAL EXCEPTION`
- [ ] 截屏取证：三页均为毛玻璃观感，玻璃卡片与悬浮底栏可见

## Review（2026-09-29）

| 验证项 | 结果 |
|---|---|
| `./gradlew lint test assembleDebug` | ✅ BUILD SUCCESSFUL |
| 真机 `connectedDebugAndroidTest` | ✅ **4/4 passed, 0 failed** |
| 安装 / 启动 | ✅ Success，`topResumedActivity=com.zifit.app/.MainActivity`，logcat 无 `FATAL EXCEPTION` |
| 截屏取证 | ✅ `D:\tool\_dl\glass_tabs\{1_diet,2_training,3_library}.png` |

**踩坑与修正**：
- 首版参数下玻璃"看不见" —— 背景太浅 + 光斑又大又淡（400dp 铺满屏宽），退化成整屏渐变，
  白面板压在浅底上没有对比。修正：基底加深、光斑缩到 250–330dp 并提高饱和度、
  玻璃填充降到 79%→50%、投影加深。
- `WindowInsets.navigationBars.asPaddingValues()` 需要**显式 import** `asPaddingValues`，
  它在 `androidx.compose.foundation.layout` 中是扩展函数，不是 `WindowInsets` 的成员。
- `NavigationBar` 的 `tonalElevation` 必须置 0：否则 Surface 会用 `surfaceTint` 给玻璃再刷一层色，
  玻璃会变成纯色块。
- 截图顺序踩坑：`am force-stop` 后重启 App 会回到起始标签页，按下标顺序拍会把"库"拍成"饮食"。

**遗留**：
- 三页面内容仍为占位，空白较多，玻璃观感在真实列表/图表内容下需再调一轮。
- 未做深色玻璃；若将来需要，须另配一套光斑与描边参数。

---

# 待办：底部导航扩到 5 标签（2026-09-29）

## 目标

新增「食材库」「动作库」两个一级标签，原「库」改名「我的」。

## 设计决策（用户已确认）

| 决策点 | 选定 |
|---|---|
| 标签顺序 | **动作库 / 训练 / 饮食 / 食材库 / 我的**（左侧训练域、右侧饮食域）|
| 「我的」页内容 | **身体数据 + 关于**（原三卡片中动作库/食材库已升为一级标签，不再重复入口）|

## 计划

1. `NavigationKeys.kt`：`Library` 拆为 `ExerciseLibrary` / `FoodLibrary` / `Profile`，按展示顺序排列。
2. `TopLevelDestination.kt`：扩到 5 项，顺序即展示顺序，`START` 保持 `DIET`。
3. 新增 `ui/exercise/ExerciseLibraryScreen.kt`、`ui/food/FoodLibraryScreen.kt`、`ui/profile/ProfileScreen.kt`；
   删除 `ui/library/`。
4. 新增三个图标 drawable（Material Symbols 的 `sports_gymnastics` / `nutrition` / `person`），
   删除失效的 `ic_tab_library.xml`。
5. `strings.xml`：五个标签 + 页面文案；页面标题刻意与标签不同（`onNodeWithText` 唯一性）。
6. 更新测试：JVM 断言 5 项 + 顺序 + 资源唯一性；真机测试覆盖五个标签的点击切换。
7. 同步 `AGENTS.md §4.1`（目录树）与 `§5.3`（图标约定）。

## 验收标准（AGENTS.md §6.4）

- [ ] `./gradlew lint test assembleDebug` 全绿
- [ ] 真机 `connectedAndroidTest` 全过
- [ ] 安装启动无崩溃，logcat 无 `FATAL EXCEPTION`
- [ ] 截屏取证：五个标签均完整显示、切换正常

## Review（2026-09-29）

| 验证项 | 结果 |
|---|---|
| `./gradlew lint test assembleDebug` | ✅ BUILD SUCCESSFUL |
| 真机 `connectedDebugAndroidTest` | ✅ **6/6 passed, 0 failed**（原 4 个 + 新增食材库/我的）|
| 安装 / 启动 | ✅ Success，logcat 无 `FATAL EXCEPTION` |
| 截屏取证 | ✅ `D:\tool\_dl\tabs5\{1_exercise,2_training,3_diet,4_food,5_profile}.png`，五标签完整显示 |

**踩坑与修正**：
- **Material Symbols 的 SVG 不能直接抄**：其 `viewBox` 为 `0 -960 960 960`（y 轴为负），
  而 VectorDrawable 的视口原点固定在左上、无 min-y 概念 —— 直接照搬会让整幅图形落到视口外。
  修正：`viewportWidth/Height="960"` + `<group android:translateY="960">`。
- 该套图标在 960 网格内各字形占比 58%–88% 不等，先用 `render_svg.py` 横排比对再定是否加内边距。
- `render_svg.py` 同步升级：可直接解析 Android VectorDrawable（`viewportWidth`、
  `android:pathData`、`<group translateX/Y>`）与负起点 viewBox。

**遗留**：切标签仍是整体替换返回栈；五个页面均为占位。本轮改动尚未 git 提交。

---

# 待办：dock 只留图标 + 实时背景模糊（2026-09-29）

## 目标

1. 底部 dock **移除中文文字，只留图标**。
2. dock **真正模糊背后的画面**（不是"叠一层半透明白"）。

## 设计与实现

| 决策点 | 选定 |
|---|---|
| 布局 | 手写 `Row`，不用 M3 `NavigationBar`（后者 80dp 最小高度 + 标签强制布局，做不出紧凑纯图标胶囊）|
| 宽度 | **随内容收缩并居中**（5×56dp + 内边距），形成"dock"观感，不再通栏 |
| 中文标签 | 降级为**无障碍描述**（`contentDescription`），不渲染文字 |
| 模糊 | 根节点 `rememberGraphicsLayer()` 录制"背景 + 页面内容" → dock 取层、按 `positionInRoot()` 反向平移、裁进胶囊、施 `BlurEffect` |
| 低版本兜底 | `backdropBlurSupported` 判 API 31+；不可用则退回 `GlassFill*` 厚填充，**不提高 minSdk** |

新增 `ui/common/Backdrop.kt`（图形层 + 能力判断）与 `ui/common/GlassDock.kt`（dock 本体）。

## 验收标准（AGENTS.md §6.4）

- [ ] `./gradlew lint test assembleDebug` 全绿
- [ ] 真机 `connectedAndroidTest` 全过
- [ ] 安装启动无崩溃，logcat 无 `FATAL EXCEPTION`
- [ ] 截屏取证：dock 只有图标、居中；**模糊被证实**（背后高对比内容被抹平，图标仍锐利）
- [ ] 收尾时手机上仍留有 App（用户要上手点按）

## Review（2026-09-29）

| 验证项 | 结果 |
|---|---|
| `./gradlew lint test assembleDebug` | ✅ BUILD SUCCESSFUL |
| 真机 `connectedDebugAndroidTest` | ✅ **7/7 passed, 0 failed**（新增"标签不得渲染为文字"用例）|
| 安装 / 启动 | ✅ Success，`topResumedActivity=com.zifit.app/.MainActivity`，无 `FATAL EXCEPTION` |
| 测试后留装 | ✅ 测试跑完 `pm list packages` 仍有 `com.zifit.app` |
| 截屏取证 | ✅ `D:\tool\_dl\dock\{final_diet,final_exercise,final_diet2,final_profile}.png`；放大图 `zoom_final_*.png` |
| **模糊确证** | ✅ `blur_proof.png` / `zoom_stripes.png`：胶囊内黑白条纹被抹成平滑灰阶，胶囊外锐利，**图标未被误糊** |

**踩坑与修正**：
1. **第一版把 `renderEffect` 挂在 dock 容器上 → 五个图标全被糊掉**（只剩一团白雾）。
   `renderEffect` 作用于该节点及其整棵子树。修正：把模糊副本隔离到 `matchParentSize` 子节点，
   层级固定为 **模糊层 → 白纱/描边 → 图标**。
2. **"背景是平滑渐变"导致模糊无法用截屏判读**（模糊平滑渐变 ≈ 不模糊）。
   决定性验证：临时把内容底部避让改 0 + 页面换成黑白条纹，条纹穿过 dock 时被抹平即证明生效；
   已回滚临时改动（`grep TEMP-BLUR-TEST` 应为空）。
3. `matchParentSize` 是 `BoxScope` 成员，**不能 import**（`Unresolved reference`）。
4. 纯图标 dock 的可读性依赖图标本身：未选中色调由 `InkFaint` 改为 `InkMuted` 提升对比。
5. 调试期间屏幕熄屏会让 `screencap` 拍出全黑图（19KB），与"渲染失败"极易混淆；
   先 `input keyevent KEYCODE_WAKEUP` 再截屏。

**遗留**：
- dock 当前压在**空白背景**上，模糊收益要等内容能滚到 dock 之下才充分体现
  （内容避让为 `ContentBottomSpace`，滚动列表接入后自然会穿过 dock）。
- 切标签仍是整体替换返回栈；五个页面均为占位。
- 累计五轮改动（底部导航 / 毛玻璃 / 图标替换 / 五标签 / 本次 dock）**均未 git 提交**。

---

## Review（2026-09-29 · dock 交互修正）

**需求**：① 点导航栏出现的灰色方块（Material ripple）去掉；② 选中态改为「图标微微变大 + 变蓝」。

**改动**：

| 文件 | 变更 |
|---|---|
| `Navigation.kt` | `selectable` 显式传 `interactionSource = null, indication = null` 关掉涟漪；选中态改为 `animateColorAsState`(→ `AccentBlue`) + `animateFloatAsState`(→ ×1.18) 驱动 `graphicsLayer` 缩放；**删除**薄荷背景胶囊与 `DockIndicatorSize` |
| `theme/Color.kt` | 新增 `AccentBlue`(0xFF2C7BE5，dock 选中态专用) |

**为什么删胶囊**：选中态已由「放大 + 变蓝」两种信号表达；dock 本身就是一个玻璃胶囊，
里面再套一个胶囊属重复装饰，且薄荷胶囊与蓝色图标同屏会串色。

| 验证项 | 结果 |
|---|---|
| `./gradlew lint test assembleDebug` | ✅ BUILD SUCCESSFUL |
| 真机 `connectedDebugAndroidTest` | ✅ **7/7 passed, 0 failed** |
| 安装 / 启动 | ✅ 无 `FATAL EXCEPTION`，测试后仍留装 |
| 选中态取证 | ✅ `dock2/z_1_selected_diet.png`（刀叉蓝且大）、`z_3_after_training.png`（哑铃蓝且大、刀叉退回灰小）|
| **涟漪已去掉的确证** | ✅ `z_2_pressed.png` 与 `z_1_selected_diet.png` **逐字节同大小同内容** —— 长按 dock 项 2.5 秒期间画面零变化，说明按下态**没有任何**指示渲染 |

**取证方法（长按期间截屏）**：`input tap` 拍不到瞬时涟漪，改为把长按放后台再截屏：

```bash
adb -s "$S" shell input swipe 426 2520 426 2520 2500 &
sleep 1
adb -s "$S" exec-out screencap -p > pressed.png
wait
```

**遗留**：同上一节；累计**六轮**改动仍未 git 提交。

---

## Review（2026-09-29 · 动作库接入 RepDB 数据集）

**需求**：动作库使用 RepDB 免费层数据；「我的 → 关于」写署名。
**用户拍板**：列表+详情+插画 / 数据**不入 Git** / 文本**全量中译**。

**新增**

| 文件 | 作用 |
|---|---|
| `tools/fetch_repdb.sh` | 拉取并校验数据集（601 条 / ≥1000 图），排除 `premium-samples/`（Term 6） |
| `data/repdb/RepDbDto.kt` | JSON DTO（含译文覆盖层 DTO） |
| `data/repdb/Exercise.kt` | 领域模型 + 搜索索引字段 |
| `data/repdb/ExerciseTaxonomy.kt` | 170+ 分类枚举 → 中文（通用术语，可入库） |
| `data/repdb/ExerciseRepository.kt` | assets 读取 + 解析 + 进程内单例 |
| `ui/exercise/*LibraryViewModel/Screen` | 搜索 + 部位筛选 + 601 行列表 |
| `ui/exercise/*DetailViewModel/Screen` | 插画（起始/到位切换）+ 要领 + 要点 + 肌群 |
| `ui/exercise/ExerciseCommon.kt` | Coil 图片封装、FilterPill、glassRow、SectionTitle |
| `ic_search.xml` / `ic_back.xml` | 界面图标（Material Symbols，负起点 viewBox 需平移 960） |

**改造**

- `Navigation.kt`：改为**每个一级标签各自维护返回栈**（此前记了多轮的遗留项，本轮兑现）；
  新增 `ExerciseDetail(id)` 二级路由
- `ProfileScreen`：关于卡片补 RepDB 署名（Term 2）+ 健康免责（Term 7）
- 删除模板遗留 `data/DataRepository.kt`
- 新增依赖：`coil-compose:3.3.0`、`kotlinx-serialization-json:1.10.0`

**验证**

| 项 | 结果 |
|---|---|
| `lint test assembleDebug` | ✅ BUILD SUCCESSFUL（单测 11 项，含新增 6 项 taxonomy 测试） |
| 真机 `connectedDebugAndroidTest` | ✅ **7/7 passed** |
| APK 体积 | 31 MB（含约 21 MB 数据集） |
| 截屏取证 | 列表 / 详情 / 关于页 → `D:\tool\_dl\repdb\ui\` |
| **独立返回栈** | ✅ 动作库进详情 → 切「我的」→ 切回动作库，**仍停在详情页** |
| 无崩溃 | ✅ logcat 无 `FATAL EXCEPTION`；测试后仍留装 |

**踩坑**

1. `coil-compose:3.6.3` 传递引入 Compose 1.12.0 → 要求 AGP 9.1 + compileSdk 37，与项目不符；降到 3.3.0（lessons 20）。
2. `render_svg.py` 只认双轴 group 平移 → 单 `translateY` 的图标渲染成全白，差点误判素材有错（lessons 19）。
3. loading 提示文案复用了页面标题 → Compose 测试节点重复（lessons 21）。
4. 详情页 chips 未去重：`category=strength` 与 `goals` 里的 strength 都译成「力量」，同屏出现两次。
5. 部位筛选按中文字符串 `sorted()` 会排成「上臂、全身、前臂、大腿」的乱序，改为按解剖学顺序。

**未完成 / 下轮**

- **中文译文尚未产出**。RepDB 只提供 EN/DE/ES；实测文本体量 **370 KB 英文**
  （描述 601 条 / 要领 3107 条 / 要点 1682 条），中译约 **740 KB**，单轮做不完，必须分批。
  当前界面：分类与肌群已是中文，**动作名与要领仍是英文**。
- 待定：译文是否入库（涉及许可 Term 3 的判断）。




---

# 第九轮：动作库去标题 · 内容滚到悬浮元素之下

## 需求（用户原话拆解）

1. 动作库页面最上方的「动作库」标题与动作统计去掉。
2. dock 栏下面不应该再有遮挡层，内容滚动时透过 dock 可见。
3. 动作库搜索和 tag 区域（全部 / 胸部 / 背部…）下面的遮挡层应该是半透明，内容滚动透过它也是可见的。

## 诊断

第 2、3 条是**同一个病根**：`NavDisplay` 上用 `padding(bottom = dock 高度)` 避让悬浮 dock，
`padding` 会**缩小可滚动容器**，内容被硬截在 dock 之上 → 那截空出来的背景就是「遮挡层」，
毛玻璃也无内容可"透"。同理，动作库把搜索框与筛选胶囊放在 `Column` 里、列表在其下方，
列表顶部被截在胶囊下方，永远滚不上去。

先实测排除的两个候选（避免改错地方）：

| 候选 | 实测 | 结论 |
|---|---|---|
| dock 的 18dp 投影形成暗带 | 采样 dock 上下像素亮度：背景自然梯度 228→218，含投影处偏离 ≤1/255 | 视觉上不可见，但**确实是 dock 下方唯一被绘制的东西**，仍移除 |
| 头部下方有实心图层 | 逐像素扫描 chips 下方 / 列表顶部，无任何异常色块 | 不存在实体图层，是"内容进不去 + 背景不透明"造成的观感 |

## 方案

| 改动 | 文件 |
|---|---|
| 新增 dock 尺寸**唯一来源** + `dockContentInset()` | `ui/common/DockMetrics.kt`（新） |
| 移除 NavDisplay 的底部避让 `padding`；常量改从 DockMetrics 取 | `Navigation.kt` |
| 移除 dock 的 18dp 投影（dock 下方唯一的绘制层） | `ui/common/GlassDock.kt` |
| 搜索 + 筛选改为**悬浮头**：`Box` 叠层 + `onSizeChanged` 测高 → 列表 `contentPadding.top` | `ui/exercise/ExerciseLibraryScreen.kt` |
| 删除 `Header`（标题 + 统计）与两条计数文案 | 同上 + `res/values/strings.xml` |
| `GlassSurface` 新增 `elevated` 形参；搜索框传 `false`（投影会压到滚过的内容上） | `ui/common/GlassSurface.kt` |
| 详情页 / 我的页：`verticalScroll` **之后**补 `padding(bottom = dockContentInset())` | `ExerciseDetailScreen.kt` / `ProfileScreen.kt` |
| 「动作库已就位」判据由标题改为搜索框提示 | `androidTest/BottomNavigationTest.kt` |

## 验证

- [x] `./gradlew lint test assembleDebug` 全绿（单测 11 项；lint 6 warnings，均为模板遗留/版本提示，无 error、无新增）
- [x] 真机 `connectedDebugAndroidTest` **7/7 passed**（PGT-AN00）
- [x] `adb install -r` Success + `am start`（**未卸载**，`pm list packages` 仍在）
- [x] 截屏 `D:\tool\_dl\ui_v4\`：`D_top`（无标题）/ `E_scroll`（滚过头部）/ `H_filter`（筛选态）/ `I_detail`（详情）
- [x] logcat 无 `FATAL EXCEPTION`；APK 30.1 MB

---

# 第十轮：柔化层铺满整屏 · dock 与柔化层再减半

## 需求（用户原话）

配截图：「这个太突兀了，可以把下边的遮罩层宽度拉满。然后 dock 和这个遮罩层的透明度小一半。」

## 诊断

截图里那块"贴上去的岛"= **柔化层（scrim）与内容共用了同一层边距**。
`screenFrame`（安全区内缩 + 16dp）原本套在 `NavDisplay` 上，等于给所有页面一副模具：
柔化层左右各少 16dp、顶部少一个状态栏 → 屏幕边缘露出可见直边。
两个诉求同源：**底衬要铺满，内容要内缩，二者必须分成两层**。

## 改动

| 文件 | 变更 |
|---|---|
| `Navigation.kt` | 外框从 `NavDisplay` 下沉为组合内的 `screenFrame`，由各 entry 自带；动作库刻意传 `Modifier` |
| `exercise/ExerciseLibraryScreen.kt` | 头部 `background(底衬) → onSizeChanged → 安全区内缩 → 8dp/HeaderGap` 四层；列表左右 16dp 走 `contentPadding` + 左右安全区走容器；`Notice` 补左右 16dp |
| `theme/Color.kt` | `HeaderScrim` 0x80 → **0x40**；`DockFillStart` 0xAD → **0x57**、`DockFillEnd` 0x59 → **0x2C** |

## 验证

- [x] `./gradlew lint test assembleDebug` → BUILD SUCCESSFUL
- [x] 真机 `connectedDebugAndroidTest` → **7/7 passed**
- [x] `adb install -r` + `am start`（未卸载，前台运行）
- [x] 截屏：`D:/tool/_dl/ui_v5/{A_top,C_scroll2,D_bottom,E_profile}.png`
- [x] logcat 无 `FATAL EXCEPTION`；APK 30.1 MB

---

# 第十一轮：透明度方向修正 · 头部吞点击 · 搜索框整块可点 · 分类改用参考图词表

## 需求（用户原话）

1. 「减半是指更不透」—— 上一轮把「透明度小一半」理解成更透了，方向反了。
2. 「动作库的搜索和 tag 区域（就是全部、胸部…）滚动他们下面的内容应该是点击不到的」。
3. 「搜索框只能点击『搜索动作/肌群/器械』下面的空白内容才能弹出输入法，
   应该点击输入框的任何位置都弹输入法」。
4. 「动作分类参考图中的分类（胸部/背部/肩部/腿部/臀部/手臂/核心/有氧/颈部/全身/拳击格斗/腹部），
   布局不要参考」。

## 诊断

- 第 1 条：`透明度 ↓ = 不透明度 ↑`。按 `α_new = α + (1-α)/2` 从基线 0xAD/0x59/0x80 重算。
- 第 2 条：浮层只是"画"在列表上，`background` 不参与命中测试 ——
  点在头部空白处会**穿透**到下方列表行，误进详情页。
- 第 3 条：只有 `BasicTextField` 自己有命中区（实测 x[191,1125]），
  左侧放大镜区 x[53,191] 与上下 14dp 内边距都是死区 —— 与用户描述完全一致。
- 第 4 条：RepDB 只有 9 个 `body_part`，参考图 12 类。逐类核数据：
  胸部 61 / 背部 104 / 肩部 73 / 腿部 174(上腿+下腿) / 臀部 159(臀大+臀中肌为主) /
  手臂 76 / 核心 70 / 有氧 16(category=cardio) / 全身 43；
  **颈部 0、拳击格斗 0、腹部 78(与核心重合 92%) → 不设**。

## 改动

| 文件 | 变更 |
|---|---|
| `data/repdb/ExerciseCategory.kt`（新）| 筛选词表枚举 + `matches` / `of` / `sort` / `palette`，声明序即展示序 |
| `data/repdb/Exercise.kt` | 新增 `categories`；`searchHaystack` 纳入分类 |
| `data/repdb/ExerciseRepository.kt` | 映射时用原始 key 算好 `categories` |
| `data/repdb/ExerciseTaxonomy.kt` | 删 `sortBodyParts`（筛选不再用原样部位标签），留注释指路 |
| `ui/exercise/ExerciseLibraryViewModel.kt` | `bodyPart*` → `category*`；只留非空分类 |
| `ui/exercise/ExerciseLibraryScreen.kt` | 头部加 `pointerInput+detectTapGestures` 吞点击；搜索框整块 `clickable`+`FocusRequester`+`keyboard.show()` |
| `theme/Color.kt` | `DockFill*` 0x57/0x2C → **0xD6/0xAC**；`HeaderScrim` 0x40 → **0xC0** |
| `test/.../ExerciseTaxonomyTest.kt` | 换掉 2 条部位排序用例，新增 5 条分类用例（合并/肌群/有氧/未支持/顺序） |

## 验证

- [x] `./gradlew lint test assembleDebug` → BUILD SUCCESSFUL（单测 **16** 项，+5）
- [x] 真机 `connectedDebugAndroidTest` → **7/7 passed**
- [x] 头部吞点击：三处空隙（胶囊间 26px 缝、搜索框与胶囊间 10px 缝、顶部 26px 内边距带）
      点击后截图 md5 **四次全同**
- [x] 阳性对照：点列表行 → 进详情页（截图不同），证明行本身可点、拦截只发生在头部
- [x] 搜索框：点放大镜区（x=120，在输入框节点 x≥191 之外）→ `mInputShown=false → true`，
      节点 `focused="true"`
- [x] 分类：横向滚动可见 全部/胸部/背部/肩部/腿部/臀部/手臂/核心/有氧/全身；
      点「手臂」→ 结果同时含上臂与前臂（合并生效）
- [x] 通透度像素核对：柔化层区域均值 238→249、dock 内纯填充带 239→250、
      胶囊外对照 226→226（不变）；与 `底色×(1-α)+255×α` 公式吻合到 ±1
- [x] logcat 无 `FATAL EXCEPTION`

## 教训

见 `tasks/lessons.md` 27–30。

---

# 第十二轮：搜索框高度恒定 · 动作按难度排序

## 需求（用户原话）

> 搜索框为输入文字和输入文字的高度不一致，以输入文字后的高度为准。然后各类动作的排序，
> 以入门、进阶、高阶排序。

拆成两条：

1. 搜索框在「未输入」与「已输入」两种状态下高度不一致 → **统一以输入态的高度为准**。
2. 动作库列表（含各分类筛选后）按 **入门 → 进阶 → 高阶** 排序。

## 诊断

- 原实现是 `Column { if (value.isEmpty()) Text(提示); BasicTextField(输入框) }` ——
  **提示与输入框是上下堆叠的两个子节点**，空态因此有**两行**、输入态只有一行，
  高度差一整行；再加提示与输入框字体上下留白略有差异，输入瞬间还会再抖一点。
- 排序：`Exercise.difficulty` 存的是**中文标签**（`difficultyLabels` 映射后的结果），
  数据集原序是按英文名字母序，与难度无关。

## 改动

| 文件 | 变更 |
|---|---|
| `ui/exercise/ExerciseLibraryScreen.kt` | 提示文字移入 `BasicTextField` 的 `decorationBox`，并给 `Modifier.matchParentSize()`：**只显示、不参与测量**，框高因此恒等于输入区那一行 |
| `data/repdb/ExerciseTaxonomy.kt` | 新增 `difficultyRank(label)`：权重取 `difficultyLabels` 声明序，词表外返回 `Int.MAX_VALUE` |
| `ui/exercise/ExerciseLibraryViewModel.kt` | `filtered.sortedBy { ExerciseTaxonomy.difficultyRank(it.difficulty) }`（稳定排序，同档内保持数据集字母序） |
| `test/.../ExerciseTaxonomyTest.kt` | 新增 3 条：难度排序权重递增 / 按声明序而非码位序 / 未知值排最后（共 **19** 项） |
| `AGENTS.md` §5.3 | 新增「占位提示走 decorationBox + matchParentSize，禁止上下堆叠；验证要量输入框节点自身 bounds」 |
| `AGENTS.md` §5.4 | 新增「列表排序」小节：难度序取词表声明序、稳定排序、在 ViewModel 排 |

## 验证

- [x] `./gradlew lint test assembleDebug` → BUILD SUCCESSFUL（单测 **19** 项，+3）
- [x] **输入框节点自身 bounds（uiautomator dump）**：
      空态 `[191,149][1125,307]` h=158；输入 `press` 后 `[191,149][1125,307]` h=158
      → 高度差 **0 px**、顶边差 **0 px**
- [x] 版面位移：胶囊行 y 空态 `(359,425)` = 输入态 `(359,425)`；
      列表首行 y 空态 `551` = 输入态 `551`（内容换了、位置没动）
- [x] 像素取证：同区域裁剪 `crop_empty.png` / `crop_typed.png` 版式一致 ——
      搜索框上下边、放大镜、提示与 `press` 的基线、胶囊行、首行顶边全部对齐
- [x] 排序（全新进程、列表未滚动）：全部 → 首行为 `Machine Assisted Dips · 入门`，
      随后 `Assisted Pull Ups / Back Extension / Band Assisted Pull Ups / Band Pull Apart` 全为入门，
      与离线推演（按难度稳定排序）**逐条一致**
- [x] 排序（点「胸部」筛选）：首屏 6 条（`Banded Chest Stretch` … `Incline Push-Up`）
      **全为「入门」**；上一轮同一筛选继续下翻为「进阶 → 高阶」，方向正确
- [x] `install -r` 成功、`lastUpdateTime` 更新、未卸载、logcat 无 `FATAL EXCEPTION`

## 教训

见 `tasks/lessons.md` 31–34。

---

# 第十三轮：食材库（结构先行，AI 接入留待下轮）

## 需求（用户原话）

> 开始做食材库，有搜索功能，可以从这个网站查询 https://www.nutridata.cn/database/list?id=1
> 搜索后可以一键加入食材库。也可以手动创建。食材的参数如图，**把水分去掉**。
> 在关于里面，写上食材的搜索来源为 nutridata

参数图（《中国食物成分表》表头）：名称 / 食部% / ~~水分%~~ / 能量 kcal / 蛋白质 g / 脂肪 g / 碳水化合物 g / 钠 mg，
**以每 100g 可食部计**。

## 决策（用户拍板）

| 问题 | 结论 |
|---|---|
| 数据来源 | **改为向 AI 查询** —— 本轮只搭结构，**下一轮**做 AI 接入 |
| 本地存储 | **Room / SQLite** |

调研结论（详见 `.workbuddy/memory/2026-09-29.md`）：nutridata 网页端接口要复刻其 AES+RSA
（客户端自造 AES 密钥→RSA 包进 `nutridata-random` 头，请求体 AES-128-ECB、响应同法加密），
属绕过其反自动化措施；官方开放平台 `oapi.nutridata.cn` 报价 ¥15 万/年起。
**两条路都不走**，改留 AI 查询的接缝，关于页仍按要求署名 nutridata。

## 方案

分三层，与动作库同构，便于下一轮把 AI 实现塞进已有接缝：

1. **数据层** `data/food/`
   - `Food`（Room `@Entity`）：名称 + 6 个营养字段，**全部按每 100g 可食部**；**不含水分**。
   - `FoodDao` / `FoodDatabase`（Room，`exportSchema = false`，见下方说明）。
   - `FoodRepository`：数据访问唯一出口，向 UI 暴露 `Flow<List<Food>>`。
   - **`FoodSearchSource` 接口 + `FoodSuggestion` 模型** = 下一轮 AI 接入的**唯一接缝**。
     本轮实现 `NotAvailableFoodSearchSource`（返回 `Unavailable`），UI 如实提示「尚未接入」。
2. **ViewModel 层** `ui/food/FoodLibraryViewModel.kt`：本地列表 + 关键词过滤 + 远程搜索结果/状态。
3. **UI 层** `ui/food/FoodLibraryScreen.kt`：沿用动作库的**无标题 + 悬浮玻璃头**版式。

## 改动

| 文件 | 变更 |
|---|---|
| `gradle/libs.versions.toml` | 新增 `ksp` / `room` 版本与库 |
| `app/build.gradle.kts` | 加 `ksp` 插件、Room 依赖、`ksp {}` 配置 |
| `data/food/Food.kt` `FoodDao.kt` `FoodDatabase.kt` `FoodRepository.kt` | 新增 |
| `data/food/FoodSearchSource.kt` | 新增（AI 接入接缝 + 本轮占位实现） |
| `ui/food/FoodLibraryViewModel.kt` `FoodLibraryScreen.kt` | 重写（原为占位页） |
| `ui/food/FoodEditorDialog.kt` | 新增（手动创建/编辑表单） |
| `res/values/strings.xml` | 新增食材库文案 + 关于页 nutridata 署名 |
| `ui/profile/ProfileScreen.kt` | 关于卡片加「食材搜索来源」一行 |
| `test/.../FoodTest.kt` `FoodLibraryViewModelTest.kt` | 新增 |

## 待验证（完成前必须全绿）

- [x] `./gradlew lint test assembleDebug` 全绿 —— **BUILD SUCCESSFUL**，单测 **46** 项全过；
      lint 12 warnings，全部为既有项（`NewerVersionAvailable` ×5、`UnusedResources` ×4、
      `TypographyOther`、`ObsoleteSdkInt`、`DefaultLocale` 在 `ExerciseDetailScreen`），
      **食材库新代码零新增告警**
- [x] 真机 `connectedDebugAndroidTest` 通过 —— **7 tests / 0 failed**（`PGT-AN00 - 16`）。
      连接顺序：`adb pair 192.168.31.196:40593 741872` → `adb connect :41275`。
      过程与教训见 `tasks/lessons.md` 40–46（含 Bash 环境注入 `http_proxy` 需先 `unset`）。
- [x] 手动创建食材 → 落库 → 重启后仍在 —— 录入 `Oats`（食部 100 / 能量 389 /
      蛋白 16.9 / 脂肪 6.9 / 碳水 66.3 / 钠 3.7）→ 保存后列表出现该行
      （`食部 100% · 蛋白 16.9 · 脂肪 6.9 · 碳水 66.3 · 钠 3.7` + 右侧 `389 kcal`）→
      `am force-stop` 后重启**仍在**。删除路径同时验证（先删掉了上一轮的半成品）。
- [x] 搜索框过滤本地列表正确；关键词非空时如实提示「营养库查询尚未接入」
      —— 搜 `Oa` 命中 `Oats` 且「营养库」段显示未接入提示；
      搜 `zz` 同时出现「我的食材里没有匹配「zz」的条目。」与「营养库查询尚未接入…」；
      清空后 `Oats` 回来。
- [x] 关于页出现 NutriData 署名 —— 「我的 → 关于」含
      「食材搜索来源：NutriData（nutridata.cn），营养数据参考《中国食物成分表》。」
- [x] logcat 无 `FATAL EXCEPTION`（各阶段多次 `logcat -c` + `-d` 后均为空）

## ⚠️ 真机验证中发现并修掉的产品缺陷（本轮新增）

| 缺陷 | 现象 | 修法 |
|---|---|---|
| 编辑弹窗溢出 | 7 栏放不下，最后一栏「钠」被压成 **21dp** 并与「取消/保存」**重叠**（`钠` 的 EditText bounds 与按钮交叉） | ① `GlassSurface` 加 `heightIn(max = 宿主可用高 * 0.88f)`；② 字段区改 `weight(1f, fill = false) + verticalScroll`；③ 高度取 `WindowInfo.containerSize` 且在 `Dialog {}` **之外**取（见 `AGENTS.md §5.5`） |

修复后实测：7 栏高度**全部 158px**、最后一栏底 `2180` < 按钮顶 `2231`（不再重叠）；
lint 告警仍为 12 条（换掉 `screenHeightDp` 后 `ConfigurationScreenWidthHeight` 清零）。

## 尚未用自动化覆盖（需人工在真机确认）

- **中文录入**：Android 16 的 `adb shell input text` 遇 CJK 直接抛
  `NullPointerException`（见 `tasks/lessons.md` 43），故自动化只填了 ASCII 名 `Oats`。
  中文输入与中文检索需用户手动点一遍。

## 本轮修掉的两个测试失败（记录，供下轮对照）

| 失败 | 病因 | 修法 |
|---|---|---|
| `FoodFormatTest.decimalsAreKeptToTheRequestedPrecision` | `round(6.85 * 10) / 10` 得 `6.8`（`6.85` 的二进制值是 `6.8499…`） | 改 `BigDecimal.valueOf(x).setScale(n, HALF_UP)`；`stripTrailingZeros().scale() <= 0` 时走 `toBigInteger()` 去掉小数点。不再用 `String.format` |
| `FoodLibraryViewModelTest.loadsExistingFoods_newestFirst` | 测试直接 `dao.rows.value = listOf(...)`，**绕过 `FakeFoodDao.sorted()`**，「最新在前」测的其实是入参顺序 | 夹具新增 `setAll(list)`（内部走 `sorted()`），三处直接赋值改用之 |

教训录入 `tasks/lessons.md` 36–46（36–39 编译/单测期，40–42、45–46 真机自动化脚本期，43–44 真机发现的产品缺陷）。

## 说明：`exportSchema = false`

本轮是数据库第 1 版，没有历史数据要迁移，先关掉 schema 导出少一个活动件；
**首次加迁移时**必须改成导出并提交 `app/schemas/`，否则 `MigrationTestHelper` 无从校验。

---

# 第十五轮：卡片宽度对齐 / 移除食部与钠 / AI 接入配置

## 目标（用户三点要求）

1. 食材库卡片的宽度**参考动作库卡片**。
2. 食材数据**移除「食部」「钠」**两个参数。
3. 「我的」页**新增 AI 接入配置**功能。

## 一、卡片宽度对齐（先量后改）

`uiautomator dump` 实测（1224px 屏）：

| 页面 | 行卡片 x 区间 | 宽 |
|---|---|---|
| 动作库 | `53..1171` | 1118px |
| 食材库（改前） | `106..1118` | 1012px |

病因：`Navigation.kt` 给 `FoodLibrary` 传了 `screenFrame`（含 `padding(horizontal = 16.dp)`），
叠加页面自身的 `contentPadding(16.dp)` → 两侧各多 16dp。
修法：与动作库一致**不传 `screenFrame`**，安全区与边距由 `FoodLibraryScreen` 自己兜
（它本来就有 `windowInsetsPadding(…Horizontal)` + `contentPadding`）。
顺带把柔化头也铺到了屏幕边缘（原本那条「柔化层必须铺满整屏」的注释这才名副其实）。

改后实测两页均为 `x 53..1171`。

## 二、移除食部与钠

| 层 | 改动 |
|---|---|
| `data/food/Food.kt` | 删 `ediblePercent` / `sodiumMg`；注释说明为何只留能量/蛋白/脂肪/碳水 |
| `FoodDraft.kt` | `FoodField` 由 7 项减到 5 项；`toFood` / `toDraft` 同步 |
| `FoodSearchSource.kt` | `FoodSuggestion` 删两字段 |
| `FoodEditorDialog.kt` | 表单由 7 栏减到 5 栏；输入框提级为 `ui/common/GlassTextField`（AI 弹窗共用） |
| `FoodLibraryScreen.kt` | 行摘要只剩 `蛋白 · 脂肪 · 碳水`，`nutrientLine` 由 5 参减到 3 参 |
| `strings.xml` | 删 `food_row_edible` / `food_row_sodium` / `food_editor_edible` / `food_editor_sodium` |
| 单测 | `FoodDraftTest` 同步；`FoodLibraryViewModelTest` 的 `FoodSuggestion` 夹具改字段 |

**数据库迁移**：`version = 1 → 2`，新增 `MIGRATION_1_2`，走**重表法**
（`CREATE food_new → INSERT … SELECT → DROP food → RENAME`）——
`ALTER TABLE … DROP COLUMN` 要 SQLite 3.35+，而本项目 minSdk 26 自带 3.18。
同时按上一轮留下的要求把 `exportSchema` 打开、加 `ksp { arg("room.schemaLocation", …) }`，
`app/schemas/`（`2.json`）随仓库提交。
⚠️ v1 的 schema 当时未导出，故这条迁移**没有 schema 可比对**，靠**真机升级安装**验证（见下）。
依然**不用** `fallbackToDestructiveMigration`（会静默清库）。

## 三、AI 接入配置

| 文件 | 职责 |
|---|---|
| `data/ai/AiConfig.kt` | 配置模型 + 表单草稿 + 纯函数校验（`AiConfigDraft.toConfig()`）+ `maskApiKey` |
| `data/ai/AiConfigRepository.kt` | **接口** + `SharedPrefsAiConfigRepository`（`SharedPreferences("ai_config")`） |
| `data/ai/AiConnectionTester.kt` | **接口** + `HttpAiConnectionTester`：`GET {baseUrl}/models` 带 Bearer；`classifyNetworkError` |
| `data/ai/AiPresets.kt` | DeepSeek / 阿里云百炼的填空预设（值复核于 2026-09-30） |
| `ui/profile/ProfileViewModel.kt` | 配置流 + 连通性测试状态（含**代次号**，作废过期的在途结论） |
| `ui/profile/AiConfigDialog.kt` | 三项输入 + 预设胶囊 + 测试连接 + 清除/取消/保存 |
| `ui/profile/ProfileScreen.kt` | 新增「AI 接入」卡片：状态 / 服务地址 / 模型 / 密钥掩码 / 缺少项 / 隐私说明 |
| `AndroidManifest.xml` | 新增 `INTERNET` 权限（唯一新增权限） |

设计取舍：密钥**明文**存应用私有目录（`androidx.security.crypto` 已停止维护，不引黑盒），
代价是必须保证**不进日志、不进 Git、界面不出现完整明文**；
连通性测试只打 `/models`（不烧 token），404 单独成一档（有些代理不提供该端点）。

## 验收（AGENTS.md §6.4）

- [x] `./gradlew lint test assembleDebug`：BUILD SUCCESSFUL；
      **单测 65 项全过**（本轮新增 18 项：`AiConfigTest` 9 / `NetworkErrorClassificationTest` 6 /
      `ProfileViewModelTest` 3+1）；
      lint 仍为 **12 warnings**，构成与上轮完全一致（DefaultLocale 1 / NewerVersionAvailable 5 /
      ObsoleteSdkInt 1 / UnusedResources 4 / TypographyOther 1）→ 无新增告警。
- [x] 真机（`PGT-AN00` / Android 16）升级安装成功，**v1 库里的 `Oats` 迁移后仍在**（未崩、未清库）。
- [x] 食材库行摘要变成 `蛋白 16.9 · 脂肪 6.9 · 碳水 66.3`（食部/钠消失）；编辑弹窗 5 栏。
- [x] 卡片宽度：动作库 `x 53..1171` = 食材库 `x 53..1171`（逐像素一致）。
- [x] AI 接入：未配置态显示「缺少：服务地址（Base URL）、API Key、模型」→ 保存后变
      「已配置」并显示掩码密钥 → `am force-stop` 重启后**仍在** → 「清除配置」回到未配置。
- [x] 连通性测试如实报错：`https://example.invalid/v1` → 「域名解析失败：…」（中文，非 Java 原文）；
      无协议头地址保存 → 「服务地址要以 http:// 或 https:// 开头」且**未落库**。
- [x] 改输入后旧的测试结论**立即消失**（代次号修复，真机复验通过）。
- [x] logcat 无 `FATAL EXCEPTION`。

## 本轮真机验证抓出的缺陷

| 缺陷 | 现象 | 修法 |
|---|---|---|
| 测试结论过期 | 改掉服务地址后，屏幕上那条「域名解析失败」说的还是**旧地址** | ViewModel 加代次号：输入变更推进代次，在途结果回来时核对，不符即丢 |
| 报错话术不可读 | 真机报 `Unable to resolve host "example.invalid": …`（Java 原文，英文） | `classifyNetworkError` 分 DNS / TIMEOUT / REFUSED / TLS / OTHER，界面说人话 |

教训录入 `tasks/lessons.md` 47–52。

## 遗留

- **AI 查询尚未接进 `FoodSearchSource`** —— 本轮只做「配置 + 连通性」，下一轮把它换成真实现
  （读 `AiConfigRepository.config`，`Unavailable` 换成真实请求结果）。
- 密钥明文存储：若将来 APK 要外发，需重新评估（当前仅侧载自用，已如实写在界面上）。

---

# 第十六轮：`.env` → 调试用 AI 配置注入（仅 debug）

## 需求
仓库根新建 `.env`，放 DeepSeek 的地址 / key / 模型名，**调试时可直接用**，
不必每次重装都在界面上重填。

## 一、数据通路

```
.env（不入库）→ app/build.gradle.kts 配置期读取 → buildConfigField
   → BuildConfig.AI_ENV_{BASE_URL,API_KEY,MODEL} → data/ai/AiEnvDefaults
   → SharedPrefsAiConfigRepository 的默认参数 → 界面
```

| 文件 | 改动 |
|---|---|
| `.env` | **新建**，三个键 + 注释；`.gitignore` 已加 `.env` |
| `.env.example` | **新建**（入库），同名三键 + 占位值 |
| `app/build.gradle.kts` | 读 `.env`、转义、`buildConfig = true`；`debug` 注入真值，`release` 注入空串；构建时打一行提示（只说齐不齐，不打内容） |
| `data/ai/AiEnvDefaults.kt` | **新建**：`AiEnvDefaults.config/isAvailable` + 纯函数 `resolveAiConfigSource` |
| `data/ai/AiConfig.kt` | 新增枚举 `AiConfigSource { NONE, SAVED, ENV }` |
| `data/ai/AiConfigRepository.kt` | 新增 `source` 流、`envDefaults` 注入参数、`env_opt_out` 标记 |
| `ui/profile/ProfileViewModel.kt` | 新增 `source`、`envDefaults`（未配齐为 null） |
| `ui/profile/ProfileScreen.kt` | 卡片新增 `来源：` 行；ENV 档换一套隐私文案 |
| `ui/profile/AiConfigDialog.kt` | 新增「载入 .env」胶囊（仅三项齐全时显示） |
| `strings.xml` | +5 条（来源行 / 两个来源名 / ENV 隐私说明 / 载入 .env） |
| `AGENTS.md` | 新增**硬约束 C13** + §5.6 完整说明 + §3.2 提示 + 目录树补 `.env` |
| `README.md` | 新增「本地开发：调试用密钥」一节；顺手修掉一处过期的「待办」 |

## 二、关键设计取舍

- **优先级：用户保存的 > `.env` > 无**。`.env` 只用于省打字，绝不覆盖用户在界面上填的值。
- `.env` 的值**不写进 prefs**（本来就在 APK 里）。代价是 `clear()` 后会被重新读到 →
  用 `env_opt_out` 标记兜住，保证「清除配置」按下去真的清掉。
- **release 不注入**：`buildTypes.release` 里显式声明同名同类型的空串
  （只写 debug 会让读字段的代码在 release 编译不过；写进去又是泄露）。
- 界面**标来源**：掩码密钥分辨不出来源。ENV 档的隐私说明也要换（那时密钥在 dex 里，
  说「保存在本机私有目录」是假话）。

## 验收（AGENTS.md §6.4）

- [x] `./gradlew lint test assembleDebug`：BUILD SUCCESSFUL；
      **单测 75 项 0 失败**（本轮 +10：`AiEnvDefaultsTest` 4 / `AiConfigRepositoryTest` 5 /
      `ProfileViewModelTest` +1）；lint 仍 **12 warnings**，构成与上轮一致（无新增）。
- [x] 生成物核对：debug 的 `AI_ENV_*` 三项有值；**release 三项全是 `""`**。
- [x] `.env` 不入库：`git check-ignore -v .env` 命中；`git status` 只出现 `.env.example`；
      真密钥在**已跟踪文件**与**未跟踪且未被忽略的文件**里都搜不到。
- [x] 真机（`PGT-AN00` / Android 16）：删掉 `shared_prefs/ai_config.xml` 后冷启动 →
      卡片直接「已配置 / 来源：仓库根 .env（构建时注入）/ 密钥 sk-••••6823」。
- [x] 弹窗三项已按 `.env` 预填，「载入 .env」胶囊如期出现（首次验的是测试值，用后即清）。
- [x] 「清除配置」→ 未配置（`.env` **没有**把值灌回来）→ 重开 →「载入 .env」一键填回 → 保存 → 来源变「本机保存」。
- [x] 顺带用真密钥打了一次连通性测试：**「连接正常（HTTP 200），地址与密钥可用。」**
      —— 预设里的 `https://api.deepseek.com`（不带 `/v1`）配 `/models` 是通的。
- [x] logcat 无 `FATAL EXCEPTION`；测试用的假 key 已从手机与 `.env` 中清除。

截屏归档：`D:\projects\自由健身\screenshots\round16\`。

教训录入 `tasks/lessons.md` 53–57。

## 遗留

- AI 查询**仍未**接进 `FoodSearchSource`（下一轮：`Unavailable` → 真实请求）。
- 第十三轮以来的改动**仍未 git 提交**（含 `app/schemas/`、`.env.example`、本轮文档）。

---

# 第十七轮：「我的」页改版（头像 + 设置 + 二级页）

## 需求（用户原话拆解）

1. 移除「我的」页最上方的「个人中心」**标题**；
2. 最上方左边＝头像 + 用户名，右边＝设置图标；
3. 点设置 → **设置页**：第一行「个人资料」，接着「AI 接入」，最后「关于」；
4. 点「个人资料」→ 个人资料设置页（名称 / 生日 / 年龄 / 目标 / 头像）；
5. 点「AI 接入」→ AI 接入设置页（原先是弹窗，本轮改为整页）；
6. 点「关于」→ 关于页。
   （用户原文最后一句写成「点击 AI 接入进入关于页面」，按上下文与前三句的排布判定为**笔误**，
   取「点击关于 → 关于页」。已在交付说明里点明这一处推断。）

## 一、路由与结构

`NavigationKeys.kt` 新增 `ProfileSettings` / `ProfileEdit` / `AiSettings` / `About`；
`Navigation.kt` 各加一个 entry（共用「我的」那条返回栈 → 「设置 → 个人资料 → 返回」回到设置页）。
`ui/common/PageHeader.kt`（新）提供二级页三件套：`PageHeader`（返回 + 标题 + 尾部动作）、
`CircleIconButton`、`NavRow`（标题 + 当前值副标题 + 箭头）、`HintText`。
新增矢量图标 `ic_settings` / `ic_chevron_right` / `ic_person`（无依赖）。

```
我的(无标题)  →  设置  →  个人资料 | AI 接入 | 关于
```

## 二、个人资料（新数据层 `data/user/`）

| 文件 | 职责 |
|---|---|
| `UserProfile.kt` | 模型 + `Goal` 枚举（增肌/减脂/保持/增力/健康）+ `UserProfileDraft` + 纯函数校验 `toProfile(today)` + `ageFromBirthday` |
| `UserProfileRepository.kt` | 接口 + `SharedPreferences("user_profile")` 实现（年龄用「键存在性」区分「没填」与 0；枚举名存盘，读到不认识的值降级为「没选」） |
| `AvatarStore.kt` | 把相册选中的图**复制进 `filesDir/avatar/`**，文件名带时间戳（Coil 按路径缓存），换/删头像时清旧文件 |

UI：`ProfileScreen`（头像 + 用户名 + 设置图标，**无标题**）/ `SettingsScreen` / `ProfileEditScreen`
（头像可点、换：`PickVisualMedia`，API 33 以下自动回退 `ACTION_OPEN_DOCUMENT`，**零权限**）
/ `AiConfigScreen`（原 `AiConfigDialog` 改整页，文件已删）/ `AboutScreen`；
`ProfileLabels.kt` 收敛目标与副标题文案；`UserProfileViewModel` 承载草稿与头像落盘。

## 三、真机抓出来的三个问题（本轮主要产出）

| 问题 | 现象 | 修法 |
|---|---|---|
| 可点元素被 dock 盖住 | AI 接入页把只读状态卡放最上面 → 「保存」与测试结论落到 `y2478`，而 dock 上沿 `y2477`；点「测试连接」屏幕上什么都不变 | 表单在前、状态卡退到最后；结论**贴在按钮旁**（同 Row + `weight(1f)`）。改后「保存」`y1680..1746`、结论 `y1433..1614`，全在首屏 |
| 新增「移除头像」把「保存」挤下去 | 保存从 `y2348` 掉到 `y2500+`（正好被 dock 吃掉） | 「移除头像」与「点击头像更换」摆**同一行** → 保存收回 `y2328..2394` |
| 头像设了就撤销不掉 | —（设计缺口，非 bug） | 补「移除头像」胶囊：清空草稿路径 → `save()` 删旧文件 |

## 验收（AGENTS.md §6.4）

- [x] `./gradlew lint test assembleDebug`：BUILD SUCCESSFUL；
      **单测 94 项 0 失败**（本轮 +19：`UserProfileTest` 12 / `UserProfileViewModelTest` 7）；
      lint 仍 **12 warnings**，构成与上轮逐条一致（无新增；两处日期短横线用
      `tools:ignore="TypographyDashes"` 挡住误报 —— ISO 日期换成 en dash 就 parse 不了）。
- [x] 「我的」页：无「个人中心」标题，左头像 + 用户名、右设置图标（`content-desc=设置`）。
- [x] 设置页三行：个人资料（副标题=当前名）/ AI 接入（副标题=已配置|未配置）/ 关于。
- [x] 个人资料：填 `JZY` / `1998-06-01` / 不填年龄 / 选「增肌」→ 保存 → 设置页副标题变 `JZY`；
      首页头部 `JZY` + `增肌 · 28 岁`（**年龄由生日推算并落库**，见 `shared_prefs/user_profile.xml`）。
- [x] 冷启动（`am force-stop` + 重开）后资料仍在。
- [x] 头像：点头像 → 系统相册（`com.android.providers.media.photopicker.PhotoPickerActivity`，
      **未申请任何权限**）→ 选中测试图 → 头像即刻渲染 → 保存后
      `files/avatar/avatar-<ts>.jpg` 出现、`avatar_path` 落库；「移除头像」后文件被删、路径清空。
- [x] 校验：生日填 `1998/06/01` → 报「生日请按 1998-06-01 这样的格式填写，或留空」且**不落库**、不跳走。
- [x] AI 接入页（整页版）：状态含服务地址/模型/**掩码密钥**/来源；三栏预填自 `.env`；
      「测试连接」用真密钥得**「连接正常（HTTP 200），地址与密钥可用。」**（结论紧贴按钮，可见）。
- [x] 关于页：GPL 声明、源码地址、RepDB 署名、NutriData 说明、健康免责、版本 1.0（取 `BuildConfig`）。
- [x] logcat 无 `FATAL EXCEPTION`；交付前把手机上的**测试资料与测试头像清空**
      （`user_profile.xml` 只剩三个空串，`age`/`goal` 键已移除，`files/avatar` 空），
      并删掉推进相册的那张测试图。

仪器测试：`androidTest/.../profile/ProfileNavigationTest`（5 条下钻路径）+
`BottomNavigationTest.tapProfile_showsHomeHeaderAndBodyCard` 改写（不再断言已删除的「个人中心」）。
⚠️ 仪器测试需真机，本轮**未跑**（沿用既有约定：构建 + 手工真机核对）。

截屏归档：`D:\projects\自由健身\screenshots\round17\`（9 张）。

教训录入 `tasks/lessons.md` 58–62。

## 遗留

- AI 查询**仍未**接进 `FoodSearchSource`（下一轮：`Unavailable` → 真实请求）。
- 身体数据（体重/体脂/围度）仍是空卡片；个人资料里的年龄/目标将来要喂给计划生成。
- 第十三轮以来的改动**仍未 git 提交**（含 `app/schemas/`、`.env.example`、本轮新文件）。

---

## 第十八轮（2026-09-30）·「我的」页第二轮打磨

### 需求（用户原文四点 + 隐含一点）

1. 「我的」顶部那行**「点头像右侧的设置…」引导语移除**；
2. **设置图标去掉白色圆底**（用户不确定那是什么：「背景色（或者是边框？）」—— 实为 `GlassRowFill` 半透明白填充 + 描边）；
3. 个人资料页**头像卡片去掉「点击头像更换」与「移除头像」两个按钮**；
4. **保存键移到页面最上方右边**；
5. **生日改成选择框架**（不再用键盘输入）。

### 实现

| 改动 | 落点 |
|---|---|
| `CircleIconButton` 增加 `filled: Boolean = true` | `ui/common/PageHeader.kt`；设置入口传 `false`，触摸区仍是 40dp |
| 新增 `GlassPickerField(label, value, placeholder, onClick)` | `ui/common/GlassPickerField.kt`；与 `GlassTextField` 同框型，右端箭头 |
| 首页副标题空时整行不渲染 | `ProfileLabels.profileSubtitle()` 返回 `String?`；`profile_header_hint` 字符串删除 |
| 保存键上移 | `PageHeader(trailing = { FilterPill(保存) })`；校验错误 `HintText` 紧贴页头下方（报错必须落在保存键的同一眼里） |
| 生日选择器 | `GlassPickerField` + Material3 `DatePickerDialog`（`title = null` 省高度，`showModeToggle = false`）；没填过默认落在「今天 − 25 年」；**清除按钮只在已有值时出现** |
| 毫秒 ↔ 日期 | `data/user/UserProfile.kt` 新增 `toPickerMillis()` / `pickerMillisToLocalDate()`，**钉死 UTC** |
| 字符串清理 | 删 `profile_avatar_pick` / `profile_avatar_remove` / `profile_header_hint`；`profile_field_birthday` 简化为「生日」；`profile_error_birthday` 改为「生日日期无效，请重新选择」；`xmlns:tools` 与两处 `TypographyDashes` ignore 一并移除（没有日期示例可排错了） |

### 测试

- `UserProfileTest` +3（`pickerMillis_isUtcMidnight_notSystemZoneMidnight` 用**固定毫秒常量** 896659200000
  锁 UTC —— 往返测试在东八区**抓不住**「偷用系统时区」（两方向同偏 8 小时恰好同天），常量才能抓住）；
  `pickerMillis_roundTripsAcrossLeapDayAndEpoch`（闰日 + 1969 负毫秒）；`birthdaySavedFromPicker_isTheSameDayAsPicked`。
- `ProfileNavigationTest` +1（`editor_birthdayIsPickerNotTextField`：占位「未设置」可见 → 点**所在行**（`hasClickAction() and hasAnyDescendant(hasText(...))`，Text 节点本身没有点击动作）→ 对话框出现）；`settingsProfile_opensEditor` 增加保存键在页头的断言。
- 真机核对：`uiautomator dump` 逐屏验证 —— 引导语消失、齿轮无底（截图目检）、保存键 `[1031,185][1125,251]` 在页头、
  头像卡片无按钮、生日选 2001-09-15 → 字段回填 `2001-09-15` + 「按生日推算：25 岁」（UTC 转换正确，非 09-14）、
  重开选择器定位到已存生日且「清除」出现、清除后保存 → 我的页副标题消失、`age` 键从 prefs 移除。
- `lint test assembleDebug`：BUILD SUCCESSFUL，**单测 97 项 0 失败**（+3）；lint 仍 12 warnings
  （DefaultLocale 1 / NewerVersionAvailable 5 / ObsoleteSdkInt 1 / UnusedResources 4 / TypographyOther 1，
  两处 `TypographyDashes` 随文案删除清零，无新增）。logcat 无 FATAL EXCEPTION。

### 真机截图

`D:/projects/自由健身/screenshots/round18/`（7 张）。

### 观察与遗留

1. **清除生日后，此前推算的年龄会残留**（如存过 2001-09-15 → 清生日 → 年龄框里躺着"25"且照常落库）。
   根因：`toDraft()` 把推算年龄回填成**显式**草稿文本，保存后与手填无法区分。
   不是本轮回归（文本框时代删生日同样发生）。要修需在草稿/库里区分「推算来的年龄」，本轮不动，记录在案。
2. 头像失去「移除」路径（用户要求）；换头像仍可点头像完成。
3. AI 查询仍未接进 `FoodSearchSource`；身体数据仍空卡片；**改动仍未 git 提交**。

---

## 第十九轮（2026-09-30）· 主题系统改造（可切换主题模式 + 主题色）

### 需求（用户原文四点）

1. **保留**「毛玻璃模糊」设置；
2. **去掉**「界面动效」；
3. 新增**浅色 / 深色**模式（现有那版定义为「彩色」）；
4. 新增**主题色**，决定底部导航选中、动作库选中分类高亮、食材库热量数字与「新建」、
   「我的」页设置图标的颜色。

### 实现

把 `theme/Color.kt` 从「一组顶层色常量」改造为**三套调色板**（`ZiFitPalette` 数据类）：

| 维度 | 取值 | 实现 |
|---|---|---|
| 主题模式 | 彩色（默认）/ 浅色 / 深色 | `ThemeMode` → 三套 palette 三选一 |
| 主题色 | 蓝（默认）/ 薄荷 / 紫 / 陶土 / 玫红 | `AccentColor` → `.copy(accent = …)` |

| 改动 | 落点 |
|---|---|
| 旧顶层色常量 → `@Composable @ReadOnlyComposable` getter | `Accent` / `InkMuted` / `GlassRowFill` … ；**170+ 处调用点一行没改** |
| 按语义改名 | `AccentBlue`→`Accent`、`Mint600/100/900`→`Brand/BrandContainer/OnBrandContainer`、`Violet600/Coral600/Sky700`→`IconViolet/IconCoral/IconSky`（一次性 grep 全量替换，含 androidTest） |
| 偏好层 | 删 `dockMotion`，加 `themeMode` / `accent`（枚举按 `name` 存，读回容错）；`MainActivity` 读偏好后把参数传给 `ZiFitTheme`（主题是纯函数）；`ZiFitTheme` 内 `SideEffect` 按 `isDark` 覆写系统栏图标明暗 |
| 设置页 | 用户界面页改**两张卡**（主题：模式 `ChoiceRow` + 主题色 `SwatchRow`；效果：毛玻璃开关）；设置页那行副标题改「彩色 · 主题色蓝 · 毛玻璃开」 |
| 图标 | 新增 `ic_contrast.xml`（Material Symbols `contrast`，离线渲染校验过），删除 `ic_motion.xml` |
| 文案 | 新建 `ui/appearance/AppearanceLabels.kt`，两个页面共用同一份说法 |

### 测试 / 门禁

`lint test connectedAndroidTest`：**14/14 全绿**，单测 97。真机逐档截图核对
彩色 / 浅色 / 深色 × 蓝 / 玫红，覆盖 dock 选中、筛选胶囊、食材库「新建」与热量数字、
设置图标、设置页副标题；系统栏图标在深色下自动转浅。
**验证后已恢复默认（彩色 + 蓝 + 毛玻璃开）**，APK 已重装（31.9 MB）。

### 遗留

- 手机上 `ui_preferences.xml` 里仍留着废弃键 `dock_motion`（无害，代码不再读）。
- Git 仓库仍未提交。

---

## 第二十轮（2026-09-30）· 主题模式改 2×2 四档

### 需求

「彩色」改名「**彩色·浅**」，「深色」改名「**彩色·深**」（因为**它本来就是彩色的深色版本**），
再补一个与「浅色」对应的「**深色**」→ 最终是 **{彩色, 中性} × {浅, 深}**。

### 实现

- `ThemeMode` 三值 → 四值：`COLORFUL_LIGHT / NEUTRAL_LIGHT / COLORFUL_DARK / NEUTRAL_DARK`
  + `companion { Default; fromStorage() }`；调色板变量随语义改名
  （`ColorfulLightPalette` / `NeutralLightPalette` / `ColorfulDarkPalette` / `NeutralDarkPalette`），
  `paletteOf` 四分支，`LocalZiFitPalette` 默认 → `ColorfulLightPalette`。
- **新增中性深色**（与浅色对应）：以 `ColorfulDarkPalette.copy` 为基础，三处刻意不同 ——
  ① 基底去蓝（`0xFF121417 / 16181C / 1B1E23`）；② 四团光斑统一成钢灰 `0xFF5E6C7C`
  （浅色那套 `0x8E9BAE` **不能复用**：深底上光斑要比底亮，浅底上要比底暗）；
  ③ `blobAlphaScale` 回到 1.0（中性没有色相可透，1.15 只会把灰斑推成灰雾）。
- `UiPreferencesRepository.read()`：主题模式改走 `ThemeMode.fromStorage()`
  （认 `COLORFUL` / `LIGHT` / `DARK` 三个旧名），**通用 `enumOrDefault` 只留给 `AccentColor`**。
- `ChoiceRow` 的胶囊容器 `Row` → `FlowRow`，新增可选参数 **`maxPerRow`**（`null` = 自动换行；
  UI 页传 2 → 严格 2×2 排法）。
- `strings.xml`：新增 `theme_mode_colorful_light/dark`、删 `theme_mode_colorful`；
  副标题改「带「彩色」的两档有彩色光斑，另两档是中性灰；浅深按昼夜挑。」
- `ProfileNavigationTest`：断言四档**同时可见** + 选「深色」→ `ThemeMode.NEUTRAL_DARK`。

### 测试 / 门禁

`lint test connectedAndroidTest` 全绿（单测 97，仪器 14/14）。
✅ 真机验证：手机上原值是 `LIGHT`，升级后正确显示「浅色」，**迁移生效**；
逐档截图核对渲染差异。**验证后已恢复用户原有偏好（浅色 + 主题色紫 + 毛玻璃关）**，APK 已重装。

### 文档

`AGENTS.md` §5.3：调色板表改「四套」、新增 2×2 两条轴的表格与「枚举名即存储值、改名必须写迁移」；
目录树、设置页章节、`blobAlphaScale` 段同步。`tasks/lessons.md` 新增 **#77**（枚举改名的存储迁移）、
**#78**（`FlowRow` + `maxItemsInEachRow` 分组换行）。

---

## 第二十一轮（2026-09-30）· 悬浮筛选头改为真·毛玻璃（实时背景模糊）

### 需求

用户给参考图（QQ 音乐截图），红框标注的**顶部悬浮筛选头**要与底部 dock 一样做**实时背景模糊** ——
内容从悬浮栏下方滚过时被真正虚化成色块，而不是只被半透明白纱压淡。

### 实现

| 文件 | 改动 |
|---|---|
| `ui/common/Backdrop.kt` | **重写**：`class Backdrop(val layer: GraphicsLayer)`（`@RequiresApi(S)`）+ `nodeOrigin` + `record(owner, layoutDirection, origin)`；录制走 `layer.record(...) { owner.drawContent() }`，`layer.renderEffect = BlurEffect(28dp, 28dp, TileMode.Clamp)` 设一次；新增 `LocalBackdrop`、`rememberBackdrop()`、`backdropBlurSupported`、`isBackdropRecording` |
| `ui/common/GlassBlurLayer.kt` | **新建**：平移对齐 + 裁剪 +（可选）渐隐遮罩 + 白纱；有遮罩走原生 `Canvas.saveLayer`，**不用 `graphicsLayer`** |
| `ui/common/GlassBackdropSource.kt` | **新建**：`rememberPageBackdrop()`（页内录制器）+ `GlassBackdropSource{…}`（把内容额外录一份，自己不画） |
| `ui/exercise/ExerciseLibraryScreen.kt` | 页面套 `CompositionLocalProvider(LocalBackdrop provides pageBackdrop)`；`GlassBackdropSource(fillMaxSize)` 只包 `LazyColumn`，悬浮头作**兄弟**；悬浮头内改用 `GlassBlurLayer(fadeMask = HeaderFadeMask, overlay = headerScrimBrush())` |
| `ui/food/FoodLibraryScreen.kt` | 同款改造（与动作库逐像素对齐的前提不变） |
| `theme/Color.kt` | `ZiFitPalette` 新增 `headerScrimSolid`（无模糊时的厚纱）：彩色·浅 `0xC0`、中性·浅 `0xC7`、彩色·深 `0xE6131922`（中性·深经 `copy` 继承）；新增 `HeaderScrimSolid` getter |
| `Navigation.kt` | 根录制改 `rememberBackdrop()` + `recorder.record(this, layoutDirection, Offset.Zero)`；`drawWithContent` 内**先录后画** |

### 过程（两个真机坑，各写了一轮）

1. **第一版真机崩溃**：`RenderThread` 栈溢出（tombstone：
   `Cause: stack pointer is not in a rw map; likely due to stack overflow`，512 帧全 `hwui::computeTransformImpl`）。
   根因 = 成环：`根录制层 → NavDisplay 图形层 N → 页内悬浮头（带 `graphicsLayer`）→ 根录制层`。
   修法两步：① `GlassBlurLayer` 去掉 `graphicsLayer`，离屏合成改原生 `Canvas.saveLayer`；
   ② 新增页内录制器，把录制范围落到 N 之下。
2. **第二版不再崩溃，但模糊看不出来** → 用「往录制块里多画一个红方块」的探针判明：
   红块进了模糊层（被糊了），**真内容一个像素都没进去**。根因 = 手写 `RenderNode` +
   偷换 `drawContext.canvas` 这条路，`drawIntoCanvas{}` 有效而 `drawContent()` **不走**那个 canvas。
   改用 `GraphicsLayer.record(...)` 后录得进去。

### 测试 / 门禁（静态部分）

- `./gradlew assembleDebug`：通过。
- `./gradlew lint test`：**BUILD SUCCESSFUL**，单测 **97 项 0 失败**，lint 无 error。
  期间修掉 4 个 NewApi：`backdropBlurSupported` 注解改 `@get:ChecksSdkIntAtLeast`，
  并在 `GlassBackdropSource` / `Navigation.kt` / `drawBackdrop` 补显式 `&& backdropBlurSupported`。
- 真机：**不再崩溃**（`pid` 稳定，进入动作库正常）—— 这一点已在手机上确认。

### ⚠️ 遗留（重要）

**模糊效果是否真正生效，尚未经真机确认。** 用户已放假、手机拿走，
真机验证推迟到**国庆后**。届时的验证步骤：

1. `adb install -r` + `am start`；
2. 进动作库 → 滚动列表 → 截图核对悬浮头**是否真正虚化**（制造高对比参照物；
   模糊类效果靠肉眼不可靠，配合像素均值量测，见 `lessons` 里的取像素法）；
3. 收敛食材库（同款改造是否一致）；
4. 还原手机上的界面偏好。

截屏归档：本轮无（真机未验）。

教训录入 `tasks/lessons.md` **#79**（`RenderEffect` 只作用于自绘像素 / 必须走 `GraphicsLayer.record`）、
**#80**（录制层成环 → RenderThread 栈溢出）、**#81**（`@get:ChecksSdkIntAtLeast`）。
`AGENTS.md` §5.3 新增「实时背景模糊」小节（两个录制器表 + 三条铁律）。
