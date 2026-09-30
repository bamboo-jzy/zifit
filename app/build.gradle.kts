plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  // Room 的注解处理器走 KSP（AGP 9 内置 Kotlin 编译，不能再挂 kapt）
  alias(libs.plugins.ksp)
}

/**
 * 调试用 AI 配置：从仓库根的 `.env` 读「服务地址 / Key / 模型」，注入 **debug 包**的 BuildConfig，
 * 让 App 一装上就带着这套配置，不用每次手填。见 `.env.example` 与 AGENTS.md §5.6。
 *
 * 三条硬规矩：
 * 1. 只读 `.env`，**不读** `.env.example` —— 后者是占位值，读了会得到一个假 Key；
 * 2. **release 包不注入**：侧载发行的 APK 里不得出现任何密钥（下面显式给空串）；
 * 3. 值只进 BuildConfig，**不进日志**：所以下面那句 lifecycle 只打「齐不齐」，不打内容。
 */
val aiEnv: Map<String, String> =
  rootProject
    .file(".env")
    .takeIf { it.isFile }
    ?.readLines()
    // 逐行 trim：本机 git 是 autocrlf=true，万一文件是 CRLF，值尾部会挂一个 \r，
    // 拼出来的 URL 会带着它去解析（`https://api.deepseek.com\r/models`），报错极难看懂。
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
    ?.associate { it.substringBefore('=').trim() to it.substringAfter('=').trim().trim('"', '\'') }
    ?: emptyMap()

/** 转成 Kotlin/Java 字符串字面量。反斜杠与引号必须转义，否则 buildConfigField 生成的源码编译不过。 */
fun aiEnvLiteral(name: String): String {
  val value = aiEnv[name].orEmpty().replace("\\", "\\\\").replace("\"", "\\\"")
  return "\"$value\""
}

val aiEnvComplete =
  listOf("AI_BASE_URL", "AI_API_KEY", "AI_MODEL").all { !aiEnv[it].isNullOrBlank() }
logger.lifecycle(
  if (aiEnv.isEmpty()) "ZiFit: 没有 .env（调试用 AI 配置不会注入；模板见 .env.example）"
  else if (aiEnvComplete) "ZiFit: 已从 .env 注入调试用 AI 配置（三项齐全）"
  else "ZiFit: 读到了 .env，但三项不齐 —— 缺失项：${listOf("AI_BASE_URL", "AI_API_KEY", "AI_MODEL").filter { aiEnv[it].isNullOrBlank() }.joinToString("、")}"
)

/**
 * release 签名配置。两个来源，**环境变量优先，本机 `keystore.properties` 兜底**：
 *
 * - 本机：`keystore.properties`（已被 .gitignore 忽略）指向**仓库之外**的密钥库；
 * - CI：GitHub Actions 把 Secrets 里的密钥库解到临时路径，再用环境变量传进来。
 *
 * 两边键名一一对应，见 `.github/workflows/release.yml`。
 * ⚠️ 密钥库**绝不入库**：仓库是 public，私钥一旦进了 Git 历史就再也拿不出来。
 */
/**
 * 与上面 [aiEnv] 同一套解析：逐行 trim、跳过空行与 `#` 注释、按第一个 `=` 切分、剥掉包裹引号。
 *
 * 不用 `java.util.Properties` —— 在 Gradle Kotlin DSL 脚本里 `java` 会被解析成别的东西
 * （实测 `Unresolved reference 'util'`），而这个文件本身也没复杂到需要它。
 */
val signingProperties: Map<String, String> =
  rootProject
    .file("keystore.properties")
    .takeIf { it.isFile }
    ?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
    ?.associate { it.substringBefore('=').trim() to it.substringAfter('=').trim().trim('"', '\'') }
    ?: emptyMap()

fun signingValue(envName: String, propKey: String): String? =
  System.getenv(envName)?.takeIf { it.isNotBlank() }
    ?: signingProperties[propKey]?.takeIf { it.isNotBlank() }

val signingStorePath = signingValue("ZIFIT_KEYSTORE_FILE", "storeFile")
val signingStorePassword = signingValue("ZIFIT_STORE_PASSWORD", "storePassword")
val signingKeyAlias = signingValue("ZIFIT_KEY_ALIAS", "keyAlias")
val signingKeyPassword = signingValue("ZIFIT_KEY_PASSWORD", "keyPassword")

// 四项齐备才算"配好"。缺项时**刻意不建 signingConfig**，让产物退回
// `app-release-unsigned.apk`（装不上，一眼能看出），而不是偷偷拿 debug 签名顶上 ——
// 那样出来的包装得上却覆盖不了已有的 release 环境，是最难排查的一类问题。
val signingComplete =
  listOf(signingStorePath, signingStorePassword, signingKeyAlias, signingKeyPassword).all { it != null }

logger.lifecycle(
  when {
    signingComplete && System.getenv("ZIFIT_KEYSTORE_FILE") != null -> "ZiFit: release 签名来自环境变量（CI）"
    signingComplete -> "ZiFit: release 签名来自 keystore.properties（本机）"
    else -> "ZiFit: 未配置 release 签名 —— assembleRelease 将产出 unsigned 包（不用于分发）"
  }
)

// 用 rootProject.file() 解析：绝对路径原样返回，相对路径相对仓库根 —— CI 与本地都能用。
val signingStoreFile = signingStorePath?.let { rootProject.file(it) }

android {
    namespace = "com.zifit.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.zifit.app"
        minSdk = 26
        targetSdk = 36
        /*
         * 版本号可被 `-PzifitVersionName=… -PzifitVersionCode=…` 覆盖，用于 CI 从 tag 反推版本
         * （`.github/workflows/release.yml` 会传）。本地不带参数时用下面这组默认值。
         */
        versionCode = providers.gradleProperty("zifitVersionCode").orNull?.toIntOrNull() ?: 1
        versionName =
          providers.gradleProperty("zifitVersionName").orNull?.takeIf { it.isNotBlank() } ?: "1.0"
    }

    signingConfigs {
        // 只有四项齐备才创建；缺项时 release 构建会落到 unsigned 包（见文件开头的说明）。
        if (signingComplete) {
            create("release") {
                storeFile = signingStoreFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // 调试包才带 .env 的值
            buildConfigField("String", "AI_ENV_BASE_URL", aiEnvLiteral("AI_BASE_URL"))
            buildConfigField("String", "AI_ENV_API_KEY", aiEnvLiteral("AI_API_KEY"))
            buildConfigField("String", "AI_ENV_MODEL", aiEnvLiteral("AI_MODEL"))
        }
        release {
            // ⚠️ 必须与 debug 声明**同名同类型**（否则读这些字段的代码在 release 下编译不过），
            // 但值恒为空 —— release APK 里不得带密钥。这是发布到公开仓库的前提（硬约束 C13）。
            buildConfigField("String", "AI_ENV_BASE_URL", "\"\"")
            buildConfigField("String", "AI_ENV_API_KEY", "\"\"")
            buildConfigField("String", "AI_ENV_MODEL", "\"\"")
            // 签名未配好时保持 null → 产出 app-release-unsigned.apk，让问题在打包阶段就暴露
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      // 打开：`data/ai/AiEnvDefaults` 要读这里注入的 AI_ENV_* 三项
      buildConfig = true
      shaders = false
    }

    // RepDB 的 WebP 本身已是压缩格式，再走 deflate 几乎无收益却拖慢 assets 读取；
    // 声明为不压缩后可走 openFd() 直读，LazyColumn 滚动更稳。
    androidResources {
      noCompress += "webp"
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // 序列化：解析随包携带的 RepDB JSON（serialization 插件已在 plugins {} 中声明）
  implementation(libs.kotlinx.serialization.json)
  // 图片加载：只用于本地 assets 内的 WebP，不引入网络模块
  implementation(libs.coil.compose)

  // 本地持久化：食材库（Room，Apache-2.0）。DAO 的 suspend / Flow 支持在 room-ktx。
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
}

ksp {
  // Room 生成的实现类输出 Kotlin 而非 Java，省掉一层 Java 互操作样板。
  arg("room.generateKotlin", "true")
  // schema 导出目录：`exportSchema = true` 的库（食材库）会把每一版表结构的 JSON 落在这里，
  // 随仓库提交。⚠️ 这是**迁移正确性的唯一凭据**，别把它加进 .gitignore。
  arg("room.schemaLocation", "$projectDir/schemas")
}
