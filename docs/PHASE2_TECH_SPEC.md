# Phase 2 技术规格书 — MVP 产品化

> 状态：已完成
> 日期：2026-04-21
> 分支：`feature/phase-02`（基于 `develop`）

---

## 1. 概述

Phase 2 的目标是在 Phase 1 核心引擎基础上，集成**自动分类引擎**、**AI 学习闭环**、**后台保活**与**隐私合规**，将产品从"能记账"升级为"用户愿意长期使用的 MVP"。

### 1.1 技术决策速览

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 种子规则格式 | **JSON in assets** | 易维护、支持外部贡献、无编译时依赖 |
| 规则匹配策略 | **Exact → Contains 两层** | 简单高效，可解释性强；Embedding 匹配留到 Phase 4 |
| LLM 供应商 | **DeepSeek API** | 成本低、中文理解好、国内可用；通过 `LlmClient` 接口可切换 |
| LLM 规则 confidence 上限 | **0.9** | 明确低于用户手动规则（1.0），保留用户反馈优先权 |
| 配额限制存储 | **DataStore Preferences** | 官方推荐、协程友好、无需自定义数据库表 |
| 前台服务类型 | **specialUse** | Android 14+ 要求；记账数据同步符合 special use 定义 |
| 用户修改分类生成的规则 | **ExactRule, confidence = 1.0** | 用户反馈是最可信信号，应获得最高匹配优先级 |
| 历史回溯分类 | **仅更新 `category == null` 的交易** | 不覆盖用户已手动修改的分类，尊重用户选择 |
| 数据导出格式 | **CSV with UTF-8 BOM** | 最通用格式，Excel 可直接打开中文不乱码 |

### 1.2 新增模块清单

```kotlin
// settings.gradle.kts
include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:common",
    ":feature:capture",
    ":feature:parser",
    ":feature:classification",  // 新增
    ":feature:ledger",
    ":ai"                       // 新增
)
```

---

## 2. 模块依赖图

```
                    :app
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
:feature:capture  :feature:parser  :feature:ledger
    │                │           │         │
    │                │           │         ▼
    │                │           │   :core:database
    │                │           │         │
    └────────────────┼───────────┼─────────┘
                     │           │
                     ▼           ▼
               :feature:classification
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
      :ai                     :core:database
        │                         │
        └────────────┬────────────┘
                     │
                     ▼
               :core:model
                     │
                     ▼
               :core:common
```

**依赖规则：**
- `:feature:classification` 依赖 `:core:database`（读写规则）、`:ai`（LLM 学习）
- `:ai` 仅依赖 `:core:model`（`CategoryRule` 类型），保持最小耦合
- `:feature:capture` 新增依赖 `:feature:classification`（入库时分类）
- `:feature:ledger` 新增依赖 `:feature:capture`（保活状态检测组件）、`:feature:classification`（详情页修改分类生成规则）
- 所有依赖单向，无循环

---

## 3. 数据库变更 — Migration 1→2

### 3.1 新增 `matchType` 列

Phase 2 需要在 `category_rules` 表中支持两种匹配方式：`EXACT` 和 `CONTAINS`。

```kotlin
// core/database/src/main/java/.../migration/Migration_1_2.kt
val Migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE category_rules ADD COLUMN matchType TEXT NOT NULL DEFAULT 'CONTAINS'"
        )
    }
}
```

### 3.2 模型变更

```kotlin
// core/model/.../CategoryRule.kt
data class CategoryRule(
    val id: Long = 0,
    val keyword: String,
    val category: String,
    val confidence: Float = 1.0f,
    val matchType: MatchType = MatchType.CONTAINS
)

enum class MatchType { EXACT, CONTAINS }
```

### 3.3 Repository 新增方法

```kotlin
interface CategoryRuleRepository {
    // ... 已有方法
    suspend fun count(): Int
    suspend fun deleteAll()
    suspend fun hasRuleCovering(merchant: String): Boolean
}

interface TransactionRepository {
    // ... 已有方法
    suspend fun getUnclassifiedByMerchantKeyword(keyword: String): List<Transaction>
    suspend fun getAll(): List<Transaction>
    suspend fun deleteAll()
}

interface ParseFailureRepository {
    // ... 已有方法
    suspend fun deleteAll()
}
```

---

## 4. `:feature:classification` — 分类引擎

### 4.1 种子规则库 `seed_rules.json`

位于 `feature/classification/src/main/assets/seed_rules.json`，共 **140 条**常见商户规则，覆盖 9 大分类：

```json
[
  {"keyword": "星巴克", "category": "饮品", "confidence": 1.0, "matchType": "exact"},
  {"keyword": "滴滴", "category": "交通", "confidence": 0.9, "matchType": "contains"},
  {"keyword": "淘宝", "category": "购物", "confidence": 0.8, "matchType": "contains"}
]
```

**加载时机**：应用首次启动（`JizhangApplication.onCreate()`），异步加载不阻塞冷启动。

```kotlin
class SeedRuleLoader(
    private val context: Context,
    private val ruleRepository: CategoryRuleRepository
) {
    suspend fun loadIfEmpty() {
        if (ruleRepository.count() > 0) return
        val rules = parseSeedRules()
        rules.forEach { ruleRepository.insert(it) }
    }
}
```

### 4.2 三层分类引擎 `ClassificationEngine`

```kotlin
class ClassificationEngine(private val ruleRepository: CategoryRuleRepository) {
    suspend fun classify(merchant: String): ClassificationResult {
        val allRules = ruleRepository.getAllRules()

        // Layer 1: Exact Match（用户手动修正，confidence = 1.0）
        allRules
            .filter { it.matchType == MatchType.EXACT }
            .firstOrNull { merchant == it.keyword }
            ?.let { return ClassificationResult.Classified(it.category, it.confidence) }

        // Layer 2: Contains Match（种子规则/LLM 规则，按 confidence 降序）
        allRules
            .filter { it.matchType == MatchType.CONTAINS }
            .sortedByDescending { it.confidence }
            .firstOrNull { merchant.contains(it.keyword) }
            ?.let { return ClassificationResult.Classified(it.category, it.confidence) }

        // Layer 3: Unclassified → 触发 LLM 学习
        return ClassificationResult.Unclassified
    }
}
```

```kotlin
sealed class ClassificationResult {
    data class Classified(val category: String, val confidence: Float) : ClassificationResult()
    data object Unclassified : ClassificationResult()
}
```

### 4.3 LLM 学习闭环 `LlmLearningUseCase`

当分类引擎返回 `Unclassified` 时，入库流程异步调用 `LlmLearningUseCase.learnForMerchant(merchant)`：

```kotlin
class LlmLearningUseCase(
    private val llmClient: LlmClient,
    private val ruleRepository: CategoryRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val quotaLimiter: DailyQuotaLimiter
) {
    suspend fun learnForMerchant(merchant: String): Boolean {
        // 1. 配额检查
        if (!quotaLimiter.canCall()) {
            LearningQueue.enqueue(merchant)
            return false
        }

        // 2. 并发防护：再次检查是否已有规则覆盖
        if (ruleRepository.hasRuleCovering(merchant)) return true

        // 3. 调用 LLM
        val result = try {
            llmClient.classify(merchant)
        } catch (e: Exception) {
            return false
        }

        quotaLimiter.recordCall()

        // 4. 提取关键词并持久化规则
        val keyword = extractKeyword(result.ruleKeyword, merchant)
        val rule = CategoryRule(
            keyword = keyword,
            category = result.category,
            confidence = result.confidence.coerceAtMost(0.9f),
            matchType = MatchType.CONTAINS
        )
        ruleRepository.insert(rule)

        // 5. 回溯历史未分类交易
        backfillUnclassifiedTransactions(rule)

        return true
    }
}
```

**关键词提取策略**：
- `"merchant contains 星巴克"` → `星巴克`
- `"merchant == 麦当劳"` → `麦当劳`
- 解析失败则回退到原始商户名

**回溯策略**：仅更新 `category == null` 的历史交易，不覆盖用户手动修改的分类。

### 4.4 日配额限制 `DailyQuotaLimiter`

使用 `androidx.datastore:datastore-preferences` 持久化配额状态，防止冷启动期费用超支。

```kotlin
class DailyQuotaLimiter(
    private val dataStore: DataStore<Preferences>,
    private val maxCallsPerDay: Int = 10
) {
    suspend fun canCall(): Boolean
    suspend fun recordCall()
    suspend fun remainingCalls(): Int
}
```

- 默认日上限：**10 次**
- 跨天自动重置：比较 `LocalDate.now()` 与上次调用日期

### 4.5 学习队列 `LearningQueue`

配额耗尽时的内存暂存队列：

```kotlin
object LearningQueue {
    private val queue = mutableSetOf<String>()
    fun enqueue(merchant: String)
    fun dequeueAll(): List<String>
}
```

> 进程被杀后队列丢失是可接受的——下次该商户出现仍会触发学习。

---

## 5. `:ai` — LLM 模块

### 5.1 接口设计

```kotlin
interface LlmClient {
    suspend fun classify(merchant: String): LlmClassificationResult
}

data class LlmClassificationResult(
    val category: String,
    val ruleKeyword: String,
    val confidence: Float
)

class LlmException(message: String) : Exception(message)
```

`LlmClient` 为接口，便于未来切换供应商（OpenAI、智谱 GLM 等）。

### 5.2 DeepSeek 实现 `DeepSeekLlmClient`

```kotlin
class DeepSeekLlmClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com"
) : LlmClient {

    override suspend fun classify(merchant: String): LlmClassificationResult {
        val response = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(
                ChatCompletionRequest(
                    model = "deepseek-chat",
                    messages = listOf(
                        Message("system", LlmPrompts.SYSTEM),
                        Message("user", LlmPrompts.user(merchant))
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )
            )
        }

        if (!response.status.isSuccess()) {
            throw LlmException("LLM API error: ${response.status}")
        }

        val jsonText = response.bodyAsText()
        val body = Json.decodeFromString<ChatCompletionResponse>(jsonText)
        val content = body.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Empty response from LLM")

        return LlmResponseParser.parse(content)
    }
}
```

**关键设计**：使用 `bodyAsText()` + 手动 `Json.decodeFromString`，避免 Ktor ContentNegotiation 在嵌套 JSON 解析中的兼容性问题。

### 5.3 Prompt 设计 `LlmPrompts`

```kotlin
object LlmPrompts {
    val SYSTEM = """
        You are a payment merchant classifier for a Chinese personal bookkeeping app.
        Given a merchant name, classify it into ONE category.
        Available categories: 餐饮, 饮品, 交通, 购物, 娱乐, 日用, 医疗, 教育, 其他.
        Output JSON only with keys: "category", "rule", "confidence".
        The "rule" field should be a simple keyword that can be used for string matching
        (e.g., "merchant contains 星巴克" or "merchant == 麦当劳").
        Confidence must be between 0.0 and 1.0.
    """.trimIndent()

    fun user(merchant: String) = "Merchant: $merchant"
}
```

### 5.4 响应解析 `LlmResponseParser`

```kotlin
object LlmResponseParser {
    fun parse(jsonString: String): LlmClassificationResult {
        val json = Json { ignoreUnknownKeys = true }
        val raw = json.decodeFromString<LlmRawResponse>(jsonString)

        val category = raw.category?.takeIf { it.isNotBlank() }
            ?: throw LlmException("Missing or empty category")
        val rule = raw.rule?.takeIf { it.isNotBlank() }
            ?: throw LlmException("Missing or empty rule")
        val confidence = raw.confidence?.coerceIn(0.0f, 1.0f) ?: 0.8f

        return LlmClassificationResult(category, rule, confidence)
    }
}
```

---

## 6. 分类引擎集成到入库流程

`PersistCapturedTransactionUseCase` 在入库时调用分类引擎，并在未分类时触发 LLM 学习：

```kotlin
class PersistCapturedTransactionUseCase(
    private val parser: TransactionParser = TransactionParser,
    private val transactionRepository: TransactionRepository,
    private val parseFailureRepository: ParseFailureRepository,
    private val classificationEngine: ClassificationEngine,
    private val llmLearningUseCase: LlmLearningUseCase
) {
    suspend operator fun invoke(notification: NotificationData) {
        val parsed = parser.parse(notification)
        if (parsed.amount != null && parsed.merchant != null && parsed.isPayment) {
            val classification = classificationEngine.classify(parsed.merchant)
            val category = (classification as? ClassificationResult.Classified)?.category

            transactionRepository.insert(
                Transaction(
                    amount = parsed.amount.toDouble(),
                    merchant = parsed.merchant,
                    category = category,
                    timestamp = notification.timestamp,
                    sourceApp = notification.packageName,
                    rawText = notification.text ?: notification.bigText ?: ""
                )
            )

            // 未分类时触发异步 LLM 学习
            if (classification == ClassificationResult.Unclassified) {
                llmLearningUseCase.learnForMerchant(parsed.merchant)
            }
        } else {
            // 解析失败记录
            parseFailureRepository.insert(...)
        }
    }
}
```

---

## 7. 用户修改分类自动生成规则

在 `TransactionDetailViewModel.save()` 中，当用户修改分类时自动生成 `ExactRule`：

```kotlin
fun save(merchant: String, category: String) {
    viewModelScope.launch {
        val current = transactionRepository.getById(transactionId)
        if (current != null) {
            if (current.category != category) {
                val userRule = CategoryRule(
                    keyword = merchant,
                    category = category,
                    confidence = 1.0f,
                    matchType = MatchType.EXACT
                )
                categoryRuleRepository.insert(userRule)
            }
            transactionRepository.update(current.copy(merchant = merchant, category = category))
        }
    }
}
```

**设计理由**：用户手动修改分类代表"我对这个商户的分类判断比 AI 更准"，因此生成 `ExactRule`（confidence = 1.0），在分类引擎中享有最高优先级。

---

## 8. 前台保活服务 `KeepAliveService`

### 8.1 服务实现

```kotlin
class KeepAliveService : Service() {
    override fun onCreate() {
        super.onCreate()
        val notification = KeepAliveNotificationHelper.create(this)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY  // 被杀后系统尝试重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context)
        fun stop(context: Context)
        fun isRunning(context: Context): Boolean
    }
}
```

### 8.2 通知设计

```kotlin
object KeepAliveNotificationHelper {
    fun create(context: Context, recordedCount: Int = 0): Notification {
        // 创建 IMPORTANCE_LOW 通知渠道
        // 标题："自动记账运行中"
        // 点击跳转到 MainActivity
        // setOngoing(true) 防止用户滑动删除
    }
}
```

### 8.3 AndroidManifest 声明

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<service
    android:name="...KeepAliveService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />
```

> Android 14+ 需要 `FOREGROUND_SERVICE_SPECIAL_USE` 权限并声明 `foregroundServiceType="specialUse"`。

### 8.4 启动时机

- `MainActivity.onCreate()`：若服务未运行则启动
- `MainActivity.onResume()`：再次检测并自动复活

---

## 9. 首次启动引导页

### 9.1 三页引导结构

1. **欢迎页**
   - 标题："自动记账，无需手动"
   - 副标题："捕获微信/支付宝通知，自动分类入账"
   - 按钮："开始使用"

2. **通知权限页**
   - 标题："开启通知使用权"
   - 说明："我们需要监听支付通知来自动记账。您的通知内容仅保存在本地，不会上传。"
   - 按钮："去开启" → `ACTION_NOTIFICATION_LISTENER_SETTINGS`
   - 按钮："下一步"

3. **电池优化页**
   - 标题："保持后台运行"
   - 说明："为防止系统杀死记账服务，请将本应用加入电池优化白名单。"
   - 按钮："去设置" → `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
   - 按钮："完成" → 标记 onboarding 完成

### 9.2 持久化

```kotlin
class OnboardingPreferences(private val dataStore: DataStore<Preferences>) {
    val onboardingCompleted: Flow<Boolean> = ...
    suspend fun setCompleted()
}
```

首次完成后不再显示，用户重装应用会再次触发。

---

## 10. 保活状态检测与复活提示

### 10.1 `ServiceAliveChecker`

```kotlin
class ServiceAliveChecker(private val context: Context) {
    fun isNotificationServiceEnabled(): Boolean
    fun isBatteryOptimizationIgnored(): Boolean
    fun isKeepAliveServiceRunning(): Boolean
    fun getMissingPermissions(): List<MissingPermission>

    enum class MissingPermission {
        NOTIFICATION_LISTENER,
        BATTERY_OPTIMIZATION,
        FOREGROUND_SERVICE
    }
}
```

### 10.2 `MainActivity` 集成

在 `MainActivity.onResume()` 中调用检测，结果通过 `StateFlow` 驱动 Compose UI：

```kotlin
override fun onResume() {
    super.onResume()
    if (!KeepAliveService.isRunning(this)) {
        KeepAliveService.start(this)
    }
    missingPermissionsFlow.value = serviceAliveChecker.getMissingPermissions()
}
```

### 10.3 `ServiceStatusBanner`

Compose 组件，根据缺失权限显示不同颜色提示：
- **红色 Banner**（错误级）：通知权限关闭 / 前台服务未运行 → 可点击跳转设置
- **黄色 Banner**（警告级）：电池优化未忽略 → 建议处理

---

## 11. 隐私政策与数据管理

### 11.1 隐私政策文档

`docs/privacy_policy.md` 包含：
- 数据仅本地存储声明
- AI 分类仅上传商户名称（不含金额/时间/身份）
- 数据导出与删除说明
- 权限用途说明

### 11.2 数据导出 `DataExportUseCase`

```kotlin
class DataExportUseCase(private val transactionRepository: TransactionRepository, private val context: Context) {
    suspend fun exportToCsv(): File {
        val transactions = transactionRepository.getAll()
        val file = File(context.getExternalFilesDir(DIRECTORY_DOCUMENTS), "jizhang_export_${timestamp}.csv")
        file.bufferedWriter().use { writer ->
            writer.write("\uFEFF") // BOM for Excel UTF-8
            writer.write("ID,金额,商户,分类,时间,来源应用,原始文本\n")
            transactions.forEach { ... }
        }
        return file
    }
}
```

### 11.3 数据清除 `ClearDataUseCase`

```kotlin
class ClearDataUseCase(...) {
    suspend fun clearAllTransactions()
    suspend fun clearAllRules()
    suspend fun clearAll() // 全部清除含失败日志
}
```

### 11.4 UI 界面 `PrivacySettingsScreen`

Compose 界面包含：
- 隐私政策文本（Scrollable）
- "导出数据"按钮 → 生成 CSV → 弹出分享 Intent
- "清除所有数据"按钮 → 二次确认对话框 → 执行清除

---

## 12. Koin DI 模块汇总

```kotlin
// app/src/main/java/.../di/AppModule.kt
val appModules = listOf(
    databaseModule,
    parserModule,
    captureModule,
    ledgerModule,
    classificationModule,
    aiModule
)
```

### 12.1 `classificationModule`

```kotlin
val classificationModule = module {
    single<DataStore<Preferences>> { PreferenceDataStoreFactory.create { ... } }
    single { ClassificationEngine(get()) }
    single { SeedRuleLoader(get(), get()) }
    single { DailyQuotaLimiter(get()) }
    single { LlmLearningUseCase(get(), get(), get(), get()) }
}
```

### 12.2 `aiModule`

```kotlin
val aiModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single<LlmClient> {
        DeepSeekLlmClient(
            httpClient = get(),
            apiKey = getProperty("deepseek_api_key", "")
        )
    }
}
```

### 12.3 `captureModule` 变更

```kotlin
val captureModule = module {
    single { NotificationDeduplicator() }
    single { CaptureNotificationHandler(get(), get()) }
    single { PersistCapturedTransactionUseCase(get(), get(), get(), get(), get()) }
}
```

### 12.4 `ledgerModule` 变更

```kotlin
val ledgerModule = module {
    single { LedgerReducer() }
    single { OnboardingPreferences(get()) }
    single { DataExportUseCase(get(), get()) }
    single { ClearDataUseCase(get(), get(), get()) }
    viewModel { LedgerViewModel(get(), get()) }
    viewModel { (transactionId: Long) -> TransactionDetailViewModel(transactionId, get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}
```

---

## 13. 测试策略与覆盖率

### 13.1 `:feature:classification` 测试

| 测试类 | 覆盖场景 |
|--------|----------|
| `ClassificationEngineTest` | Exact Match、Contains Match、无匹配、confidence 排序 |
| `SeedRuleLoaderTest` | 数据库为空时加载、已有规则时跳过 |
| `DailyQuotaLimiterTest` | 首次可用、达到上限、跨天重置、剩余次数 |
| `LlmLearningUseCaseTest` | 正常学习、配额耗尽、已有规则、LLM 异常、confidence 上限 0.9 |

### 13.2 `:ai` 测试

| 测试类 | 覆盖场景 |
|--------|----------|
| `LlmResponseParserTest` | 完整 JSON、缺 confidence、额外字段、非法 JSON |
| `DeepSeekLlmClientTest` | 正常响应、空 choices、HTTP 错误状态码 |

### 13.3 `:feature:capture` 测试

| 测试类 | 覆盖场景 |
|--------|----------|
| `PersistCapturedTransactionUseCaseTest` | 已分类商户含 category、未分类商户 category 为 null、未分类触发 LLM 学习、已分类不触发 |

### 13.4 `:feature:ledger` 测试

| 测试类 | 覆盖场景 |
|--------|----------|
| `TransactionDetailViewModelTest` | 修改分类生成 ExactRule（confidence=1.0）、未修改不生成规则 |
| `DataExportUseCaseTest` | CSV 文件创建、BOM 头、数据行正确性 |

---

## 14. Issue Log

### Issue 1: Ktor MockEngine JSON 解析失败

**现象**：`DeepSeekLlmClientTest` 中 `response.body()` 抛出 `JsonDecodingException` / `MissingFieldException`。

**根因**：
1. `respond` content 字符串嵌套 JSON 转义问题
2. Ktor ContentNegotiation 的 `body()` 自动反序列化与嵌套 JSON 字符串（`content` 字段内是转义后的 JSON）不匹配

**解决**：
- 改用 `response.bodyAsText()` + 手动 `Json.decodeFromString<ChatCompletionResponse>()`
- 测试中使用字符串拼接 `"...\"category\"..."` 精确控制转义

### Issue 2: `koin-android` 无法用于纯 JVM 模块

**现象**：`:ai` 模块编译报错，AAR 无法用于纯 JVM 库。

**解决**：
- `gradle/libs.versions.toml` 新增 `koin-core`
- `:ai/build.gradle.kts` 使用 `implementation(libs.koin.core)` 替代 `koin-android`

### Issue 3: JUnit 4 Robolectric 测试不被 JUnit Platform 发现

**现象**：`DailyQuotaLimiterTest`、`SeedRuleLoaderTest` 使用 `@RunWith(RobolectricTestRunner::class)` 但测试未被运行。

**解决**：
- `:feature:classification/build.gradle.kts` 添加 `testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")`
- `tasks.withType<Test> { useJUnitPlatform() }` 保持统一

### Issue 4: `LlmPrompts.SYSTEM` const val 编译错误

**现象**：`const val SYSTEM = """.trimIndent()` 报错，`trimIndent()` 不是编译时常量。

**解决**：移除 `const` 关键字，`val SYSTEM = ...`。

### Issue 5: `:app` 模块未声明 `:feature:classification` 和 `:ai` 依赖

**现象**：`./gradlew :app:assembleDebug` 编译报错，`Unresolved reference` for `classificationModule` / `aiModule`。

**解决**：`app/build.gradle.kts` 添加：
```kotlin
implementation(project(":feature:classification"))
implementation(project(":ai"))
```

---

## 15. 关键文件清单

### 配置
- `settings.gradle.kts`（新增 `:feature:classification`, `:ai`）
- `gradle/libs.versions.toml`（新增 Ktor、DataStore 依赖）
- `app/src/main/AndroidManifest.xml`（新增 FOREGROUND_SERVICE 权限和 KeepAliveService 声明）

### `:core:database`（变更）
- `core/database/.../migration/Migration_1_2.kt`
- `core/database/.../entity/CategoryRuleEntity.kt`（新增 `matchType` 列）
- `core/database/.../dao/CategoryRuleDao.kt`（新增 `count()`、`deleteAll()`）
- `core/database/.../dao/TransactionDao.kt`（新增 `getUnclassifiedByMerchantKeyword()`、`getAll()`、`deleteAll()`）
- `core/database/.../repository/CategoryRuleRepository.kt` + `Impl.kt`
- `core/database/.../repository/TransactionRepository.kt` + `Impl.kt`

### `:feature:classification`
- `feature/classification/src/main/assets/seed_rules.json`
- `feature/classification/.../seed/SeedRuleLoader.kt`
- `feature/classification/.../ClassificationEngine.kt`
- `feature/classification/.../ClassificationResult.kt`
- `feature/classification/.../learn/LlmLearningUseCase.kt`
- `feature/classification/.../learn/LearningQueue.kt`
- `feature/classification/.../quota/DailyQuotaLimiter.kt`
- `feature/classification/.../di/ClassificationModule.kt`

### `:ai`
- `ai/.../llm/LlmClient.kt`
- `ai/.../llm/DeepSeekLlmClient.kt`
- `ai/.../prompt/LlmPrompts.kt`
- `ai/.../parser/LlmResponseParser.kt`
- `ai/.../di/AiModule.kt`

### `:feature:capture`（变更 + 新增）
- `feature/capture/.../usecase/PersistCapturedTransactionUseCase.kt`（新增分类引擎 + LLM 学习注入）
- `feature/capture/.../keepalive/KeepAliveService.kt`
- `feature/capture/.../keepalive/KeepAliveNotificationHelper.kt`
- `feature/capture/.../keepalive/ServiceAliveChecker.kt`
- `feature/capture/src/main/res/drawable/ic_notification.xml`

### `:feature:ledger`（变更 + 新增）
- `feature/ledger/.../detail/TransactionDetailViewModel.kt`（用户修改分类生成规则）
- `feature/ledger/.../onboarding/OnboardingScreen.kt`
- `feature/ledger/.../onboarding/OnboardingViewModel.kt`
- `feature/ledger/.../onboarding/OnboardingPreferences.kt`
- `feature/ledger/.../settings/PrivacySettingsScreen.kt`
- `feature/ledger/.../settings/DataExportUseCase.kt`
- `feature/ledger/.../settings/ClearDataUseCase.kt`
- `feature/ledger/.../di/LedgerModule.kt`

### `:app`（变更）
- `app/.../MainActivity.kt`（集成 onboarding、保活启动、服务状态检测）
- `app/.../di/AppModule.kt`（新增 classificationModule、aiModule）

### 文档
- `docs/privacy_policy.md`

---

## 16. 待后续优化项（Phase 3+）

1. **Embedding 匹配**：当前仅 Exact/Contains 两层，未来可引入向量相似度匹配（如 `all-MiniLM`）处理变体商户名。
2. **LLM 供应商切换 UI**：当前通过 `getProperty("deepseek_api_key")` 注入，未来可在设置页提供供应商选择和 API Key 输入。
3. **学习队列持久化**：当前 `LearningQueue` 为内存队列，未来可改为数据库表 `PendingLearningQueue`，支持跨进程恢复。
4. **通知计数动态更新**：`KeepAliveNotificationHelper` 当前使用静态 `recordedCount`，未来可通过 Flow 监听数据库实现动态更新。
5. **批量导出分享**：`PrivacySettingsScreen` 当前仅生成文件，未来可添加 Android Sharesheet 直接分享 CSV。
