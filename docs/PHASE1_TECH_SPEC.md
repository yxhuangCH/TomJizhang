# Phase 1 技术规格书 — 核心引擎

> 状态：已完成  
> 日期：2026-04-19  
> 分支：`feature/phase-02`（基于 `develop`）

---

## 1. 概述

Phase 1 的目标是在 PoC 验证的基础上，搭建完整的模块化 Clean Architecture，实现「捕获通知 → 解析交易 → 持久化存储 → 列表/详情展示」的完整闭环。

### 1.1 技术决策速览

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 模块架构 | Clean Modular (`:core:*` + `:feature:*`) | 隔离职责，便于 Phase 2 引入 `:feature:classification` 和 `:ai` |
| DI 框架 | Koin 3.5.6 | 轻量、模块化友好、无需 kapt/ksp 注解处理 |
| 数据库 | Room 2.6.1 + KSP | 成熟生态，原生支持 `Flow` 查询 |
| 不可变集合 | `kotlinx.collections.immutable` | 与 Compose 配合最佳，`==` 比较即结构比对 |
| UI 架构 | Compose + Reducer + StateFlow | 单向数据流，重组可控 |
| 测试 | JUnit 5 (Jupiter) + MockK + Turbine | 新代码统一 Jupiter；Room/Context 测试用 Robolectric |

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
    ":feature:ledger"
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
    │                │                │
    │                │                ▼
    │                │         :core:database
    │                │                │
    └────────────────┼────────────────┘
                     │
                     ▼
               :core:model
                     │
                     ▼
               :core:common
```

**依赖规则：**
- `feature` → `core`（单向）
- `feature:capture` → `feature:parser`（解析器作为输入处理的一部分）
- `app` → 所有模块（Application 入口和 Koin 聚合）
- `:core:database` → `:core:model`（Repository 返回领域模型）

---

## 3. `:core:model` — 领域模型

纯 Kotlin 数据类，无任何 Android 依赖。

### 3.1 Transaction

```kotlin
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String?,
    val timestamp: Long,
    val sourceApp: String,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

- `category` 初始可为 `null`，Phase 2 由分类引擎填充。
- `rawText` 保存原始通知文本，用于后续 Parser 回归测试。

### 3.2 CategoryRule（Phase 2 前置）

```kotlin
data class CategoryRule(
    val id: Long = 0,
    val keyword: String,
    val category: String,
    val confidence: Float = 1.0f
)
```

### 3.3 ParseFailureLog

```kotlin
data class ParseFailureLog(
    val id: Long = 0,
    val rawText: String,
    val sourceApp: String,
    val timestamp: Long,
    val reason: String
)
```

---

## 4. `:core:database` — Room 数据库层

### 4.1 实体定义

| 实体 | 表名 | 说明 |
|------|------|------|
| `TransactionEntity` | `transactions` | 交易记录 |
| `CategoryRuleEntity` | `category_rules` | 分类规则（Phase 2 使用） |
| `ParseFailureLogEntity` | `parse_failures` | 解析失败日志 |

### 4.2 关键 DAO 方法

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Update
    suspend fun update(entity: TransactionEntity)
}
```

### 4.3 Repository 层

Repository 负责 **Entity ↔ Domain Model 映射**，向 feature 模块屏蔽 Room。

```kotlin
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    suspend fun insert(transaction: Transaction): Long
    suspend fun getById(id: Long): Transaction?
    suspend fun update(transaction: Transaction)
}
```

实现位于 `:core:database`，调用方通过 Koin 注入接口。

### 4.4 数据库配置

```kotlin
@Database(
    entities = [TransactionEntity::class, CategoryRuleEntity::class, ParseFailureLogEntity::class],
    version = 1
)
abstract class JizhangDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun parseFailureDao(): ParseFailureDao
}
```

---

## 5. `:feature:parser` — 交易解析器

### 5.1 输入输出

```kotlin
data class ParsedTransaction(
    val amount: Double?,
    val merchant: String?,
    val isPayment: Boolean
)

object TransactionParser {
    fun parse(notification: NotificationData): ParsedTransaction
}
```

### 5.2 解析策略

1. **支付检测**：将 `title + text + bigText` 拼接为 `fullText`，检查是否包含「支付」「付款」「支出」等关键词。
2. **金额提取**：正则匹配 `\d+\.\d{2}` 或 `\d+` 后接「元」。
3. **商户提取**：基于通知文本的启发式提取（如「向 XXX 付款」→ XXX）。

### 5.3 关键修复记录

| 问题 | 原因 | 修复 |
|------|------|------|
| `isPaymentText` 仅检查 `text` 字段 | 真实通知中「微信支付」常在 `title` | 拼接 `title+text+bigText` 为 `fullText` 后检测 |
| 跨模块 smart-cast 失败 | Kotlin 不允许跨模块边界 smart-cast | 提取局部变量：`val amount = parsed.amount` |

---

## 6. `:feature:capture` — 通知捕获与持久化

### 6.1 NotificationListenerService

`CaptureNotificationService` 监听系统通知，过滤微信 (`com.tencent.mm`) 和支付宝 (`com.uber.commerce`) 包名。

### 6.2 去重机制

```kotlin
class NotificationDeduplicator(
    private val windowMillis: Long = 3000L,
    private val maxSize: Int = 20
) {
    fun isDuplicate(newData: NotificationData): Boolean
}
```

**判定逻辑**：
- 在 3 秒时间窗口内
- `packageName` 相同 且 (`text` 相同 或 `bigText` 相同)
- 修复前缺陷：`null == null` 导致误匹配 → 修复后要求非 null 且相等

### 6.3 持久化 UseCase

```kotlin
class PersistCapturedTransactionUseCase(
    private val parser: TransactionParser,
    private val transactionRepository: TransactionRepository,
    private val parseFailureRepository: ParseFailureRepository
) {
    suspend operator fun invoke(notification: NotificationData)
}
```

**逻辑分支**：
- 解析成功（有金额、有商户、是支付）→ `transactionRepository.insert()`
- 解析失败 → `parseFailureRepository.insert()`（记录原始文本和原因）

---

## 7. `:feature:ledger` — 记账 UI

### 7.1 数据流架构

```
Room Flow (observeAll)
    ↓
TransactionRepository
    ↓
LedgerViewModel (viewModelScope)
    ↓
LedgerReducer.reduce(oldState, transactions)
    ↓
StateFlow<LedgerUiState> (distinctUntilChanged)
    ↓
Compose UI (collectAsState)
```

### 7.2 LedgerReducer

```kotlin
class LedgerReducer {
    fun reduce(old: LedgerUiState, transactions: List<Transaction>): LedgerUiState {
        val mapped = transactions.map { it.toUiItem() }
        if (old.items == mapped) return old
        return old.copy(items = mapped.toImmutableList(), isLoading = false)
    }
}
```

- 使用 `kotlinx.collections.immutable.persistentListOf` 保证 `==` 为结构比较。
- 若映射结果与旧状态相同，直接返回旧引用，避免无意义重组。

### 7.3 ViewModel

```kotlin
class LedgerViewModel(
    repository: TransactionRepository,
    reducer: LedgerReducer
) : ViewModel() {
    val uiState: StateFlow<LedgerUiState> = repository.observeAll()
        .map { reducer.reduce(LedgerUiState.EMPTY, it) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, LedgerUiState.EMPTY)
}
```

> **注意**：初始使用 `SharingStarted.WhileSubscribed(5000)`，但在 `UnconfinedTestDispatcher` 下 Turbine 测试会挂起。最终改为 `SharingStarted.Lazily`，确保测试稳定性。

### 7.4 TransactionDetailScreen

- 使用 `FlowRow`（`ExperimentalLayoutApi`）展示分类 FilterChip。
- 用户修改分类后点击保存，回调到 ViewModel 的 `save(merchant, category)`。
- `saveCompleted` 状态触发 `LaunchedEffect`，自动返回列表页。

---

## 8. `:app` — 入口与 DI 集成

### 8.1 Koin 模块结构

```kotlin
val appModules = listOf(
    databaseModule,   // :core:database
    captureModule,    // :feature:capture
    parserModule,     // :feature:parser
    ledgerModule      // :feature:ledger
)
```

### 8.2 MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JizhangPocTheme {
                var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
                when (val id = selectedTransactionId) {
                    null -> TransactionListScreen(...)
                    else -> TransactionDetailScreen(...)
                }
            }
        }
    }
}
```

- 使用 `koinViewModel()` 获取 ViewModel。
- Detail 页通过 `parametersOf(id)` 传递交易 ID。

### 8.3 Application

```kotlin
class JizhangApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@JizhangApplication)
                modules(appModules)
            }
        }
    }
}
```

> **注意**：`GlobalContext.getOrNull()` 检查用于防止 Robolectric 多测试用例运行时触发 `KoinAppAlreadyStartedException`。

---

## 9. 测试策略与实现

### 9.1 测试框架矩阵

| 测试类型 | 框架 | 适用场景 |
|----------|------|----------|
| 纯逻辑单元测试 | JUnit 5 (Jupiter) + MockK | Parser, Reducer, ViewModel, UseCase |
| Flow 异步测试 | Turbine | ViewModel 的 `StateFlow`/`SharedFlow` |
| Room DAO 测试 | JUnit 4 + Robolectric | 需要 `Context` 和 `Application` |
| Koin 模块校验 | Robolectric | `androidContext()` 依赖 |
| Compose UI 测试 | Compose Test Rule | Screen 组件交互验证 |

### 9.2 关键测试覆盖

| 模块 | 测试类 | 覆盖点 |
|------|--------|--------|
| `:feature:parser` | `TransactionParserTest` | 微信/支付宝支付解析、非支付过滤、金额/商户提取 |
| `:feature:capture` | `PersistCapturedTransactionUseCaseTest` | 成功入库、解析失败记录、非支付丢弃 |
| `:feature:capture` | `NotificationDeduplicatorTest` | 窗口内去重、不同内容通过、null 安全 |
| `:feature:ledger` | `LedgerReducerTest` | 列表映射、空列表、重复状态复用 |
| `:feature:ledger` | `LedgerViewModelTest` | Flow 收集、状态变化、Turbine 验证 |
| `:feature:ledger` | `TransactionDetailViewModelTest` | 加载详情、保存更新 |
| `:app` | `AppModuleTest` (Robolectric) | Koin 模块无循环依赖、关键类型可解析 |

### 9.3 ViewModel 测试模式

```kotlin
@BeforeEach
fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

@AfterEach
fun tearDown() {
    Dispatchers.resetMain()
}
```

所有涉及 `viewModelScope` 的测试必须设置 `Dispatchers.Main`，否则 `stateIn` 会崩溃。

---

## 10. 问题记录与解决方案

| # | 问题 | 根因 | 解决方案 |
|---|------|------|----------|
| 1 | Gradle 要求 Java 21，系统只有 17 | `gradle-daemon-jvm.properties` 指定了 21 | 修改为 `toolchainVersion=17` |
| 2 | 跨模块 smart-cast 编译失败 | Kotlin 不允许跨模块边界 smart-cast | 提取局部变量后再判断 |
| 3 | `:feature:capture` 编译失败，找不到 Repository | 缺少 `:core:database` 依赖 | 在 `build.gradle.kts` 中添加 `implementation(project(":core:database"))` |
| 4 | 去重器误将两个 `null` text 视为同一交易 | `isSameTransaction` 中 `text == other.text` 在双方都为 null 时返回 true | 增加非空判定：`text != null && other.text != null && text == other.text` |
| 5 | Koin `checkModules` 在 JUnit 5 中无法解析 `androidContext` | `checkModules` 需要真实 Android Context | 改用 `koinApplication { androidContext(context); modules(...) }.koin` + `getOrNull` 验证 |
| 6 | `:feature:parser` 缺少 Koin Android 依赖 | 模块内使用了 `module { single { ... } }` 但未声明 Koin | 添加 `implementation(libs.koin.android)` |
| 7 | `SharingStarted.WhileSubscribed` 在 TestDispatcher 下 Turbine 挂起 | `WhileSubscribed` 依赖真实生命周期 | 改为 `SharingStarted.Lazily` |
| 8 | `MutableSharedFlow` 在测试中无 replay 导致空悬 | 测试未收到初始值 | 改用 `MutableStateFlow` 作为测试输入 |
| 9 | `distinctUntilChanged` 过滤了首个空列表 | 初始状态和首个发射值相同 | 测试先发射非空列表，再验证空列表 |
| 10 | `FlowRow` 编译报错实验性 API | Compose Foundation Layout API 未稳定 | 添加 `@OptIn(ExperimentalLayoutApi::class)` |
| 11 | `MainActivity` 缺少 `parametersOf` 导入 | Koin Compose 参数传递需要显式 import | 添加 `import org.koin.core.parameter.parametersOf` |
| 12 | `KoinAppAlreadyStartedException`（Robolectric） | 多个测试用例共享同一进程，Koin 被重复启动 | `JizhangApplication` 中增加 `GlobalContext.getOrNull() == null` 检查 |

---

## 11. 验收结果

### 11.1 编译门禁

```bash
./gradlew :app:assembleDebug   # ✅ SUCCESS
```

### 11.2 测试门禁

```bash
./gradlew testDebugUnitTest    # ✅ 33 tests passed, 0 failed
```

### 11.3 模块构建验证

```bash
./gradlew :core:model:build            # ✅
./gradlew :core:database:build         # ✅
./gradlew :feature:parser:build        # ✅
./gradlew :feature:capture:build       # ✅
./gradlew :feature:ledger:build        # ✅
```

### 11.4 功能验证

- [x] `TransactionParser` 对已知微信/支付宝样本解析成功率 100%（基于现有测试用例）。
- [x] `NotificationDeduplicator` 3 秒窗口内正确去重。
- [x] Room 数据库 `observeAll()` Flow 在插入新交易后自动触发 UI 更新。
- [x] `LedgerReducer` 对重复列表返回同一引用，避免重组。
- [x] `TransactionDetailScreen` 支持商户编辑和分类选择（FilterChip）。
- [x] Koin DI 全链路无循环依赖，`AppModuleTest` 通过。

---

## 12. Phase 2 前置接口

Phase 1 已为 Phase 2 预留以下扩展点：

1. **`Transaction.category` 可空**：Phase 2 的分类引擎将填充此字段。
2. **`CategoryRule` 实体已存在**：Phase 2 只需新增 `matchType` 字段和数据库迁移。
3. **`PersistCapturedTransactionUseCase` 已预留分类引擎注入位**：计划增加 `classificationEngine: ClassificationEngine` 参数。
4. **`TransactionDetailViewModel.save()` 可扩展为生成用户规则**：用户修改分类时自动生成 `ExactRule`。

---

## 13. 附录：关键文件索引

| 文件 | 说明 |
|------|------|
| `core/model/src/main/java/.../core/model/Transaction.kt` | 领域模型 |
| `core/database/src/main/java/.../core/database/JizhangDatabase.kt` | Room Database |
| `core/database/src/main/java/.../core/database/repository/TransactionRepositoryImpl.kt` | Repository 实现 |
| `feature/parser/src/main/java/.../feature/parser/TransactionParser.kt` | 通知解析器 |
| `feature/capture/src/main/java/.../capture/dedup/NotificationDeduplicator.kt` | 去重器 |
| `feature/capture/src/main/java/.../capture/usecase/PersistCapturedTransactionUseCase.kt` | 入库 UseCase |
| `feature/ledger/src/main/java/.../ledger/ui/LedgerReducer.kt` | Reducer |
| `feature/ledger/src/main/java/.../ledger/ui/LedgerViewModel.kt` | 列表 ViewModel |
| `feature/ledger/src/main/java/.../ledger/ui/detail/TransactionDetailViewModel.kt` | 详情 ViewModel |
| `app/src/main/java/.../jizhang/MainActivity.kt` | 入口 Activity |
| `app/src/main/java/.../jizhang/di/AppModule.kt` | Koin 模块聚合 |
