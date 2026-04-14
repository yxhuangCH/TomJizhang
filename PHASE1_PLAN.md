# Phase 1 实施计划：核心引擎构建

> 周期：3 周  
> 目标：将 Phase 0 PoC 转化为模块化的 Production 级核心引擎，完成 **捕获 → 解析 → 存储 → 展示** 完整链路。  
> 状态：待审核

---

## 目录

1. [当前状态与目标状态](#1-当前状态与目标状态)
2. [Week 1：模块搭建、数据模型与数据库](#2-week-1模块搭建数据模型与数据库)
3. [Week 2：解析器、捕获层与依赖注入](#3-week-2解析器捕获层与依赖注入)
4. [Week 3：记账 UI 与集成](#4-week-3记账-ui-与集成)
5. [模块依赖图](#5-模块依赖图)
6. [技术决策说明](#6-技术决策说明)
7. [新增依赖清单](#7-新增依赖清单)
8. [关键文件清单](#8-关键文件清单)
9. [审核意见栏](#9-审核意见栏)

---

## 1. 当前状态与目标状态

### 当前状态（Phase 0 已完成）

- 单一 `:app` 模块，代码位于 `com.yxhuang.jizhang.poc`
- `NotificationListenerService` 已验证可捕获微信/支付宝通知
- `AccessibilityService` 已验证可作为 Fallback
- `TransactionParser` 已具备正则解析能力（32 个单元测试通过）
- 无数据库、无 DI、无模块化、无生产级 UI

### 目标状态（Phase 1 结束）

- 完整的 Clean Modular Architecture：`:core:*` + `:feature:*`
- Room 数据库支持 `Transaction`、`CategoryRule`、`ParseFailureLog`
- Flow + Repository 数据层
- Koin 依赖注入
- 生产级 `CaptureNotificationService`（含去重逻辑）
- `Reducer + StateFlow + Compose` 的记账列表与详情/编辑页
- 新 `MainActivity` 替代 PoC 入口页

---

## 2. Week 1：模块搭建、数据模型与数据库

### 2.1 Step 1.1 — 创建 Gradle 模块

#### 实现内容

修改 `settings.gradle.kts`：

```kotlin
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

新建模块及其插件选择：

| 模块 | 插件 | 说明 |
|------|------|------|
| `:core:model` | `kotlin("jvm")` | 纯 Kotlin，无 Android 依赖 |
| `:core:database` | `com.android.library` + `ksp` | Room 需要 Android 和 KSP |
| `:core:common` | `com.android.library` | 工具类、通用扩展 |
| `:feature:capture` | `com.android.library` | 通知监听、无障碍服务、去重 |
| `:feature:parser` | `com.android.library` | 交易解析器 |
| `:feature:ledger` | `com.android.library` | Compose UI、ViewModel、Reducer |

每个模块需创建 `build.gradle.kts`。`:core:model` 示例：

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
}
```

#### 该步骤的测试代码

- 无代码逻辑，仅构建验证。通过 Gradle 编译任务验证模块结构正确。

#### 该步骤的验收标准

- [ ] `./gradlew :core:model:build` 成功编译
- [ ] `./gradlew :core:database:build` 成功编译
- [ ] `./gradlew :feature:capture:build :feature:parser:build :feature:ledger:build` 成功编译

---

### 2.2 Step 1.2 — 定义 `:core:model` 领域模型

#### 实现内容

新建以下纯 Kotlin data class：

#### `Transaction.kt`

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

#### `CategoryRule.kt`

```kotlin
data class CategoryRule(
    val id: Long = 0,
    val keyword: String,
    val category: String,
    val confidence: Float = 1.0f
)
```

#### `ParseFailureLog.kt`

```kotlin
data class ParseFailureLog(
    val id: Long = 0,
    val rawText: String,
    val sourceApp: String,
    val timestamp: Long,
    val reason: String?
)
```

#### `ParsedTransaction.kt`

```kotlin
data class ParsedTransaction(
    val amount: String?,
    val merchant: String?,
    val isPayment: Boolean,
    val rawText: String,
    val sourceApp: String,
    val timestamp: Long
)
```

#### `NotificationData.kt`

从 PoC 迁移（已是纯 `@Serializable` data class，无 Android 依赖）。

#### 该步骤的测试代码

创建 `core/model/src/test/java/com/yxhuang/jizhang/core/model/DomainModelTest.kt`：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DomainModelTest {
    @Test
    fun `transaction has correct default values`() {
        val tx = Transaction(
            amount = 25.0,
            merchant = "星巴克",
            category = null,
            timestamp = 1000L,
            sourceApp = "com.tencent.mm",
            rawText = "微信支付 25.00元 星巴克"
        )
        assertEquals(0L, tx.id)
        assertTrue(tx.createdAt > 0)
    }

    @Test
    fun `categoryRule copy changes category`() {
        val rule = CategoryRule(keyword = "星巴克", category = "餐饮")
        val updated = rule.copy(category = "饮品")
        assertEquals("饮品", updated.category)
        assertEquals("星巴克", updated.keyword)
    }

    @Test
    fun `parsedTransaction equals works`() {
        val p1 = ParsedTransaction("25.0", "星巴克", true, "raw", "wechat", 1L)
        val p2 = ParsedTransaction("25.0", "星巴克", true, "raw", "wechat", 1L)
        assertEquals(p1, p2)
    }
}
```

#### 该步骤的验收标准

- [ ] `./gradlew :core:model:test` 通过
- [ ] 所有 data class 具备完整的 `copy()` 和 `equals` 行为验证
- [ ] `NotificationData` 从 `:app` 成功迁移且不影响现有序列化

---

### 2.3 Step 1.3 — `:core:database` Room 数据库

#### 实现内容

#### 文件清单

- `entity/TransactionEntity.kt`
- `entity/CategoryRuleEntity.kt`
- `entity/ParseFailureLogEntity.kt`
- `dao/TransactionDao.kt`
- `dao/CategoryRuleDao.kt`
- `dao/ParseFailureLogDao.kt`
- `JizhangDatabase.kt`
- `di/DatabaseModule.kt`

#### Entity 示例

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String?,
    val timestamp: Long,
    val sourceApp: String,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### DAO 示例

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?
}
```

#### DatabaseModule (Koin)

```kotlin
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            JizhangDatabase::class.java,
            "jizhang.db"
        ).build()
    }
    single { get<JizhangDatabase>().transactionDao() }
    single { get<JizhangDatabase>().categoryRuleDao() }
    single { get<JizhangDatabase>().parseFailureLogDao() }
}
```

**`core:database/build.gradle.kts` 关键依赖：**

```kotlin
implementation(project(":core:model"))
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
implementation(libs.kotlinx.coroutines.android)
ksp(libs.androidx.room.compiler)
```

#### 该步骤的测试代码

**说明：** Room DAO 的集成测试需要 Android `Context`。为配合 JUnit 5 + MockK 的主测试栈，DAO 测试采用 **JUnit 4 + Robolectric**（例外情况），其他层（Repository/ViewModel/UseCase）全部使用 JUnit 5 + MockK。

创建 `core/database/src/test/java/.../core/database/JizhangDatabaseTest.kt`（JUnit 4 + Robolectric）：

```kotlin
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Before
import org.junit.After
import org.junit.Test
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JizhangDatabaseTest {
    private lateinit var db: JizhangDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JizhangDatabase::class.java).build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and observe transactions via flow`() = runTest {
        val entity = TransactionEntity(
            amount = 25.0,
            merchant = "星巴克",
            category = null,
            timestamp = 1000L,
            sourceApp = "wechat",
            rawText = "test"
        )
        dao.insert(entity)

        dao.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("星巴克", list[0].merchant)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `update transaction changes merchant`() = runTest {
        val id = dao.insert(TransactionEntity(amount = 10.0, merchant = "A", category = null, timestamp = 1L, sourceApp = "wechat", rawText = "test"))
        dao.update(TransactionEntity(id = id, amount = 10.0, merchant = "B", category = null, timestamp = 1L, sourceApp = "wechat", rawText = "test"))
        val updated = dao.getById(id)
        assertEquals("B", updated?.merchant)
    }
}
```

#### 该步骤的验收标准

- [ ] `./gradlew :core:database:testDebugUnitTest` 全部通过
- [ ] DAO 的 `observeAll()` 通过 Turbine 验证 Flow 正确发射
- [ ] `TransactionDao`、`CategoryRuleDao`、`ParseFailureLogDao` 各至少覆盖 insert + query

---

### 2.4 Step 1.4 — Repository 层

#### 实现内容

在 `:core:database` 中创建 Repository，负责 **Entity ↔ Domain Model** 转换。

#### 接口与实现

- `repository/TransactionRepository.kt` + `TransactionRepositoryImpl.kt`
- `repository/CategoryRuleRepository.kt` + `CategoryRuleRepositoryImpl.kt`
- `repository/ParseFailureRepository.kt` + `ParseFailureRepositoryImpl.kt`

#### `TransactionRepository` 接口

```kotlin
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun getById(id: Long): Transaction?
}
```

**原则：** Repository 对 feature 模块只暴露 domain model，隐藏 Room Entity。

Expose via Koin in `di/DatabaseModule.kt`.

#### 该步骤的测试代码

Repository 层使用 **JUnit 5 + MockK + Turbine**，Mock DAO 行为，不依赖真实 Room。

创建 `core/database/src/test/java/.../core/database/repository/TransactionRepositoryImplTest.kt`：

```kotlin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import app.cash.turbine.test

class TransactionRepositoryImplTest {
    private val dao: TransactionDao = mockk(relaxed = true)
    private val repository = TransactionRepositoryImpl(dao)

    @Test
    fun `observeAll emits domain models mapped from entities`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(TransactionEntity(1L, 25.0, "星巴克", null, 1L, "wechat", "test", 1L))
        )

        repository.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("星巴克", list[0].merchant)
            assertTrue(list[0] is Transaction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert converts domain model to entity and delegates to dao`() = runTest {
        coEvery { dao.insert(any()) } returns 42L

        val id = repository.insert(
            Transaction(amount = 18.5, merchant = "滴滴", category = null, timestamp = 2L, sourceApp = "alipay", rawText = "test")
        )

        assertEquals(42L, id)
        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun `getById returns mapped domain model`() = runTest {
        coEvery { dao.getById(1L) } returns TransactionEntity(1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test", 1L)

        val result = repository.getById(1L)

        assertEquals("星巴克", result?.merchant)
        assertEquals("餐饮", result?.category)
    }
}
```

#### 该步骤的验收标准

- [ ] 所有 Repository 实现通过单元测试（JUnit 5 + MockK）
- [ ] `Repository.observeAll()` 发射的是 `:core:model` 类型，而非 Entity
- [ ] insert/update/getById 的数据转换无丢失
- [ ] `./gradlew :core:database:testDebugUnitTest` 新增测试后仍全绿

---

## 3. Week 2：解析器、捕获层与依赖注入

### 3.1 Step 2.1 — 迁移解析器到 `:feature:parser`

#### 实现内容

#### 新建文件

- `feature/parser/TransactionParser.kt`
- `di/ParserModule.kt`

#### 接口变更

```kotlin
object TransactionParser {
    fun parse(notification: NotificationData): ParsedTransaction
}
```

- 输入从 `String` 升级为 `NotificationData`
- 输出使用 `:core:model` 的 `ParsedTransaction`
- 保留所有现有正则逻辑和 fallback 策略

#### 该步骤的测试代码

迁移并扩展测试到 `feature/parser/src/test/java/.../feature/parser/TransactionParserTest.kt`（JUnit 5）：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TransactionParserTest {
    @Test
    fun `parse wechat payment from notification data`() {
        val notification = NotificationData(
            timestamp = 1000L,
            packageName = "com.tencent.mm",
            tickerText = null,
            title = "微信支付",
            text = "25.00元 星巴克",
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertEquals("25.00", result.amount)
        assertEquals("星巴克", result.merchant)
        assertTrue(result.isPayment)
        assertEquals("com.tencent.mm", result.sourceApp)
    }

    @Test
    fun `parse alipay payment from notification data`() {
        val notification = NotificationData(
            timestamp = 2000L,
            packageName = "com.eg.android.AlipayGphone",
            text = "支付宝 滴滴出行 18.50",
            title = "支付宝",
            tickerText = null,
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertEquals("18.50", result.amount)
        assertEquals("滴滴出行", result.merchant)
    }

    @Test
    fun `parse unknown package returns null fields`() {
        val notification = NotificationData(
            timestamp = 3000L,
            packageName = "com.unknown",
            text = "微信支付 星巴克 25.00元",
            title = null,
            tickerText = null,
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertNull(result.amount)
        assertNull(result.merchant)
        assertTrue(result.isPayment)
    }
}
```

**要求：** 迁移原有 PoC 的所有测试用例，并新增 `NotificationData` 输入测试。

#### 该步骤的验收标准

- [ ] `./gradlew :feature:parser:testDebugUnitTest` 全部通过
- [ ] Parser 模块行覆盖率 ≥ 80%
- [ ] 所有原有 PoC 解析场景在新接口下仍被覆盖

---

### 3.2 Step 2.2 — `:feature:capture` 捕获层

#### 实现内容

#### 新建文件

- `notification/CaptureNotificationService.kt`
- `notification/NotificationExtractor.kt`
- `dedup/NotificationDeduplicator.kt`
- `usecase/PersistCapturedTransactionUseCase.kt`
- `di/CaptureModule.kt`

#### `CaptureNotificationService`

生产级 `NotificationListenerService`：

1. 过滤包名（微信/支付宝）
2. `NotificationExtractor.extract(sbn)` → `NotificationData`
3. 传给 `NotificationDeduplicator`
4. 非重复则调用 `PersistCapturedTransactionUseCase`

#### `NotificationDeduplicator`

```kotlin
class NotificationDeduplicator(private val windowMillis: Long = 3000L) {
    private val recent = ArrayDeque<NotificationData>(20)

    fun isDuplicate(newData: NotificationData): Boolean {
        // 基于 (packageName, text, amount 提取结果) 和时间窗判断
    }
}
```

使用内存环形缓冲区（最近 20 条），因为服务进程长期存活。

#### `PersistCapturedTransactionUseCase`

```kotlin
class PersistCapturedTransactionUseCase(
    private val parser: TransactionParser,
    private val transactionRepository: TransactionRepository,
    private val parseFailureRepository: ParseFailureRepository
) {
    suspend operator fun invoke(notification: NotificationData) {
        val parsed = parser.parse(notification)
        if (parsed.amount != null && parsed.merchant != null && parsed.isPayment) {
            transactionRepository.insert(
                Transaction(
                    amount = parsed.amount.toDouble(),
                    merchant = parsed.merchant,
                    category = null,
                    timestamp = notification.timestamp,
                    sourceApp = notification.packageName,
                    rawText = notification.text ?: notification.bigText ?: ""
                )
            )
        } else {
            parseFailureRepository.insert(
                ParseFailureLog(
                    rawText = notification.text ?: "",
                    sourceApp = notification.packageName,
                    timestamp = notification.timestamp,
                    reason = if (!parsed.isPayment) "Not a payment" else "Missing amount or merchant"
                )
            )
        }
    }
}
```

#### 该步骤的测试代码

**A. `NotificationExtractorTest.kt`**（JUnit 5，迁移 PoC 测试）：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NotificationExtractorTest {
    @Test
    fun `shouldCapture returns true for wechat`() {
        assertTrue(NotificationExtractor.shouldCapture("com.tencent.mm"))
    }

    @Test
    fun `extract creates NotificationData with all fields`() {
        val data = NotificationExtractor.extract(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "25.00元 星巴克",
            currentTimeMillis = 12345678L
        )
        assertEquals("星巴克", data.text?.substringAfter(" "))
        assertEquals(12345678L, data.timestamp)
    }
}
```

**B. `NotificationDeduplicatorTest.kt`**（JUnit 5）：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NotificationDeduplicatorTest {
    private val deduplicator = NotificationDeduplicator(windowMillis = 3000L)

    @Test
    fun `exact duplicate within window is duplicate`() {
        val n1 = NotificationData(1000L, "com.tencent.mm", null, "支付", "25.00", null, null, null)
        val n2 = NotificationData(1001L, "com.tencent.mm", null, "支付", "25.00", null, null, null)
        assertFalse(deduplicator.isDuplicate(n1))
        assertTrue(deduplicator.isDuplicate(n2))
    }

    @Test
    fun `same text after window is not duplicate`() {
        val n1 = NotificationData(1000L, "com.tencent.mm", null, "支付", "25.00", null, null, null)
        val n2 = NotificationData(5000L, "com.tencent.mm", null, "支付", "25.00", null, null, null)
        assertFalse(deduplicator.isDuplicate(n1))
        assertFalse(deduplicator.isDuplicate(n2))
    }

    @Test
    fun `different text is not duplicate`() {
        val n1 = NotificationData(1000L, "com.tencent.mm", null, "支付", "25.00", null, null, null)
        val n2 = NotificationData(1001L, "com.tencent.mm", null, "支付", "30.00", null, null, null)
        assertFalse(deduplicator.isDuplicate(n1))
        assertFalse(deduplicator.isDuplicate(n2))
    }
}
```

**C. `PersistCapturedTransactionUseCaseTest.kt`**（JUnit 5 + MockK）：

```kotlin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PersistCapturedTransactionUseCaseTest {
    private val parser = TransactionParser
    private val transactionRepo: TransactionRepository = mockk(relaxed = true)
    private val failureRepo: ParseFailureRepository = mockk(relaxed = true)
    private val useCase = PersistCapturedTransactionUseCase(parser, transactionRepo, failureRepo)

    @Test
    fun `valid payment inserts transaction`() = runTest {
        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCase(notification)
        coVerify(exactly = 1) { transactionRepo.insert(any()) }
        coVerify(exactly = 0) { failureRepo.insert(any()) }
    }

    @Test
    fun `non payment inserts parse failure`() = runTest {
        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信", "你收到一条新消息", null, null, null
        )
        useCase(notification)
        coVerify(exactly = 0) { transactionRepo.insert(any()) }
        coVerify(exactly = 1) { failureRepo.insert(match { it.reason == "Not a payment" }) }
    }

    @Test
    fun `missing merchant inserts parse failure`() = runTest {
        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元", null, null, null
        )
        useCase(notification)
        coVerify(exactly = 0) { transactionRepo.insert(any()) }
        coVerify(exactly = 1) { failureRepo.insert(match { it.reason == "Missing amount or merchant" }) }
    }
}
```

#### 该步骤的验收标准

- [ ] `./gradlew :feature:capture:testDebugUnitTest` 全部通过
- [ ] `NotificationExtractor` 测试覆盖率 ≥ 80%
- [ ] `NotificationDeduplicator` 覆盖：去重命中、超时放行、不同内容放行
- [ ] `PersistCapturedTransactionUseCase` 覆盖：有效支付入库、非支付进失败日志、缺少字段进失败日志

---

### 3.3 Step 2.3 — Koin 依赖注入集成

#### 实现内容

#### 新建文件

- `app/src/main/java/com/yxhuang/jizhang/JizhangApplication.kt`
- `app/src/main/java/com/yxhuang/jizhang/di/AppModule.kt`

#### `JizhangApplication`

```kotlin
class JizhangApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@JizhangApplication)
            modules(appModules)
        }
    }
}
```

#### `AppModule.kt`

```kotlin
val appModules = listOf(
    databaseModule,
    transactionRepositoryModule,
    categoryRuleRepositoryModule,
    parseFailureRepositoryModule,
    parserModule,
    captureModule,
    ledgerModule
)
```

#### Manifest 修改

```xml
<application
    android:name=".JizhangApplication"
    ... >
```

#### `app/build.gradle.kts` 新增依赖

```kotlin
implementation(libs.koin.android)
implementation(libs.koin.androidx.compose)
```

#### 该步骤的测试代码

Koin 的 `checkModules` 在 JUnit 5 下可直接使用。创建 `app/src/test/java/.../di/KoinModuleCheckTest.kt`（JUnit 5 + Robolectric，因需 `Context`）：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.test.check.checkModules
import tech.rahulsuvarna.robolectric.JUnit5RobolectricExtension

@ExtendWith(JUnit5RobolectricExtension::class)
class KoinModuleCheckTest {
    @Test
    fun `check all modules can be created`() {
        checkModules {
            androidContext(org.robolectric.RuntimeEnvironment.getApplication())
            modules(appModules)
        }
    }
}
```

> **注：** 若项目中未引入 JUnit 5 的 Robolectric Extension，可暂用 JUnit 4 版本的 `KoinModuleCheckTest`，但其他所有业务逻辑测试统一使用 JUnit 5 + MockK。

#### 该步骤的验收标准

- [ ] `./gradlew :app:testDebugUnitTest` 中 Koin 模块校验测试通过
- [ ] `JizhangApplication` 在 `AndroidManifest.xml` 中正确注册
- [ ] 启动 App 不崩溃，Logcat 无 Koin 循环依赖或缺失绑定错误

---

## 4. Week 3：记账 UI 与集成

### 4.1 Step 3.1 — Ledger UI State、Reducer、ViewModel

#### 实现内容

#### 新建文件

- `feature/ledger/ui/LedgerUiState.kt`
- `feature/ledger/ui/LedgerReducer.kt`
- `feature/ledger/ui/LedgerViewModel.kt`

#### `LedgerUiState`

```kotlin
data class LedgerUiState(
    val items: ImmutableList<TransactionItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TransactionItem(
    val id: Long,
    val amountText: String,
    val merchant: String,
    val category: String?,
    val timeText: String
)
```

#### `LedgerReducer`

```kotlin
class LedgerReducer {
    fun reduce(old: LedgerUiState, transactions: List<Transaction>): LedgerUiState {
        val mapped = transactions.map { it.toUiItem() }
        if (old.items == mapped) return old
        return old.copy(items = mapped.toImmutableList())
    }
}
```

#### `LedgerViewModel`

```kotlin
class LedgerViewModel(
    repository: TransactionRepository,
    private val reducer: LedgerReducer
) : ViewModel() {
    val uiState: StateFlow<LedgerUiState> = repository.observeAll()
        .map { reducer.reduce(LedgerUiState(isLoading = true), it) }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LedgerUiState(isLoading = true)
        )
}
```

**`feature:ledger/build.gradle.kts` 新增：**

```kotlin
implementation(project(":core:model"))
implementation(project(":core:database"))
implementation(libs.kotlinx.collections.immutable)
implementation(libs.koin.androidx.compose)
implementation(libs.androidx.lifecycle.viewmodel.compose)
```

#### 该步骤的测试代码

**A. `LedgerReducerTest.kt`**（JUnit 5）：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LedgerReducerTest {
    private val reducer = LedgerReducer()

    @Test
    fun `reduce empty list returns empty state`() {
        val result = reducer.reduce(LedgerUiState(), emptyList())
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `reduce same list returns old state reference`() {
        val transactions = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))
        val old = reducer.reduce(LedgerUiState(), transactions)
        val new = reducer.reduce(old, transactions)
        assertSame(old, new)
    }

    @Test
    fun `reduce different list returns new state`() {
        val t1 = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))
        val t2 = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))
        val old = reducer.reduce(LedgerUiState(), t1)
        val new = reducer.reduce(old, t2)
        assertNotSame(old, new)
        assertEquals("星巴克", new.items[0].merchant)
    }
}
```

**B. `LedgerViewModelTest.kt`**（JUnit 5 + MockK + Turbine）：

```kotlin
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import app.cash.turbine.test

class LedgerViewModelTest {
    private val repository: TransactionRepository = mockk()
    private val flow = MutableSharedFlow<List<Transaction>>()

    @Test
    fun `uiState emits loading then items`() = runTest {
        every { repository.observeAll() } returns flow
        val viewModel = LedgerViewModel(repository, LedgerReducer())

        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)

            flow.emit(listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test")))

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

#### 该步骤的验收标准

- [ ] `./gradlew :feature:ledger:testDebugUnitTest` 中 Reducer 和 ViewModel 测试通过
- [ ] `LedgerReducer` 对相同数据返回旧引用（避免无意义重组）
- [ ] `LedgerViewModel` 首次发射 `isLoading = true`，随后发射列表数据

---

### 4.2 Step 3.2 — 交易列表页

#### 实现内容

#### 新建文件

- `feature/ledger/ui/list/TransactionListScreen.kt`
- `feature/ledger/ui/list/TransactionListItem.kt`

#### 设计

- `TransactionListScreen` 通过 `collectAsStateWithLifecycle()` 观察 `LedgerViewModel.uiState`
- `LazyColumn` + `items(state.items, key = { it.id })`
- 每条显示：金额（右对齐）、商户名、分类标签、格式化时间
- 点击跳转详情页

#### 该步骤的测试代码

Compose UI 测试（ instrumentation 或 Robolectric Compose Test）。创建 `feature/ledger/src/androidTest/.../TransactionListScreenTest.kt`：

```kotlin
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class TransactionListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `transaction list renders correct item count`() {
        val items = listOf(
            TransactionItem(1L, "-25.00", "星巴克", "餐饮", "10:30"),
            TransactionItem(2L, "-18.50", "滴滴出行", "交通", "14:20")
        )
        composeTestRule.setContent {
            TransactionListScreen(
                uiState = LedgerUiState(items = items.toImmutableList()),
                onTransactionClick = {}
            )
        }
        composeTestRule.onNodeWithText("星巴克").assertIsDisplayed()
        composeTestRule.onNodeWithText("滴滴出行").assertIsDisplayed()
    }

    @Test
    fun `clicking item triggers callback`() {
        var clickedId: Long? = null
        val items = listOf(TransactionItem(1L, "-25.00", "星巴克", "餐饮", "10:30"))
        composeTestRule.setContent {
            TransactionListScreen(
                uiState = LedgerUiState(items = items.toImmutableList()),
                onTransactionClick = { clickedId = it }
            )
        }
        composeTestRule.onNodeWithText("星巴克").performClick()
        assertEquals(1L, clickedId)
    }
}
```

#### 该步骤的验收标准

- [ ] Compose UI 测试通过
- [ ] 列表正确渲染传入的 `TransactionItem` 数量
- [ ] 点击条目触发 `onTransactionClick` 回调并传递正确 ID
- [ ] `LazyColumn` 使用了 `key = { it.id }`

---

### 4.3 Step 3.3 — 交易详情/编辑页

#### 实现内容

#### 新建文件

- `feature/ledger/ui/detail/TransactionDetailViewModel.kt`
- `feature/ledger/ui/detail/TransactionDetailScreen.kt`

#### `TransactionDetailViewModel`

- 通过 ID 加载交易
- 暴露 `StateFlow<TransactionDetailUiState>`
- 处理 `save(amount, merchant, category)` → 调用 `TransactionRepository.update()`

#### `TransactionDetailScreen`

- `OutlinedTextField`：金额、商户
- 分类选择：Chip 组（"餐饮", "交通", "购物", "娱乐", "其他"）
- 保存按钮（输入有效时启用）

#### 该步骤的测试代码

**A. `TransactionDetailViewModelTest.kt`**（JUnit 5 + MockK + Turbine）：

```kotlin
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import app.cash.turbine.test

class TransactionDetailViewModelTest {
    @Test
    fun `load populates state with existing transaction`() = runTest {
        val repo: TransactionRepository = mockk()
        every { repo.observeAll() } returns flowOf(emptyList()) // unused
        coEvery { repo.getById(1L) } returns Transaction(1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test")

        val viewModel = TransactionDetailViewModel(1L, repo)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("星巴克", state.merchant)
            assertEquals("餐饮", state.category)
            assertEquals("25.0", state.amountText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save updates repository`() = runTest {
        val repo: TransactionRepository = mockk(relaxed = true)
        coEvery { repo.getById(1L) } returns Transaction(1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test")

        val viewModel = TransactionDetailViewModel(1L, repo)
        viewModel.save(merchant = "瑞幸咖啡", category = "饮品")

        coVerify {
            repo.update(match { it.merchant == "瑞幸咖啡" && it.category == "饮品" })
        }
    }
}
```

**B. `TransactionDetailScreenTest.kt`**（Compose UI Test）：

```kotlin
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import org.junit.Rule
import org.junit.Test

class TransactionDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `save button enabled when input is valid`() {
        composeTestRule.setContent {
            TransactionDetailScreen(
                merchant = "星巴克",
                amountText = "25.00",
                onSave = {}
            )
        }
        composeTestRule.onNodeWithText("保存").assertIsEnabled()
    }

    @Test
    fun `save button disabled when merchant is blank`() {
        composeTestRule.setContent {
            TransactionDetailScreen(
                merchant = "",
                amountText = "25.00",
                onSave = {}
            )
        }
        composeTestRule.onNodeWithText("保存").assertIsNotEnabled()
    }
}
```

#### 该步骤的验收标准

- [ ] `TransactionDetailViewModelTest`：加载成功、保存调用 Repository.update() 成功
- [ ] `TransactionDetailScreenTest`：有效输入时保存按钮启用，无效输入时禁用
- [ ] 分类选择支持 5 个种子分类并正确回写状态

---

### 4.4 Step 3.4 — 集成到 `:app`

#### 实现内容

#### 新建/修改文件

- 新建 `app/src/main/java/com/yxhuang/jizhang/MainActivity.kt`（生产级，替代 PoC 版本）
- 修改 `app/src/main/AndroidManifest.xml`
- 修改 `app/build.gradle.kts`

#### 生产级 `MainActivity`

使用 Compose 作为根，展示 `TransactionListScreen`，并通过简单导航（如条件渲染或 Navigation Compose）切换到 `TransactionDetailScreen`。

#### `app/build.gradle.kts` 新增模块依赖

```kotlin
implementation(project(":core:model"))
implementation(project(":core:database"))
implementation(project(":core:common"))
implementation(project(":feature:capture"))
implementation(project(":feature:parser"))
implementation(project(":feature:ledger"))
```

#### 该步骤的测试代码

- `KoinModuleCheckTest`（已在 Step 2.3 中定义）作为集成前校验。
- 无额外代码测试，依赖端到端手动 QA。

#### 该步骤的验收标准

- [ ] `./gradlew :app:assembleDebug` 成功
- [ ] `./gradlew :app:testDebugUnitTest` 全部通过（含 Koin 校验）
- [ ] 安装 APK 到真机/模拟器后，App 能正常启动并显示记账列表页
- [ ] 授予通知权限后，完成一笔微信支付/支付宝支付，5 秒内交易出现在列表中
- [ ] 点击交易条目进入编辑页，修改分类后返回列表，列表实时刷新
- [ ] 列表滑动帧率稳定 60fps（通过 Layout Inspector 或 Systrace 验证）

---

## 5. 模块依赖图

```
                :app
                 │
    ┌────────────┼────────────┐
    │            │            │
    ▼            ▼            ▼
:feature:capture  :feature:parser  :feature:ledger
    │            │            │
    │            │            ▼
    │            │      :core:database
    │            │            │
    └────────────┴────────────┘
                 │
                 ▼
           :core:model
                 │
                 ▼
           :core:common
```

**依赖原则：**
- `feature → core`（单向）
- `core:model` 无 Android 依赖，可被任何模块引用
- `app` 聚合所有模块

---

## 6. 技术决策说明

| 决策项 | 选择 | 原因 |
|--------|------|------|
| DI 框架 | **Koin** | 轻量、模块化友好、无需 kapt/annotation processing |
| 数据库 | **Room** | Android 官方、与 Flow/Kotlin Coroutines 集成成熟 |
| 不可变集合 | **kotlinx.collections.immutable** | 与 Compose `distinctUntilChanged` 配合最佳 |
| 后台捕获 | **NotificationListenerService** | Phase 0 已验证可行，优先使用原生方案 |
| 去重策略 | **内存环形缓冲区（20条 / 3秒窗）** | 简单高效，无需数据库 IO |
| UI 架构 | **Reducer + StateFlow + Compose** | 符合 DESIGN.md 规范，重组可控 |
| 测试框架 | **JUnit 5 + MockK + Turbine** | 现代测试栈，MockK 对 Kotlin 协程支持好，Turbine 测 Flow 最可靠 |

---

## 7. 新增依赖清单

需在 `gradle/libs.versions.toml` 中新增：

```toml
[versions]
room = "2.6.1"
koin = "3.5.6"
koinCompose = "3.5.6"
coroutines = "1.8.1"
immutableCollections = "0.3.7"
turbine = "1.1.0"
junit5 = "5.10.2"
mockk = "1.13.11"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }
koin-androidx-compose = { group = "io.insert-koin", name = "koin-androidx-compose", version.ref = "koinCompose" }
koin-test = { group = "io.insert-koin", name = "koin-test", version.ref = "koin" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "immutableCollections" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.7.0" }
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

[plugins]
android-library = { id = "com.android.library", version.ref = "agp" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

---

## 8. 关键文件清单

### 配置
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `build.gradle.kts`（root，新增 `android-library` 和 `ksp` plugins）

### `:core:model`
- `core/model/src/main/java/.../core/model/Transaction.kt`
- `core/model/src/main/java/.../core/model/CategoryRule.kt`
- `core/model/src/main/java/.../core/model/ParseFailureLog.kt`
- `core/model/src/main/java/.../core/model/ParsedTransaction.kt`
- `core/model/src/main/java/.../core/model/NotificationData.kt`
- `core/model/src/test/java/.../core/model/DomainModelTest.kt`

### `:core:database`
- `core/database/src/main/java/.../core/database/entity/*Entity.kt`
- `core/database/src/main/java/.../core/database/dao/*Dao.kt`
- `core/database/src/main/java/.../core/database/JizhangDatabase.kt`
- `core/database/src/main/java/.../core/database/di/DatabaseModule.kt`
- `core/database/src/main/java/.../core/database/repository/*Repository.kt`
- `core/database/src/main/java/.../core/database/repository/*RepositoryImpl.kt`
- `core/database/src/test/java/.../core/database/JizhangDatabaseTest.kt`（JUnit 4 + Robolectric）
- `core/database/src/test/java/.../core/database/repository/*RepositoryImplTest.kt`（JUnit 5 + MockK）

### `:feature:parser`
- `feature/parser/src/main/java/.../feature/parser/TransactionParser.kt`
- `feature/parser/src/main/java/.../feature/parser/di/ParserModule.kt`
- `feature/parser/src/test/java/.../feature/parser/TransactionParserTest.kt`（JUnit 5）

### `:feature:capture`
- `feature/capture/src/main/java/.../feature/capture/notification/CaptureNotificationService.kt`
- `feature/capture/src/main/java/.../feature/capture/notification/NotificationExtractor.kt`
- `feature/capture/src/main/java/.../feature/capture/dedup/NotificationDeduplicator.kt`
- `feature/capture/src/main/java/.../feature/capture/usecase/PersistCapturedTransactionUseCase.kt`
- `feature/capture/src/main/java/.../feature/capture/di/CaptureModule.kt`
- `feature/capture/src/test/java/.../feature/capture/notification/NotificationExtractorTest.kt`（JUnit 5）
- `feature/capture/src/test/java/.../feature/capture/dedup/NotificationDeduplicatorTest.kt`（JUnit 5）
- `feature/capture/src/test/java/.../feature/capture/usecase/PersistCapturedTransactionUseCaseTest.kt`（JUnit 5 + MockK）

### `:feature:ledger`
- `feature/ledger/src/main/java/.../feature/ledger/ui/LedgerUiState.kt`
- `feature/ledger/src/main/java/.../feature/ledger/ui/LedgerReducer.kt`
- `feature/ledger/src/main/java/.../feature/ledger/ui/LedgerViewModel.kt`
- `feature/ledger/src/main/java/.../feature/ledger/ui/list/TransactionListScreen.kt`
- `feature/ledger/src/main/java/.../feature/ledger/ui/list/TransactionListItem.kt`
- `feature/ledger/src/main/java/.../feature/ledger/ui/detail/TransactionDetailViewModel.kt`
- `feature/ledger/src/main/java/.../feature/ledger/ui/detail/TransactionDetailScreen.kt`
- `feature/ledger/src/main/java/.../feature/ledger/di/LedgerModule.kt`
- `feature/ledger/src/test/java/.../feature/ledger/ui/LedgerReducerTest.kt`（JUnit 5）
- `feature/ledger/src/test/java/.../feature/ledger/ui/LedgerViewModelTest.kt`（JUnit 5 + MockK + Turbine）
- `feature/ledger/src/test/java/.../feature/ledger/ui/detail/TransactionDetailViewModelTest.kt`（JUnit 5 + MockK + Turbine）
- `feature/ledger/src/androidTest/.../TransactionListScreenTest.kt`
- `feature/ledger/src/androidTest/.../TransactionDetailScreenTest.kt`

### `:app`
- `app/src/main/java/com/yxhuang/jizhang/JizhangApplication.kt`
- `app/src/main/java/com/yxhuang/jizhang/MainActivity.kt`
- `app/src/main/java/com/yxhuang/jizhang/di/AppModule.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/.../di/KoinModuleCheckTest.kt`

---

## 9. 审核意见栏

请在确认或修改以下内容后，在此文档末尾回复：

1. [ ] 模块划分是否符合预期？
2. [ ] Koin 作为 DI 框架是否可接受？
3. [ ] `NotificationDeduplicator` 的 3 秒/20 条策略是否需要调整？
4. [ ] Ledger UI 的分类种子列表（餐饮、交通、购物、娱乐、其他）是否足够？
5. [ ] JUnit 5 + MockK + Turbine 的测试栈是否可接受？（Room DAO 集成测试例外保留 JUnit 4 + Robolectric）
6. [ ] 是否有需要删减或新增的功能点？
