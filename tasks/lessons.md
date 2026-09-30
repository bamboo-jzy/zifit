# 教训（lessons）

> 每被纠正一次，立即在此追加一条错误模式。会话开始时先复习本文件。

## 环境 / 构建

1. **AGP 拒绝非 ASCII 项目路径** —— 路径含中文或空格直接构建失败，
   `android.overridePathCheck=true` 只屏蔽检查、底层仍失败。故项目根必须是纯 ASCII 路径。
2. **Git Bash 的 PATH 只认 POSIX 路径** —— 写 `D:/...` 无效，必须写 `/d/...`。
3. **`core.autocrlf=true` 会改坏 `gradlew`** —— checkout 时被转成 CRLF 后，
   Git Bash 下 `./gradlew` 报 `bad interpreter`。已由 `.gitattributes` 显式锁定 LF。
4. **`android` CLI 不能并发调用** —— 并发会争抢 `.sdk/refs/remote` 锁导致失败（`sdkmanager` 已废弃）。
5. **「USB 调试在本机不可用」是错判 —— 根因是 adb 不补做枚举，拔插一次就好。**（2026-09-30 更正）
   旧记录写「荣耀 HDB 抢占接口，Windows 从不暴露 ADB 接口」，实测**不成立**：开启「USB 调试」后
   Windows 会枚举出 `USB\VID_339B&PID_107D&MI_02`（驱动 `winusb.inf`、problem code 0），
   `adb devices` / `install -r` / `am start` 全链路可用（`A4YQVB3325001966`）。
   真正的坑是 adb（实测 37.0.0）**不为「自己启动前就已经存在」的 ADB 接口补做枚举** ——
   打开 USB 调试后必须再**拔插一次数据线**、让它收到一次设备到达事件才会识别。
   判定顺序：Windows 是否枚举出 ADB 接口 →（是）用 `D:\tool\_dl\adb_probe.ps1` 发 `CNXN` 看手机回不回 `AUTH`
   →（回）说明是主机侧 adb 没枚举，拔插即可，**别再怀疑线材/驱动**。
   无线调试（`adb pair` + mDNS）降为兜底。
6. **`.workbuddy/` 必须留在 `.gitignore`** —— 内含本地记忆与工作区数据，不得入库。

## 代码

7. **`NavKey` 与 enum 常量同名会冲突** —— 枚举项若与同包内的 `NavKey` 数据对象同名
   （如 `Diet`），构造参数会解析到枚举项自身。枚举常量一律用大写下划线（`DIET`）。
8. **Compose 测试用 `onNodeWithText` 时页面标题不能与标签名重复** ——
   页面标题若与底部标签文案相同，同一文案匹配到多个节点导致断言失败。
   故页面标题用更具体的中文（如「今日饮食」），标签用短词（「饮食」）。
9. **矢量图标路径必须离线校验后再上真机** —— 凭记忆抄 Material 图标的 `pathData`
   极易写出残缺版本，且小尺寸下不易察觉（曾把哑铃画成斜向箭头）。
   校验工具：`D:\tool\_dl\render_svg.py`（支持 M/L/H/V/C/S/Q/T/Z，**逐 `<path>` 独立
   非零填充**，与 Android VectorDrawable 语义一致，可校验贝塞尔曲线）；
   真机小图标用 `D:\tool\_dl\png_crop.py` 放大判读。
   旧的 `icon_check.py` 只支持直线命令，遇曲线路径无解。
10. **`NavKey` 与枚举常量区分大小写** —— `enum class E { DIET(Diet, ...) }` 合法，
    但 `enum class E { Diet(Diet, ...) }` 会解析到枚举项自身而编译失败。
11. **彩色分层 SVG 转 VectorDrawable 时，多个 `<path>` 不可合并** ——
    Android 逐 `<path>` 各自按 fillType 填充；合并成一条后，原设计里
    「轮廓 path 挖的孔 + 填色 path 补的块」会被同一环绕规则重算，形状走样
    （该实心的变镂空、该镂空的变实心）。保持一 path 一元素、统一 `fillColor` 即可。
12. **图标必须在视口内留统一内边距** —— Material 图标在 24 视口内只占约 83%；
    直接移植满幅 SVG（16/16、510/510）会让该图标明显大于同栏其他图标。
    做法：扩大 viewport + `<group android:translateX/Y>` 居中平移，令内容占 85–90%。
    斜向构图（如 45° 哑铃）需取偏大比例补偿视觉缩小 —— 第一版取 83% 即明显偏小。
13. **Material Symbols 的 24px SVG 其 `viewBox` 是 `0 -960 960 960`（y 轴为负）** ——
    VectorDrawable 的 viewport 没有 min-x/min-y 概念，原点固定左上；
    直接照搬会让整幅图形落到视口之外（真机表现为图标消失）。
    必须加 `<group android:translateY="960">` 平移入正坐标区。
    另：该套图标在 960 网格内各字形占比本就不同（58%–88%），
    混用时可先跑 `render_svg.py` 横排比对视觉大小 —— 该脚本已支持
    Android VectorDrawable（含 `viewportWidth` 与 `<group translateX/Y>`）与负起点 viewBox。
14. **`connectedAndroidTest` 跑完会卸载 APK** —— AGP 默认在仪器测试结束后
    卸载 app 与 test APK，表现为"刚验证完、手机上却找不到 App"，容易被误判为
    安装失败或崩溃卸载。项目已在 `gradle.properties` 固定
    `android.injected.androidTest.leaveApksInstalledAfterRun=true`（AGP 9.0.1 的
    `BooleanOption`，默认 `false`；已实测有效：测试后 `com.zifit.app` 仍在）。
    仍须遵守：**验证流程的最后一步恒为 `adb install -r` + `am start`**，
    且用户明确要求测试期间**不要卸载** App（他要上手点按体验）。
15. **`Modifier.graphicsLayer { renderEffect = BlurEffect(...) }` 会模糊该节点及其整棵子树** ——
    因此做 backdrop blur 时**不能**把 `renderEffect` 挂在容器上，否则浮层自己的内容
    （图标、文字）一并被糊掉。实测：dock 里只剩一团白雾，五个图标全消失。
    正确做法：把"背后画面的模糊副本"隔离在一个 `matchParentSize()` 子节点上，
    层级顺序为 **模糊层 → 白纱/描边 → 图标（最上层）**；模糊副本是不透明的屏幕拷贝，
    排在它下面的东西全被盖住。
16. **Compose 的 `BlurEffect` 需 API 31+，且要"背后真有内容"才看得出来** ——
    本项目的背景是平滑径向渐变，直接截屏对比**看不出**模糊与否
    （模糊平滑渐变 ≈ 不模糊），容易被误判为"没生效"。
    可复现的验证方法：临时把内容底部避让改成 `0.dp`，并把页面换成一屏**黑白横向条纹**
    （`repeat(120){ Box(height(20.dp).background(if (i%2==0) Black else White)) }`）——
    条纹穿过 dock 时会从锐利黑/白变成平滑灰阶，而图标依旧清晰，即为"模糊生效且未误伤图标"。
    验完**立即回滚**这两处临时代码（在改动行尾留 `// TEMP-BLUR-TEST` 便于 grep 复查）。
17. **手写的自定义组件必须显式关掉 Material 的默认指示（indication）** ——
    `Modifier.selectable` / `clickable` 在不传 `indication` 时取 `LocalIndication`，
    Material3 会渲染 ripple。手写的 dock 胶囊**没有 `shape`**，涟漪就是**一个灰色矩形**，
    点按后一闪即散 —— 视觉上极廉价。
    修法：`selectable(selected, interactionSource = null, indication = null, role, onClick)`。
    注意必须走这个六参重载；四参重载没有 `indication` 形参，写了也没法关。
18. **"选中态放大"用缩放、不要改 `size`** —— 改 `Modifier.size` 会触发重新布局，
    相邻项被推着抖动；`graphicsLayer { scaleX/scaleY }` 只影响绘制阶段，
    其余元素纹丝不动。缩放值由 `animateFloatAsState` 驱动即得平滑过渡。
    另：同一栏位里各图标**本身**占视口比例可能不同（本项目 58%–88%），
    放大倍数相同时视觉增幅会被放大，取 1.15–1.20 即可，别取 1.3。
19. **`render_svg.py` 只认「两轴同时出现」的 group 平移** —— 旧逻辑用
    `translateX=".."\s+translateY=".."` 一次匹配，只写单轴（如
    `Material Symbols` 的 `viewBox="0 -960 960 960"` 只需 `translateY`）时偏移读成 0，
    图形被画到视口外 → **渲染出全白，误判为「图标写错了」**。已改为两轴各自独立匹配。
    教训：离线校验工具本身出错时会给出**看似确定的错误结论**，遇到「明明该有图形却是空白」
    先怀疑工具而非素材。
20. **Coil 与 Compose BOM 强耦合，加依赖前必须跑 `checkDebugAarMetadata`** ——
    `coil-compose:3.6.3` 传递引入 `androidx.compose.ui:1.12.0`，而它要求
    **AGP ≥ 9.1.0 且 compileSdk ≥ 37**；本项目是 AGP 9.0.1 + compileSdk 36（BOM 给 Compose 1.10.6），
    于是 `checkDebugAarMetadata` 直接 FAILED（`assembleDebug` 之前就挂）。
    处置：降到 `coil-compose:3.3.0` 即可兼容。**不要用 `enforcedPlatform` 硬压版本** ——
    库是针对更高 Compose 编译的，压下来会在运行时 `NoSuchMethodError`。
21. **Compose 测试里，状态提示文案不要复用页面标题** —— 曾把 loading 提示写成
    `stringResource(R.string.exercise_title)`（「动作库」），结果标题与提示各渲染一次，
    `onNodeWithText` 命中 2 个节点，测试报「expected exactly 1」。改用独立的
    `exercise_loading` 文案。**这类失败会被误读成「页面没渲染出来」**，实际是文案撞车。
22. **避让悬浮元素只能用 `contentPadding`,不能用 `padding`** —— 用 `padding(bottom = dock高度)`
    避让悬浮 dock,`padding` 会**缩小可滚动容器**,列表被硬截在 dock 之上、再也滚不进去。
    表现上不是"报错",而是**看起来一切正常、只是那块区域永远空着** ——
    用户称之为「遮挡层」,实际是"内容进不去 + 背景不透明"。
    改法:`NavDisplay` 层级不做底部避让,由各页面的可滚动容器用
    `contentPadding = PaddingValues(bottom = dockContentInset())` 避让;
    对 `verticalScroll` 的 `Column`,padding 必须写在 `verticalScroll` **之后**才属于滚动内容。
    **判断有没有改对,看内容能不能滚进留白区并被 dock 覆盖,而不是看有没有报错。**
23. **悬浮元素不要加投影** —— 内容会从它下面穿过,`Modifier.shadow` 会在内容上压出一条暗带,
    又被读成"遮挡层"。想验证投影是否存在,别只看截图(本项目 18% alpha 的投影**几乎测不出来**:
    采样 dock 上下像素亮度,含投影处与纯背景梯度只差 ≤1/255),而应直接看代码里有没有 `shadow`。
    教训:**"看不出来"不等于"没有"**;反过来,用户抱怨的层次感问题也未必来自他指的那个东西。
24. **悬浮筛选头背后的柔化层要半透明,不能省** —— 一开始只做"列表从搜索框与胶囊下面滚过"
    (头部区域全透明),结果是列表行从筛选胶囊的**缝隙里原样穿出**,文字与文字叠在一起更难看。
    加一层 `HeaderScrim`(半透明白)后,内容依然透得出来但退到背景里去。
    头部用 `Brush.verticalGradient(0f→0.78f 保持,1f→透明)` 做底部淡出,避免留下一条硬边。
25. **"底衬铺满"与"内容内缩"必须分成两层写在同一 Modifier 链上,且底衬在前。** 此前把页面外框
    (安全区 + 16dp)统一套在 `NavDisplay` 上,等于给所有页面一副模具 —— 需要全宽打底的悬浮头
    就永远铺不满:柔化层左右各少 16dp、顶部少一个状态栏,屏幕边缘露出可见直边。
    用户的描述是"太突兀了/像贴上去一块",极易被误读为"颜色太重",于是去调透明度 —— 方向错了。
    **判断法:看边缘。有可见直边 = 是尺寸问题不是颜色问题;边缘干净了才轮到调透明度。**
    正确姿势:外框下沉到各 entry(`Navigation.kt` 的 `screenFrame`),需要铺满的页面刻意不带外框,
    自己按 `fillMaxWidth → background(底衬) → 安全区内缩 → 内容内缩` 排。
26. **`onSizeChanged` 上报的是它右侧(内侧)修饰符撑出来的尺寸** —— 所以"量含 padding 的高度"
    必须把它放在所有 padding **之前**。放在 `windowInsetsPadding` 之后会少算一个状态栏,
    列表少让位、首行被压在搜索框下。这类错不会崩、只会"差一点点",不截图逐像素比对看不出来。
27. **「透明度小一半」= 更不透,不是更透。** 中文里"透明度"与"不透明度"是反的:
    透明度 ↓ = 不透明度 ↑。正确换算是 `α_new = α + (1-α)/2`(透明度减半 = 不透明度向 1 靠拢一半)。
    本项目实际改反过一次,**用户澄清后才改回来** —— 涉及这类词先按字面换算,并在回复里写出
    具体 alpha 值供对方校验,比只描述"更透/更不透"安全得多。
28. **"看起来没变"不等于"没有生效"。** 实测:浅色列表上把 dock 填充 α 从 0.34 提到 0.84,
    区域均值只 +11 亮度(底色本身已 ~232,`232×0.66+255×0.34=240` vs `232×0.16+255×0.84=251`)。
    若靠肉眼判断会误以为没生效、再去翻代码。正确做法:取**不含图标与文字的纯填充带**量区域均值,
    再用合成公式反算比对(吻合到 ±1 即可判定生效)。
29. **⚠️ 用 md5 比对截图判断"点击没生效"会得到假阴性。** 滚动惯性未停、输入法弹出/收起、
    时钟跳分钟,都会让整图变化。要么先让界面完全静置(滑动后 sleep ≥3s)再取基准图,
    要么直接查语义状态(`mInputShown`、节点 `focused`、UI dump 的文本列表)。
    更坑的是**前台应用漂移**:一次 `KEYCODE_BACK` 就退出了应用(根页面的 BACK 会 finish activity),
    后续 tap 全打在了微信/系统设置上,截图看着"没反应"。**每个验证步骤前先确认前台包名**
    (`dumpsys window | grep mCurrentFocus` 应为 `com.zifit.app`),比事后排查省几轮。
30. **悬浮层必须显式吞点击。** `Modifier.background` 只是绘制,不参与命中测试 ——
    浮层下的列表行照样能收到点击,用户点浮层空白处就误进详情页。
    做法 `pointerInput(Unit) { detectTapGestures(onTap = {}) }`,位置**必须在 `padding` 之前**
    (修饰符链上的指针命中区 = 该位置节点的尺寸)。子节点(输入框/胶囊)在命中链更内层、
    先消费事件,因此不会误伤。手写输入控件同理:面板整体 `clickable` + 输入框挂 `FocusRequester`
    + `keyboard?.show()`,否则只有输入框那一条窄缝可点。

31. **占位提示不能与输入框上下堆叠,必须走 `decorationBox` + `matchParentSize()`。**
    `Column { Text(提示); BasicTextField() }` 让空态多出一行,用户看到的就是
    "输入文字和没输入文字的高度不一致"。约定:**高度以输入态为准**。
    不要靠"提示与输入框都用同一个 `bodyMedium`,行高应该一样"来赌 ——
    两者的字体上下留白本就可有差异,`Column` 取的是两者之和/`Box` 取的是最大值,
    只要提示更高一点,空态就还是更高。`matchParentSize()` 让提示**不参与测量**,
    才是唯一稳的写法。验证要量**输入框节点自身**的 `bounds`(空态/输入态应逐像素相同)。

32. **中文"排序"别直接 `sorted()`。** 排的是 Unicode 码位,不是语义顺序。
    本轮 `入门/进阶/高阶` 恰好与码位序一致(入 U+5165 < 进 U+8FDB < 高 U+9AD8),
    纯属巧合 —— 换一组 `新手/熟练/专家`(专 4E13 < 新 65B0 < 熟 719F)立刻变成
    专家→新手→熟练。凡有序词表一律另给一个 `rank`(取枚举/映射的**声明序**),
    并补一条"按声明序而非码位序"的断言。写这类测试时先自己算一遍码位,
    否则断言会写反(本轮把"码位序是错的"当成了前提,实测才发现前提不成立)。

33. **Git Bash + adb 的两个高频坑(无线调试时必踩)。**
    ① 设备侧路径必须加 `MSYS_NO_PATHCONV=1`:`adb shell uiautomator dump /sdcard/d.xml`
    会被 Git Bash 改写成 `C:/Users/.../sdcard/d.xml`,adb 在**本机**找不到该路径而静默失败
    (加了 `>/dev/null` 就完全看不出错);
    ② 同一个开关又会关掉**主机侧**路径转换,于是 `adb install -r /d/projects/...apk`
    变成 adb 无法 `stat` 的路径 —— 主机侧文件路径要写 Windows 形式 `D:/projects/...`。
    结论:**同一次调用里两种写法并存是正常的**,别试图统一。
    另外 `uiautomator dump` 结果用 `exec-out cat` 取回即可,不必 `pull`。

34. **mDNS 会同时广播新旧端口,`head -1` 取到的可能是死端口。**
    实测同一次 `adb mdns services` 列出 `38955`(活)与 `38925`(连不上)。连接逻辑要
    **逐个尝试、取先连上的**,不要只取第一条;连不上时也别急着判断"手机掉线",
    先看是不是拿到了旧端口。附带:`adb shell` 里 `dumpsys window | grep mCurrentFocus`
    要作为**每个验证步骤前的第一句**,否则一次 `KEYCODE_BACK` 退出应用后,
    后续 tap 全打在别的 App 上(教训 29 的复发点)。

35. **`Edit` 工具的 `old_string` 必须覆盖到整行/整个表达式。**
    本轮把 `fun difficulty(key: String?): String = lookup(...).orEmpty()` 只匹配到
    `.orEmpty()` 之前,后缀被留下并**拼到了新插入代码的末尾**,直接编译失败
    (`Unresolved reference 'orEmpty'`)。改动函数签名/带链式调用的单行语句时,
    老串要含行尾。

36. **`Density.toDp()` 只有 `Int` / `Float` / `TextUnit` 三个接收者,`IntSize` 没有。**
    写 `onSizeChanged { headerHeight = with(density) { it.toDp() } }` 直接编译失败
    (`None of the following candidates is applicable`)。要 `it.height.toDp()`
    或 `it.width.toDp()`,别指望它自动挑一个维度。
    正确写法已在 `ExerciseLibraryScreen.kt` 里躺了好几轮,新页面照抄时**别凭记忆简写**。

37. **小数四舍五入不能用 `round(x * 10) / 10`。**
    `formatNutrient(6.85)` 期望 `6.9`,该写法给出 `6.8` —— 因为 `6.85` 的二进制值是
    `6.849999999999999644…`,乘 10 得 `68.4999…`,`round` 后是 `68`。
    要用 `BigDecimal.valueOf(x).setScale(n, RoundingMode.HALF_UP)`:
    `valueOf` 取的是 `Double.toString` 那个「最短能往返的十进制」,也就是用户在输入框里
    敲下的 `6.85`,再按 HALF_UP 才是直觉结果。
    判断"有没有小数部分"用 `stripTrailingZeros().scale() <= 0`,再 `toBigInteger()`;
    **别用 `String.format("%.1f", …)`** —— 它同时还踩 `DefaultLocale`(某些区域小数点是逗号),
    而 `String.format(Locale.ROOT, …)` 又解决不了半进位。

38. **测试夹具直接给 `StateFlow.value` 赋值,会绕过夹具自己的不变量。**
    `FakeFoodDao` 的行为约定是「`rows` 恒为 `createdAt DESC, id DESC` 排序后」,
    但 `dao.rows.value = listOf(旧, 新)` 这种写法**不经过 `sorted()`** ——
    于是「最新在前」这条断言测的是**入参顺序**,不是 DAO 的顺序。
    本次表现为假红(改了断言才发现),反过来也会给出假绿。
    修法:夹具开一个 `setAll(list)` 走 `sorted()`,测试一律用它,别暴露裸 `value` 赋值。

39. **mDNS 查不到端口 ≠ 手机掉线,先看无线调试是不是被关了。**
    实测 `ping` 手机通(2/2 回包、55ms),但 `adb mdns services` 列表为空、
    `:5555` 与 `:5037` 均「积极拒绝」—— 结论是**无线调试被系统关闭**(息屏/重启后自动关),
    与网络、线材、驱动都无关。判定顺序:`ping`(在网?) → `mdns services`(广播中?) →
    端口连通性。三者只有 `mdns` 空时,去手机上重开无线调试,不要在电脑侧反复调 adb。

40. **`adb connect` 报 `failed to connect to <ip>:<port>`,但裸 TCP 能连上 —— 两种病因,别只认一种。**
    无线调试有**两个**端口,都是 5 位随机数、每次重开都会变,肉眼极难分辨:
    主页那行 `IP 地址和端口` = **connect** 端口;点进「使用配对码配对设备」弹窗里的是 **pair** 端口。
    「裸 TCP 通 + `adb connect` 失败」有三种可能,要按序排除:

    | 病因 | 特征 | 处置 |
    |---|---|---|
    | ① 拿到的是 **pair** 端口 | 裸 TCP 通、`adb connect` 失败 | 让用户退回**主页面**重读那行端口 |
    | ② **未配对 / 配对已失效** | 裸 TCP 通、端口确实是主页那行、`connect` 仍失败 | 走 `adb pair <ip>:<pair端口> <配对码>`;**配对弹窗开着时 connect 会被拒**,必须先 pair 再 connect |
    | ③ 沙箱/代理干扰 | Bash 里 `adb connect` 失败但 `bash /dev/tcp` 通 | 先 `unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY` 再 `kill-server`+`start-server` |

    裸连通性测试用 PowerShell `TcpClient.BeginConnect` + `WaitOne(4000)`,或 bash
    `timeout 3 bash -c "exec 3<>/dev/tcp/<ip>/<port>"`。
    附带:`mdns services` 完全为空时 mDNS 这条路是死的,只能靠**手读端口**;
    `zifit_connect <端口>` 与 `verify_food_c.sh <配对端口> <配对码> <连接端口>` 都支持显式端口,绕过 mDNS。

41. **`adb shell` 会吞掉 stdin —— 放进 `while read < file` 循环里,循环只跑一轮。**
    表现:脚本明明遍历了 7 栏,却**只有第一栏被填上**,后面的 `echo` 与 `input text` 全没执行。
    病因:第一次 `adb shell` 就把剩余行从 stdin 读走了,`read` 随即拿到 EOF 退出循环。
    处置:**循环体里所有 adb 调用加 `</dev/null`**;或先把文件读进数组(`mapfile -t`)再 `for`。
    同一类坑适用于所有会读 stdin 的子进程(cat/grep/python - ...)。

42. **`uiautomator dump` 的输出**不包含**输入法窗口 —— 键盘弹着时,点 dock 会静默打在键盘上。**
    本轮现象:点「我的」标签两次都无反应,dump 里没有任何键盘节点,看着像「点击失效」。
    真相:`dumpsys input_method` 显示 `mInputShown=true`,键盘一直没收,后续 tap 全落在键盘上,
    还在搜索框里插进了一个 `.`(dump 里出现 `没有匹配「.」` 才暴露)。
    → **点非输入区之前必须收键盘**:先查 `mInputShown`,为 true 才按 `KEYCODE_BACK`
    (直接 BACK 有风险:键盘没弹时会关掉弹窗/退出应用);收完再复查一次。
    → 另:`uiautomator dump` **不能**用来判断键盘在不在,唯一可靠来源是 `dumpsys input_method`。

43. **Android 16 的 `input text` 对 CJK 直接抛异常,中文无法自动化输入。**
    `adb shell input text "燕麦"` →
    `NullPointerException: Attempt to get length of null array`(`InputShellCommand.sendText`)。
    所以自动化只能填 ASCII;中文录入必须人工在真机上做。
    等价结论:凡是"要输中文"的自动化验证,只验 **ASCII 链路能通 + 落库正确**,
    中文只验**展示**(如资源里预置的中文行、UI 文案),不要假装验过中文输入。

44. **Compose `Dialog` 放不下时,只给内层滚动区 `heightIn(max=…)` 是不够的。**
    真机(2688px / density 3.3)表现:7 栏弹窗的最后一栏「钠」被压成 21dp,
    **并与底部「取消/保存」重叠**。原因是外层 `Column` 没有上界,溢出的部分继续排下去。
    正确做法两层都要:
    ① `GlassSurface(modifier = Modifier.fillMaxWidth().heightIn(max = 可用高 * 0.88f))` 给**整个弹窗**上界;
    ② 字段区 `Modifier.weight(1f, fill = false).verticalScroll(...)`,吃剩余空间且不多占。
    ⚠️ **限高尺寸必须在 `Dialog {}` 之外取** —— `Dialog` 内部 `LocalWindowInfo` /
    `LocalConfiguration` 描述的是**弹窗自己的窗口**(wrap-content),拿不到宿主可用高度。
    ⚠️ 用 `WindowInfo.containerSize.height` 而**不是** `Configuration.screenHeightDp`:
    后者对 insets 的处理随 targetSdk 变化、会四舍五入到整 dp,Compose lint 亦会报
    `ConfigurationScreenWidthHeight`(本轮实测新增一条告警,换掉即清零)。

45. **Compose 里的 dock 项是 `content-desc`,不是 `text`。**
    脚本用 `text="食材库"` 去查会拿到空串,接着 `input tap`(空参)报
    `IllegalArgumentException: Argument expected after "tap"`。
    查坐标的正则要写成 `(?:text|content-desc)="…"`,两者都匹配。

46. **`MSYS_NO_PATHCONV=1` 要作用于 `adb.exe` 进程,写在设备侧命令字符串里等于没写。**
    本轮写成 `adb shell "MSYS_NO_PATHCONV=1 uiautomator dump /sdcard/d.xml"`,
    Git Bash 仍把 `/sdcard/d.xml` 改写掉 → dump 静默失败 → `exec-out cat` 取回
    **109 字节的空 hierarchy**,极易误判成「页面没渲染 / 应用白屏」。
    正确写法:`MSYS_NO_PATHCONV=1 "$ADB" -s "$SERIAL" shell uiautomator dump /sdcard/x.xml`。
    排错提示:`dump` 回来的字节数是个好探针 —— 空 hierarchy ≈ 109 字节,
    正常首页 ≈ 13.5 KB,带弹窗 ≈ 15 KB。

## 第十五轮（卡片宽度对齐 / 移除食部与钠 / AI 接入配置）

47. **`mDNS` 会静默失声：连接端口还活着，`adb mdns services` 却已经空了。**
    表现:`ping` 通、`adb devices` 空、mDNS 列表**一条都没有**;此时拿**上次成功过的端口**直接
    `adb connect` 又能连上(实测 `41275` 一直是活的,只是没在广播)。
    处置:连接脚本写成「mDNS 端口 → **上次成功的端口**(缓存到文件)」依次试,取先连上的。
    ⚠️ 别据此认定手机掉线 —— 只有在**端口也不通**时才去手机上重开无线调试。

48. **`ALTER TABLE … DROP COLUMN` 要 SQLite 3.35+,minSdk 26 只有 3.18。**
    所以 Room 迁移里删列一律走**重表法**:`CREATE 新表 → INSERT … SELECT → DROP 旧表 → RENAME`。
    新表的建表语句必须与 `FoodDatabase_Impl.createAllTables` 里那条**逐字一致**
    (列名 / 类型亲和性 / NOT NULL / 主键都参与校验),否则 Room 打开时会判定 schema 不符。

49. **`uiautomator dump` 里含双引号的文本会被写成单引号属性**(`text='连不上：Unable to
    resolve host "x"'`)。用 `text="…"` 去 grep 会**一条都匹配不到**,极易误判成「界面没显示」。
    解析要写 `text=(?:'([^']*)'|"([^"]*)")` 两种引号都吃。

50. **界面上的「结论」必须和输入绑定:输入一变就作废,包括在途的那一次。**
    真机抓出来的:`example.invalid` 测出「域名解析失败」后,用户把地址改成别的,
    屏幕上那条结论**仍指着旧地址**。修法:ViewModel 里放一个**代次号**,
    `resetTest()` 推进代次,在途结果回来时核对代次,对不上就丢弃(`ProfileViewModelTest` 有断言)。
    同一类问题适用于一切「异步校验 → 展示结论」的界面(保存/测试/搜索均同)。

51. **`@Composable` 不能放进 `joinToString { … }` 的 lambda,但可以放进 `map { … }`。**
    病因:`map` 是 `inline`,`joinToString` 不是 —— 非 inline 的 lambda 不是 Composable 上下文,
    编译期直接报错。要拼一串「栏位名」这类**需要 `stringResource` 的文字**时,
    先 `list.map { label(it) }` 再 `joinToString("、")`。

52. **同一套版式的页面,外框必须共用同一路径,否则卡片宽度会差一截(用户一眼看得出来)。**
    本轮用户反馈「食材库卡片宽度参考动作库」。实测:动作库卡片 `x 53..1171`,
    食材库 `x 106..1118` —— 差 32px。病因是 `Navigation.kt` 给食材库多套了一层
    `screenFrame`(16dp 水平边距),叠加页面自身的 `contentPadding(16.dp)`。
    修法:与动作库一致**不套 `screenFrame`**,安全区与边距由页面自己兜。
    教训:版式「看起来一样」不算数,拿 `uiautomator dump` 量两边的**实际坐标**再下结论。

## 第十六轮（`.env` → debug 包注入 AI 配置）

53. **Gradle 读 `.env` 注入 `BuildConfig`：`buildConfigField` 必须在每个 buildType 里都声明。**
    只写在 `debug {}` 里，读这些字段的代码在 **release 下直接编译不过**（字段不存在）；
    而写进 release 又是"把密钥随 APK 分发"。正确做法：两处**同名同类型**，release 那处值恒为 `""`。
    另两个必踩的点：值的 `trim()`（本机 `core.autocrlf=true`，CRLF 会在值尾挂一个 `\r`，
    拼出来的 URL 变成 `…com\r/models`，报错完全看不懂）；反斜杠与引号要转义，
    否则生成的 `BuildConfig.java` 本身就编译不过。

54. **调试默认值不要写进 prefs，且「清除」必须留一个 `opt_out` 标记。**
    `.env` 的值本来就在 APK 里，抄一份进 `SharedPreferences` 只会让"用户填的"与"构建时注入的"混淆。
    但读取时按优先级现算有个坑：`clear()` 之后下一次读取又把 `.env` 的值捞回来，
    **「清除配置」按了等于没按**。修法：`clear()` 顺手写 `env_opt_out = true`。
    优先级表单独抽成纯函数 `resolveAiConfigSource` 才测得动（4 条断言）。

55. **界面上"看起来一样"的两份配置必须标来源。** 掩码密钥只能看出头尾四位 ——
    用户根本分不清那是自己填的还是构建时注入的。配置卡片加一行 `来源：` 之后一目了然；
    顺带发现隐私说明那句「密钥保存在本机应用私有目录」在 ENV 那一档**是假话**
    （它编在 dex 里），必须换一套说法。**凡是"同一块 UI 承载两种数据来源"，文案都要分档。**

56. **单测不要断言 `.env` 的当前内容。** 那是**本机状态**：本机填了真 key，
    `AiEnvDefaults.isAvailable` 就是 true，写死断言 `false` 的测试当场合规、隔天翻脸。
    要测的是**规则**（`resolveAiConfigSource` 输入输出），不是"本机现在恰好填了没"。

57. **想在真机上验"首启即生效"，不能 `pm clear` —— 那会把食材库一起清掉。**
    只删 AI 配置的正确姿势（debug 包可 `run-as`）：
    `adb shell run-as com.zifit.app rm -f shared_prefs/ai_config.xml`，再 `am force-stop` + 重启。
    另外：**真机自动化输入的 `input text` 打不了这个密钥没关系** ——
    `.env` 注入的路径本来就不需要手输，装完即生效，这恰恰是它存在的意义。

## 第十七轮（「我的」页改版：头像 + 设置 + 个人资料）

58. **悬浮 dock（以及一切浮层）会把页面底部的内容盖住 —— 布局时要用坐标校验，不能靠眼睛。**
    真机实测 1224×2688：dock 上沿在 `y = 2477`。AI 接入页原先把只读的状态卡排在最上面，
    把「保存」和「测试连接」的结论顶到了 `y2478` 以下 → 用户点「测试连接」，
    屏幕上**什么都不会变**（结论生成在看不见的地方），「保存」也点不到。
    修法：① 要点的东西（输入、测试、保存）排前面，只读回显排最后；
    ② 结论**贴在按钮旁边**（同一个 Row、给 `weight(1f)`），不另起一行；
    ③ 验收：`uiautomator dump` 后逐个核对**可点元素**的 `y2 < 2477`。
    ⚠️ 别指望 `BringIntoViewRequester` 救场：滚动容器的视口是整屏（dock 是浮层、
    不在布局里），节点"已经在视口内"，它根本不会滚。

59. **加了新控件之后必须重新量一遍同一屏里的主按钮。** 头像区加一行「移除头像」后，
    「保存」从 `y2348` 掉到 `y2500+`（正好被 dock 吃掉）。改成与「点击头像更换」**同一行**
    才收回来（`y2328..2394`）。教训：页面是**一维**的，任何一处加高都会把下面所有东西推下去 ——
    动布局后要把该页可点元素重新量一遍，别只量改动的那一处。

60. **凡是「上传/选择」类字段，都要给一条回退路径，否则就是把用户关在门外。**
    头像选了以后原本无法撤销（`AvatarPicker` 只能选新的）。补上「移除头像」才闭环：
    草稿里清空路径 → `save()` 里按「旧路径非空且与新路径不同」删掉私有目录里的文件
    （这条既有逻辑刚好覆盖了「清空」这一档，单测补了一条 `save_removingAvatar_deletesPreviousFile`）。

61. **JVM 单测里 `android.net.Uri` 只能当类型用，不能构造**（单测跑的是 android.jar 桩，
    `Uri.parse()` 直接抛 "not mocked"）。所以别把 `Uri` 放在关键逻辑的中途：
    头像的「删旧文件」放在 `save()`（只比较**路径字符串**）就能完整单测；
    真正依赖 `Uri` 的「读图 → 写进私有目录」留给真机验证。**设计的可测性是可以主动安排的。**

62. **要真机验「选图」路径又不想碰用户相册：自己造一张图推进 MediaStore。**
    `adb push D:/.../test.png /sdcard/Pictures/` +
    `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/test.png`
    → 图立刻出现在系统相册选择器里（**最新一张 → 第一格**，坐标好点）。
    验完 `adb shell rm` 删掉再扫一次即可；用户自己的照片一张都不用点。
    ⚠️ 两条路径坑：`MSYS_NO_PATHCONV=1` 时**本地**路径也必须写成 `D:/...`（写 `/d/...` 会
    `cannot stat`）；系统选择器会**缓存**上一次的列表，删/加了图要退出重进才刷新。

63. **时区换算的 bug 在开发机上测不出来 —— 要用「固定常量」把它钉死。**
    Material3 `DatePicker` 的 `selectedDateMillis` 是 **UTC 零点**毫秒。转 `LocalDate` 若图省事用
    `ZoneId.systemDefault()`：在东八区的开发机上，往返测试两个方向同偏 8 小时、恰好还是同一天，
    **测试全绿**；真现形要等一台 UTC-5 的机器（选 6 月 1 日存成 5 月 31 日）。
    所以 `data/user/UserProfile.kt` 的 `toPickerMillis` / `pickerMillisToLocalDate` 钉死 `ZoneOffset.UTC`，
    单测断言**固定毫秒常量**（1998-06-01 = 896659200000，按系统时区算出来差 8 小时，必挂）。
    往返测试只防「两个函数不成对」，防不了「两处一起错」—— 常量才是防线。

64. **「选择器类字段」加了之后，删掉它的旧路径（清除/撤销）要跟着搬，不是跟着删。**
    生日从文本框换成日历选择器后，原来「留空 = 删掉文字」的能力没了 —— 清除动作要放进
    选择器对话框里（且只在已有值时出现，别给一个空态点了没反应的按钮）。
    同理头像：这轮按用户要求删了「移除头像」，那就是**明确接受**了「头像设了去不掉」的取舍 ——
    记下来，别在下轮把按钮擅自加回去。

65. **「设备沉默」类问题，先用一次握手把「手机端」和「主机端」切开，别在硬件上打转。**
    现象：Windows 已枚举出手机 ADB 接口（`winusb.inf`、problem code 0、可 CreateFile），
    但 `adb devices` 恒为空。抓 adb 自己的日志才见真章：
    `ADB_TRACE=all adb -L tcp:5037 nodaemon server` 里 USB 线程只有
    `usb_windows.cpp:171 Created device thread` / `:205 Created power notification thread`，
    **之后一条枚举日志都没有** —— 这就是「主机侧压根没看到接口」的铁证（adbd 失联会是另一种表现）。
    再用几十行 WinUSB P/Invoke 直连该接口：读接口描述符得到 `class=0xFF / sub=0x42 / proto=0x01`
    与两个 bulk 端点（`0x03` OUT / `0x84` IN），发一个 24 字节 `CNXN` 包，手机回 `AUTH`（24 字节）
    → **手机端 adbd 完全正常**，责任瞬间划清，不必再猜线材/驱动/纯净模式。
    ⚠️ 两个 WinUSB 硬性要求（第一次写必踩）：`CreateFile` **必须带 `FILE_FLAG_OVERLAPPED`**，
    否则 `WinUsb_Initialize` 报 err=6；`WinUsb_SetPipePolicy(..., PIPE_TRANSFER_TIMEOUT=0x03, ...)`
    必须先设，否则 `ReadPipe` 永久阻塞。脚本已落 `D:\tool\_dl\adb_probe.ps1`，可直接复用。

66. **仪器测试不许写死「未设置 / 未填写」这类占位文案 —— 它们依赖「手机上没数据」。**
    `ProfileNavigationTest` 里两条用例断言「个人资料首页显示『未设置名称』」「生日显示『未设置』」，
    在开发者自己手机上一旦填过名字就**反过来变红**（实测：真机存了真名「竹子是不秋草」，
    2/6 红）。修法：用生产侧的同一个读出口现算期望值 ——
    `UserProfileRepository.get(context).profile.value.name.ifBlank { text(R.string.profile_name_unset) }`，
    真值优先、空则占位，与界面渲染规则同源，有数据/没数据都绿。
    **不要去清用户的 `SharedPreferences` 来「修」测试** —— 那是删用户真实数据。

    > 2026-09-30 追加：**同一坑在 `BottomNavigationTest#tapProfile_showsHomeHeaderAndBodyCard`
    > 还有一处**，只跑单个测试类时看不见，全量 `connectedAndroidTest` 才暴露（14 个用例里红 1 个）。
    > 教训是**修这类问题时把全仓的断言过一遍，别只修报错那一条** ——
    > `grep -rn "profile_name_unset\|profile_birthday_unset" app/src/androidTest` 一次能捞干净。

67. **`onNode(hasClickAction() and hasAnyDescendant(hasText(x)))` 必须加 `useUnmergedTree = true`。**
    `Modifier.clickable` 会**合并后代语义**：在默认（合并后的）树里，那段文字已经被并进
    行节点自身、不再是它的「后代」，`hasAnyDescendant` 于是一无所获，报错原文会直说
    「However, the unmerged tree contains '1' node that matches」。
    这条与「点这一行而不是点文字」的写法配套：文字节点自己没有点击动作，
    所以只能反过来找「带这段文字后代的**可点**节点」——而它只在未合并树里成立。

68. **设置页的行是「分组卡」，不是「每行一张卡」。**
    原来三行各画一张 `glassRow` 圆角卡，读起来是三个互不相干的胶囊，分组信息全丢。
    参考图（iOS 设置）的骨架是「白底圆角分组 + 组间留白」：`SettingsGroup`
    （= `GlassSurface(contentPadding = 0.dp)`）装若干 `NavRow`，**组内不画分隔线**。
    ⚠️ 两个易错点：① 组内容区必须**零内边距**，否则行文字比页头标题还靠内（默认 18dp 会叠加）；
    ② 行首图标的彩色是**裸字形**，别套彩色方底；同一组内要么都有图标要么都没有，
    混着放文字左边界参差。图标色只做扫视定位，**别照搬参考图的高饱和彩虹**（橙车/绿铃），
    与冷色玻璃底不同族。（色名 2026-09-30 起改为语义名 + 主题化，见 #73：个人资料 `Accent`、
    AI 接入 `IconViolet`、用户界面 `IconCoral`、关于 `IconSky`。）

    > 2026-09-30 追加（第二轮）：这张卡**只剩一张**了 —— 四行并成一张。原先"两张卡"的
    > 分界是「要填的 / 只读的」，但四行其实是**同一类东西**（点进去改一项设置），
    > 给同类项硬分组只会让人去猜分界意味着什么。见 #71。

69. **一个共享偏好有多个消费点 ⇒ 它的 Repository 必须是进程内单例。**
    新增的 `UiPreferencesRepository` 若照抄 `UserProfileRepository`
    「每次 `get()` 都新建实例」的写法，界面偏好会**当场失效**：设置页写的是 A 实例，
    根导航读的是 B 实例，两边各持一份 `StateFlow`，拨了开关要重启 App 才看得到。
    判断口径 —— 问「这份数据此刻被**几处**消费」：
    - 一处、且只在使用时读一次（个人资料：进页面现读）→ 每次新建也能跑；
    - 多处、且要求**即时同步**（界面偏好：设置页写、根导航读）→ 必须单例。
    ⚠️ 仪器测试里**拨动开关后必须拨回原值**：测试跑在开发者本人的手机上，
    改完不还原等于每跑一次就把真实偏好翻一次面（`ProfileNavigationTest#settingsUi_*` 已按此写）。

70. **全站同一种样式只能有「唯一出处」；手写复制一份，必然在某次改动里分叉。**
    用户要求「返回箭头取消背景色、标题字号统一小一点」时，改 `PageHeader` 只覆盖了
    四个二级页 —— 动作详情页**自己手写**过一版（40dp 白圆底返回 + `headlineSmall` 标题），
    当场漏掉，得单独找出来改。
    修法：把那段手写块换成 `PageHeader(titleMaxLines = 2)`（动作名本身就是标题，一行放不下）。
    推论：**样式类需求要落在唯一出处上**；万一某处非手写不可，就在注释里写明它复刻自哪里、
    以及为什么不能复用。副词条：字号阶梯要有语义 —— `titleLarge`(20sp) 专供页头标题，
    `headlineSmall`(24sp) 留给「我的」首页的用户名（页面主角≠页头标题），**别合并成一档**。

71. **分组卡的「分组」要有异质性作依据，否则就只能是一张卡。**
    设置页曾按「要填的（个人资料 / AI 接入）/ 只读的（关于）」分成两张卡；加入
    「用户界面」后四行都是同一类（点进去改一项设置），于是并成一张。
    分组的意义是「**组内同质、组间异质**」—— 分界线要能一句话说出来，说不出来就别分。
    加新入口时先问它是否与既有行**异质**，而不是顺手往第一张卡里追加。

72. **`MSYS_NO_PATHCONV=1` 与 `./gradlew` 不能共存在同一条命令里。**
    真机验证时习惯先 `export MSYS_NO_PATHCONV=1`（防止 adb 把 `/sdcard/ui.xml` 之类
    转成 Windows 路径），但同一条命令后面再跑 `./gradlew` 就会得到：
    ```
    错误: 找不到或无法加载主类 org.gradle.wrapper.GradleWrapperMain
    原因: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
    ```
    原因不在 jar：`gradlew` 把 `$APP_HOME/gradle/wrapper/gradle-wrapper.jar` 交给 java 时，
    **正常要靠 MSYS 把 `/d/projects/...` 转成 `D:\projects\...`**；关掉转换后 java 拿到 POSIX 路径，
    自然找不到类。⚠️ 这条报错与「wrapper jar 真丢了」长得一模一样，
    **先回头看命令里有没有那个 export**，别急着重下 wrapper。
    修法：gradle 单独一条命令跑；adb 那几条用**行内前缀**形式 `MSYS_NO_PATHCONV=1 adb shell ...`。

73. **把「一套静态配色」改成「可切换主题」时，用 `@Composable` 取色 getter 承接旧名字。**
    2026-09-30 加主题模式（彩色 / 浅色 / 深色）+ 主题色时，真正的成本不在调色板 ——
    而在**全站 170+ 处取色点**。做法：把顶层 `val AccentBlue = Color(…)` 换成
    ```kotlin
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalZiFitPalette.current.accent
    ```
    调用点**一行都不用改** —— 与 `MaterialTheme.colorScheme.primary` 是同一个套路。
    ⚠️ 但有两类调用点会编译失败，必须**先 `grep` 出全部引用**再动手，别写到一半才发现：
    - **`Modifier` 扩展函数**（`fun Modifier.glassRow()`）→ 给函数加 `@Composable` 即可
      （调用处本来都在组合里）；
    - **顶层 `val`**（背景光斑表、`ColorScheme` 的构造）→ 改成 `@Composable` 函数，
      或把颜色改成"选择器"（枚举 / lambda）在组合内再解析。
    顺带一条同名陷阱：改名要按语义改（`AccentBlue` → `Accent`），但要**一次性 grep 到位**
    （含 `app/src/androidTest`），漏一处就是编译期红。

74. **深色模式改的是「角色关系」，不是「颜色取反」。**
    三处最容易翻车：① 玻璃填充在深底上要变成**低透明度白**（深底没有"白色"可叠），
    且比浅色档更实一点，否则胶囊边界直接消失；② 光斑要比浅色**更浓**
    （同样的 α 在深底上几乎看不见），色号也要更深沉；③ `HeaderScrim` 这类"底衬"
    必须跟着反相 —— 深色下还在叠白纱，整块会发灰。
    还有一处**不属于配色但必须一起做**：**系统栏图标明暗**。用
    `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark`
    在主题里 `SideEffect` 覆写；只要 `enableEdgeToEdge` 里钉死 `SystemBarStyle.light`，
    深色主题下状态栏那几个字就会消失。
    另：拿 Context 掏 Activity 要**逐层剥 `ContextWrapper`**，直接 `as Activity` 会抛
    `ClassCastException`（Compose 传下来的常是 `ContextThemeWrapper`）。

75. **枚举存进 `SharedPreferences`，读回来必须容错。**
    `valueOf(raw)` 遇到旧版本写的值、或手改坏的值会**在启动那一刻抛异常** ——
    只有装过旧版的用户会踩，一踩就崩在起跑线上，而且复现不了。
    用 `enumValues<T>().firstOrNull { it.name == raw } ?: fallback`。
    同理：删掉的旧键（如 `dock_motion`）不必清理，留在 XML 里无害，但**别再读它**。

76. **主题色一个色号要同时当「前景强调」和「选中填充」，取值必须压在中间调。**
    它既是热量数字/图标的**文字色**，又是筛选胶囊选中时的**背景色**（上面压白字）。
    取太浅 → 胶囊上的白字糊掉；取太深 → 压在深色底上看不清。
    所以**不为深色模式另配一套色号**：另配就意味着取色板上显示的颜色 ≠ 实际用到的颜色，
    而调色板的第一职责是"所见即所得"。
    同理，**不要把所有图标都改成跟随主题色** —— 设置页四个行首图标里只留「个人资料」跟主题色，
    其余三个固定色相；全跟随的话，选到某个色号时几个图标会撞成一片。

77. **枚举名就是 `SharedPreferences` 的存储值：给枚举改名 = 给存储格式做迁移。**
    2026-09-30 把主题模式从三档改四档，`COLORFUL / LIGHT / DARK` 变成
    `COLORFUL_LIGHT / NEUTRAL_LIGHT / COLORFUL_DARK / NEUTRAL_DARK`。
    #75 那条"读回来要容错"（`firstOrNull { it.name == raw } ?: fallback`）在这里**帮不上忙**：
    它只认现名，认不出旧名就**静默回落到默认值** —— 不崩、不报错，只是用户存过的选择
    在升级后被悄悄重置成默认。比崩溃更难发现。
    正确做法是给这个枚举写一个**专门的还原函数**，显式列出旧名到新名的映射
    （`ThemeMode.fromStorage()`），并且**旧名要迁到语义等价的那一档**
    （旧 `DARK` 当年就是"彩色的深色版"→ 迁 `COLORFUL_DARK`，不是 `NEUTRAL_DARK`，
    否则用户升级后看到一张没见过的脸）。
    两条推论：① 通用 `enumOrDefault` 只能在"枚举名从没变过"时用，用之前先想一下它改没改过名；
    ② **改枚举名之前先想清楚要不要改** —— 名字一旦成为存储格式，改名的成本就永久存在了。

78. **可选胶囊的容器用 `FlowRow`，并用 `maxItemsInEachRow` 主动决定每行几个。**
    三档加到四档、标签又变成「彩色·浅」这种四字词之后，写死 `Row` 的表现不是溢出报错，
    而是**最后一个胶囊被卡片裁掉**（用户看到的是"这个选项没了"）。
    换成 `FlowRow` 只是底线；真正的坑是**听天由命地自动换行**：断点取决于字体、字号、机型，
    同一组选项窄屏排成 2+2、宽屏排成 4+0 —— 而这类选项往往自带分组语义
    （主题模式的四档就是"浅组一行、深组一行"）。
    所以给 `ChoiceRow` 加了一个可选的 `maxPerRow`，需要分组时显式传 2。
    配套：仪器测试里**逐个断言每一档都 `assertIsDisplayed()`** ——
    "被裁掉"这件事没有任何报错，只有断言能钉住。

79. **`RenderEffect` 只作用于「本层自己画出来的像素」；内容必须用 `GraphicsLayer.record` 录，
    不能自己开 `RenderNode` 往里画。**
    毛玻璃 dock 的第一版是手写 `RenderNode` + `beginRecording()`，并把
    `owner.drawContext.canvas` 临时换成 node 的录制画布，好让内容落进去。**这条是死路。**
    2026-09-30 用「往录制块里额外画一个红方块」探针判明：模糊层里**有红块**（被糊了），
    **却完全没有真内容** —— 看着像成了，其实录了个空白。
    根因：`drawIntoCanvas { }` 会直接读 `drawContext.canvas`，所以"换画布"对它有效；
    但 **`drawContent()` 不走 `drawContext.canvas`**，它由框架在内部拿着自己的画布去画子树。
    用 `GraphicsLayer.record(owner, layoutDirection, size) { owner.drawContent() }`
    （Compose 官方为此提供的入口，内部做正确的画布切换）才录得进去。
    两条推论：
    - 现象特征是**"位置 / 白纱 / 几何全对，就是背后内容依旧清晰"**（像没开模糊）——
      见到这个症状先怀疑"内容根本没录进去"，不要去调半径、调 α。
    - 模糊半径是在**录制层**上设的（`layer.renderEffect = BlurEffect(r, r, TileMode.Clamp)`，
      设一次即可）。悬浮层只做平移对齐与裁剪采样，**给它加 `Modifier.blur` /
      `graphicsLayer { renderEffect }` 是"什么都没发生"**。

80. **录制层与被录内容互相引用 = 成环 = RenderThread 栈溢出，进程当场被杀。**
    真机 tombstone：`Cause: stack pointer is not in a rw map; likely due to stack overflow`，
    512 帧全是 `hwui::computeTransformImpl`（**不是 OOM、不是 NPE**，日志里没有任何业务栈帧，
    极易被误读成"设备/系统问题"）。
    机制：录制本质是"指向子节点的引用"，HWUI 在 `prepareTree` 时按引用设 `mParent`，
    `A.parent = B` 且 `B.parent = A` 之后算变换就无限递归。当时的环是：
    ```
    根录制层 ─含→ NavDisplay 图形层 N      （N 是被录下来的内容的一部分）
    N       ─含→ 页内悬浮筛选头            （悬浮头在页面里，页面在 N 里）
    悬浮头   ─含→ 采样根录制层             （→ 回到第一行）
    ```
    ⚠️ 三条反直觉之处，缺一个就修不掉：
    - **"录制遍里跳过自己"不解决这个环。** 跳过只是让那一遍画出来是空的，
      但 `graphicsLayer` 节点自身仍会被录进显示列表，引用照样成立。要**同时**做两件事。
    - **悬浮层不能带 `Modifier.graphicsLayer`（含 `clipToBounds`）** —— 那会平白多出一个
      RenderNode 去引用录制层。遮罩的离屏合成改用原生 `Canvas.saveLayer(bounds, paint)`：
      它是 Skia 的图层栈，**不进** HWUI 的 RenderNode 树。
    - 根本解法是**换一个录制范围**，让环不存在：页内悬浮头别用根录制器，改用页内录制器
      （`rememberPageBackdrop()` + `GlassBackdropSource{…}`），录制范围落在 N **之下**。
      推论：**悬浮头必须是录制源的兄弟节点，不能是它的子节点**（是子节点就自己糊自己，还是环）。
    另：`CompositingStrategy.Offscreen` 的 `graphicsLayer` 是同一个坑的另一个入口，也别用。

81. **`@ChecksSdkIntAtLeast` 必须写成 `@get:` 点目标，否则版本守卫等于没写。**
    为了让调用处能写 `if (backdropBlurSupported) …` 而不被 lint 判 NewApi，给
    `val backdropBlurSupported get() = …` 标了 `@ChecksSdkIntAtLeast(api = S)`。
    只写这个注解时，Kotlin 会把它落到**字段**上，而 lint 读的是 **getter** ——
    守卫完全无效，实测连报 **4 个 NewApi**。改成 `@get:ChecksSdkIntAtLeast(...)` 才认。
    ⚠️ 顺带一个语法坑：`@get:` 只能挂在**属性声明**上，写成
    `get() = … @get:ChecksSdkIntAtLeast` 会报
    `'@get:' annotations can only be applied to property declarations`。
    还有一半守卫要靠手写：lint 的流分析**推不出** `LocalBackdrop.current != null` 意味着什么，
    所以调用处仍要**显式**再带上 `&& backdropBlurSupported`。
