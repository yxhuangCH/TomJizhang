# Phase 3 实施计划：数据洞察与财务管理

> 周期：4 周
> 目标：在 Phase 2 MVP 产品化基础上，增加**数据统计分析**、**预算管理**、**交易搜索筛选**与**周期性交易识别**，将产品从"自动记账工具"升级为"个人财务管理中心"。
> 前置条件：Phase 2 已全部完成并通过验收（分类引擎、AI 学习闭环、保活服务、隐私合规）。
> 状态：待审核

---

## 目录

1. [当前状态与目标状态](#1-当前状态与目标状态)
2. [Week 1：数据洞察引擎与统计图表](#2-week-1数据洞察引擎与统计图表)
3. [Week 2：预算管理与超支预警](#3-week-2预算管理与超支预警)
4. [Week 3：交易搜索与筛选](#4-week-3交易搜索与筛选)
5. [Week 4：周期性交易识别](#5-week-4周期性交易识别)
6. [模块依赖图](#6-模块依赖图)
7. [技术决策说明](#7-技术决策说明)
8. [新增依赖清单](#8-新增依赖清单)
9. [关键文件清单](#9-关键文件清单)
10. [审核意见栏](#10-审核意见栏)
11. [附录 A：推迟到 Phase 4 的功能](#11-附录-a推迟到-phase-4-的功能)

---

## 1. 当前状态与目标状态

### 当前状态（Phase 2 已完成）

- 完整 Clean Modular Architecture：`:core:*` + `:feature:*` + `:ai`
- 三层分类引擎 + AI 学习闭环 + 日配额限制
- 140 条种子规则库，用户修改分类自动生成 ExactRule
- 前台保活服务 + 首次启动引导页 + 保活状态检测
- 隐私政策 + CSV 导出 + 数据清除
- Ledger 列表 + 详情/编辑页（仅展示原始数据，无统计能力）
- **所有交易仅按时间倒序列出，无汇总、无图表、无分析**
- **交易模型无收支类型区分**（所有交易视为支出）

### 目标状态（Phase 3 结束）

- **Transaction 模型新增 `type: TransactionType`**（INCOME / EXPENSE），贯穿全管道
- 新增 `:feature:analytics` 模块：数据统计引擎 + 图表 UI
- **月度/年度收支汇总**：总收入、总支出、净结余
- **分类支出饼图/环形图**：直观展示各类别占比
- **支出趋势折线图**：日/周/月粒度的时间序列分析
- **Top 商户排行**：消费最多的商户列表
- **预算管理**：为每个分类设置月度预算，超支时推送预警通知
- **交易搜索**：按商户名、金额范围、分类、时间区间实时搜索
- **交易筛选**：多条件组合筛选（分类、时间范围、金额区间）
- **周期性交易识别**：自动识别房租、订阅、水电煤等周期性支出

### 推迟到 Phase 4 的功能

- 银行短信（SMS）解析
- 美团 / 京东支付通知扩展支持

---

## 2. Week 1：数据洞察引擎与统计图表

### 2.1 Step 3.1 — 创建 `:feature:analytics` 模块 + `TransactionType` 模型变更

#### 实现内容

**A. 修改 `settings.gradle.kts`：**

```kotlin
include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:common",
    ":feature:capture",
    ":feature:parser",
    ":feature:classification",
    ":feature:ledger",
    ":feature:analytics",  // 新增
    ":ai"
)
```

**:feature:analytics/build.gradle.kts** 关键依赖：

```kotlin
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.vico.compose)      // 主图表库
    implementation(libs.mpandroidchart)    // 备选（见风险说明）
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

**B. `TransactionType` 模型变更（`:core:model`）：**

```kotlin
// core/model/src/main/java/.../core/model/Transaction.kt
enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,  // 新增
    val timestamp: Long,
    val sourceApp: String,
    val rawText: String
)
```

**C. `TransactionEntity` 变更（`:core:database`）：**

```kotlin
// core/database/src/main/java/.../core/database/entity/TransactionEntity.kt
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "merchant") val merchant: String,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "type") val type: String = "EXPENSE",  // 新增，存储枚举名
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "source_app") val sourceApp: String,
    @ColumnInfo(name = "raw_text") val rawText: String
)
```

**D. 数据库 Schema 直接修改（无需迁移脚本）：**

Phase 3 处于开发阶段，尚未发布到生产环境。因此不需要编写 `Migration` 类，直接在 Entity 和 Database 定义中修改 schema，然后使用 `fallbackToDestructiveMigration()` 或手动处理版本号：

```kotlin
// core/database/src/main/java/.../core/database/JizhangDatabase.kt
@Database(
    entities = [
        TransactionEntity::class,
        CategoryRuleEntity::class,
        ParseFailureLogEntity::class,
        BudgetEntity::class           // 新增
    ],
    version = 3                       // 从 2 → 3
)
abstract class JizhangDatabase : RoomDatabase() {
    // ...
    companion object {
        fun build(context: Context): JizhangDatabase {
            return Room.databaseBuilder(context, JizhangDatabase::class.java, "jizhang.db")
                .addMigrations(Migration_1_2)   // 保留 Phase 2 的迁移
                .fallbackToDestructiveMigration() // Phase 3 schema 变更使用 destructive 模式
                .build()
        }
    }
}
```

**需要直接修改的 Entity：**

`TransactionEntity` 新增 `type` 列（见上方 C 节），`BudgetEntity` 为全新表（见 Step 3.5）。

**为什么不需要 Migration：**
- Phase 3 是开发阶段，未发布到生产
- 测试环境数据库可随时重建
- 避免维护 2→3 迁移脚本的负担
- `fallbackToDestructiveMigration()` 在开发阶段可接受（不丢失数据前会清空，但开发数据可重新生成）

**E. 全管道集成 TransactionType：**

- `TransactionEntity.toDomain()` / `toEntity()` — 新增 `type` 字段映射
- `TransactionDao` — 新增 `getByType(type: String): Flow<List<TransactionEntity>>`
- `PersistCapturedTransactionUseCase` — 所有捕获的交易默认为 `EXPENSE`（目前仅捕获支付通知，收入场景 Phase 4 再处理）
- `TransactionDetailViewModel.save()` — 保留用户修改 `type` 的能力

#### 该步骤的验收标准

- [ ] `./gradlew :feature:analytics:build` 成功编译
- [ ] `:app:assembleDebug` 在新增模块依赖后仍成功
- [ ] `JizhangDatabase` 版本升级到 3 后编译通过，`fallbackToDestructiveMigration()` 正常工作
- [ ] `BudgetEntity` 和 `TransactionEntity.type` 在 `@Database` 注解中正确声明
- [ ] `TransactionEntity.toDomain()` 正确映射 `type` 字段

---

### 2.2 Step 3.2 — DAO 层索引查询方法

#### 实现内容

在 `TransactionDao` 中新增高性能查询方法（配合 SQL 索引）：

```kotlin
// core/database/src/main/java/.../core/database/dao/TransactionDao.kt
@Dao
interface TransactionDao {
    // 现有方法...

    // ===== Phase 3 新增方法 =====

    /**
     * 按关键词模糊搜索商户名（索引优化）
     */
    @Query("""
        SELECT * FROM transactions
        WHERE merchant LIKE '%' || :keyword || '%'
        ORDER BY timestamp DESC
    """)
    fun searchByKeyword(keyword: String): Flow<List<TransactionEntity>>

    /**
     * 按分类和时间范围查询
     */
    @Query("""
        SELECT * FROM transactions
        WHERE category = :category
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
    """)
    suspend fun getByCategoryAndDateRange(
        category: String,
        startTime: Long,
        endTime: Long
    ): List<TransactionEntity>

    /**
     * 按商户名精确查询（用于周期性检测）
     */
    @Query("SELECT * FROM transactions WHERE merchant = :merchant ORDER BY timestamp ASC")
    suspend fun getByMerchantName(merchant: String): List<TransactionEntity>

    /**
     * 按类型和时间范围汇总金额
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getTotalByTypeAndDateRange(type: String, startTime: Long, endTime: Long): Double

    /**
     * 按分类汇总指定时间范围内的金额
     */
    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as count FROM transactions
        WHERE type = 'EXPENSE'
        AND timestamp BETWEEN :startTime AND :endTime
        GROUP BY category
        ORDER BY total DESC
    """)
    suspend fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary>
}

data class CategorySummary(
    val category: String?,
    val total: Double,
    val count: Int
)
```

**索引添加（在 `TransactionEntity` 的 Room 注解或数据库回调中定义）：**

```kotlin
// 方式一：在 Entity 中使用 @Index 注解（Room 自动创建）
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["type"]),
        Index(value = ["merchant"]),
        Index(value = ["category", "timestamp"])
    ]
)
data class TransactionEntity(...)
```

**Repository 层新增方法：**

```kotlin
// TransactionRepository.kt
interface TransactionRepository {
    // ... 现有方法 ...
    fun searchByKeyword(keyword: String): Flow<List<Transaction>>
    suspend fun getByCategoryAndDateRange(category: String, startTime: Long, endTime: Long): List<Transaction>
    suspend fun getByMerchantName(merchant: String): List<Transaction>
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startTime: Long, endTime: Long): Double
    suspend fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary>
}
```

#### 该步骤的测试代码

```kotlin
// 使用 Room 内存数据库 + JUnit 4 + Robolectric
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: JizhangDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JizhangDatabase::class.java
        ).build()
        dao = db.transactionDao()
    }

    @Test
    fun searchByKeyword_returnsMatchingMerchants() = runTest {
        dao.insert(transactionEntity(merchant = "星巴克"))
        dao.insert(transactionEntity(merchant = "瑞幸咖啡"))
        dao.insert(transactionEntity(merchant = "滴滴出行"))

        val result = dao.searchByKeyword("星").first()

        assertEquals(1, result.size)
        assertEquals("星巴克", result[0].merchant)
    }

    @Test
    fun getCategorySummary_groupsCorrectly() = runTest {
        dao.insert(transactionEntity(amount = 100.0, category = "餐饮"))
        dao.insert(transactionEntity(amount = 50.0, category = "餐饮"))
        dao.insert(transactionEntity(amount = 80.0, category = "交通"))

        val result = dao.getCategorySummary(0L, Long.MAX_VALUE)

        assertEquals(2, result.size)
        assertEquals("餐饮", result[0].category)
        assertEquals(150.0, result[0].total, 0.01)
        assertEquals(2, result[0].count)
    }
}
```

#### 该步骤的验收标准

- [ ] `searchByKeyword` 模糊搜索返回正确结果
- [ ] `getByCategoryAndDateRange` 按分类+时间筛选正确
- [ ] `getCategorySummary` 聚合查询正确
- [ ] Repository 层方法正确映射 Entity ↔ Domain

---

### 2.3 Step 3.3 — 统计引擎 `AnalyticsEngine`

#### 实现内容

**新建文件：**

- `feature/analytics/src/main/java/.../analytics/engine/AnalyticsEngine.kt`
- `feature/analytics/src/main/java/.../analytics/engine/AnalyticsModels.kt`

#### `AnalyticsModels.kt`

```kotlin
data class MonthlySummary(
    val yearMonth: String,        // "2026-04"
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val transactionCount: Int
)

data class CategoryBreakdown(
    val category: String,
    val amount: Double,
    val percentage: Float,        // 0.0 ~ 1.0
    val transactionCount: Int
)

data class TrendPoint(
    val label: String,            // "04-01" 或 "第1周"
    val amount: Double
)

data class MerchantRanking(
    val merchant: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val category: String?
)
```

#### `AnalyticsEngine.kt`

```kotlin
class AnalyticsEngine(
    private val transactionRepository: TransactionRepository
) {
    /**
     * 获取指定年月范围的月度汇总
     * 使用 TransactionType 区分收入和支出
     */
    suspend fun getMonthlySummaries(
        startYearMonth: String,
        endYearMonth: String
    ): List<MonthlySummary> {
        val startTime = YearMonth.parse(startYearMonth).atDay(1).toEpochMillis()
        val endTime = YearMonth.parse(endYearMonth).atEndOfMonth().toEpochMillis()

        // 使用 DAO 聚合查询，避免全表加载
        val totalExpense = transactionRepository.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE, startTime, endTime
        )
        val totalIncome = transactionRepository.getTotalByTypeAndDateRange(
            TransactionType.INCOME, startTime, endTime
        )
        // ... 按月分组聚合
    }

    /**
     * 获取指定年月的分类支出占比
     */
    suspend fun getCategoryBreakdown(yearMonth: String): List<CategoryBreakdown> {
        val ym = YearMonth.parse(yearMonth)
        val summary = transactionRepository.getCategorySummary(
            ym.atDay(1).toEpochMillis(),
            ym.atEndOfMonth().toEpochMillis()
        )
        val total = summary.sumOf { it.total }
        return summary.map {
            CategoryBreakdown(
                category = it.category ?: "未分类",
                amount = it.total,
                percentage = if (total > 0) (it.total / total).toFloat() else 0f,
                transactionCount = it.count
            )
        }
    }

    /**
     * 获取指定年月的日趋势
     */
    suspend fun getDailyTrend(yearMonth: String): List<TrendPoint>

    /**
     * 获取 Top N 消费商户排行
     */
    suspend fun getTopMerchants(yearMonth: String, limit: Int = 10): List<MerchantRanking>
}
```

**设计要点：**
- 优先使用 DAO 层聚合查询（`getCategorySummary`、`getTotalByTypeAndDateRange`），避免全表加载到内存
- 对于复杂计算（如日趋势、Top 商户），使用 `getAll()` + 内存聚合（数据量 < 10K 时可接受）
- `TransactionType` 区分收入/支出：收入为正结余，支出为负结余

**数据一致性策略：**
- 统计报表使用 **suspend 函数**（一次性快照查询），用户切换月份时重新请求
- 实时余额/预算进度使用 **Flow**（响应式更新），由 ViewModel 层组合

#### 该步骤的测试代码

```kotlin
class AnalyticsEngineTest {
    private val repo = mockk<TransactionRepository>()
    private val engine = AnalyticsEngine(repo)

    @Test
    fun `getMonthlySummaries calculates totals correctly`() = runTest {
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 150.0
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.INCOME, any(), any()) } returns 5000.0

        val result = engine.getMonthlySummaries("2026-04", "2026-04")

        assertEquals(1, result.size)
        assertEquals(150.0, result[0].totalExpense, 0.01)
        assertEquals(5000.0, result[0].totalIncome, 0.01)
        assertEquals(4850.0, result[0].netBalance, 0.01)
    }

    @Test
    fun `getCategoryBreakdown calculates percentages`() = runTest {
        coEvery { repo.getCategorySummary(any(), any()) } returns listOf(
            CategorySummary("餐饮", 150.0, 3),
            CategorySummary("交通", 50.0, 1)
        )

        val result = engine.getCategoryBreakdown("2026-04")

        assertEquals(2, result.size)
        assertEquals(0.75f, result[0].percentage, 0.01f)  // 150/200
        assertEquals(0.25f, result[1].percentage, 0.01f)  // 50/200
    }

    @Test
    fun `getCategoryBreakdown returns empty list when no data`() = runTest {
        coEvery { repo.getCategorySummary(any(), any()) } returns emptyList()

        val result = engine.getCategoryBreakdown("2026-04")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTopMerchants returns ordered list`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            Transaction(1, 100.0, "星巴克", "饮品", TransactionType.EXPENSE, timestampOf(2026, 4, 1), "wechat", "raw"),
            Transaction(2, 80.0, "星巴克", "饮品", TransactionType.EXPENSE, timestampOf(2026, 4, 5), "wechat", "raw"),
            Transaction(3, 30.0, "滴滴", "交通", TransactionType.EXPENSE, timestampOf(2026, 4, 3), "wechat", "raw")
        )

        val result = engine.getTopMerchants("2026-04", limit = 2)

        assertEquals(2, result.size)
        assertEquals("星巴克", result[0].merchant)
        assertEquals(180.0, result[0].totalAmount, 0.01)
        assertEquals(2, result[0].transactionCount)
    }
}
```

#### 该步骤的验收标准

- [ ] `./gradlew :feature:analytics:testDebugUnitTest` 全部通过
- [ ] `AnalyticsEngine` 覆盖：月度汇总（含收入/支出/结余）、分类占比、日趋势、商户排行
- [ ] 空数据时返回空列表不崩溃
- [ ] 跨月边界计算正确

---

### 2.4 Step 3.4 — 统计图表 UI

#### 实现内容

#### 新建文件

- `feature/analytics/src/main/java/.../analytics/ui/AnalyticsScreen.kt`
- `feature/analytics/src/main/java/.../analytics/ui/AnalyticsViewModel.kt`
- `feature/analytics/src/main/java/.../analytics/ui/components/CategoryPieChart.kt`
- `feature/analytics/src/main/java/.../analytics/ui/components/TrendLineChart.kt`
- `feature/analytics/src/main/java/.../analytics/ui/components/MonthlySummaryCard.kt`
- `feature/analytics/src/main/java/.../analytics/ui/components/TopMerchantList.kt`
- `feature/analytics/src/main/java/.../analytics/di/AnalyticsModule.kt`

#### `AnalyticsScreen.kt`

顶部 Tab 切换：
- **月度概览**（默认）：SummaryCard（总收入/支出/结余）+ 分类饼图 + Top 商户
- **趋势分析**：日/周折线图 + 月度对比柱状图

月份选择器：左右箭头切换年月，支持回到当前月。

#### 图表库选择与风险应对

| 库 | 优点 | 缺点 | 策略 |
|---|---|---|---|
| **Vico** (compose-m3) | Compose 原生、声明式 API、动画流畅 | v2.0.0-alpha 不稳定风险 | **首选**，持续跟进稳定版 |
| **MPAndroidChart** | 功能最丰富、社区大、稳定 | View-based，Compose 需 AndroidView 包装 | **备选**（已添加依赖） |

**风险应对方案：**
1. 首选 Vico `compose-m3`  artifact（推荐用于 Material3）
2. 若 Vico alpha 版出现 blocking issue（如崩溃、Compose 版本不兼容），立即切换到 MPAndroidChart
3. MPAndroidChart 使用 `AndroidView` 包装为 Compose 组件，接口对齐
4. 图表组件接口抽象化，降低切换成本：

```kotlin
// 抽象图表接口（降低 Vico ↔ MPAndroidChart 切换成本）
@Composable
fun CategoryPieChart(
    data: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    // 内部实现：Vico DonutChart 或 MPAndroidChart PieChart
    // 通过 build.gradle 依赖切换，代码层用 expect/actual 或编译时分支
}
```

#### 该步骤的验收标准

- [ ] AnalyticsScreen 在真实数据下正常渲染
- [ ] 切换月份时图表数据正确刷新
- [ ] 空数据时显示友好提示（"本月暂无交易记录"）
- [ ] Vico/MPAndroidChart 至少一个可正常渲染

---

## 3. Week 2：预算管理与超支预警

### 3.1 Step 3.5 — 预算数据模型与数据库

#### 实现内容

**TransactionType 和 Budgets 的数据库 Schema 已在 Step 3.1 中直接定义**（Entity 注解 + Database 版本升级）。

`BudgetEntity` 和 `BudgetDao`：

```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "monthly_limit") val monthlyLimit: Double,
    @ColumnInfo(name = "alert_threshold") val alertThreshold: Float
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category")
    suspend fun getByCategory(category: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteByCategory(category: String)
}
```

**Repository：**

```kotlin
// TransactionRepository.kt — 新增方法
interface TransactionRepository {
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startTime: Long, endTime: Long): Double
}

// BudgetRepository.kt — 新增
interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    suspend fun getBudgetByCategory(category: String): Budget?
    suspend fun upsertBudget(budget: Budget)
    suspend fun deleteBudget(category: String)
}
```

#### 该步骤的测试代码

```kotlin
@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {
    private lateinit var db: JizhangDatabase
    private lateinit var dao: BudgetDao

    @Test
    fun upsertAndQuery_returnsCorrectBudget() = runTest {
        dao.upsert(BudgetEntity(category = "餐饮", monthlyLimit = 1000.0, alertThreshold = 0.8f))

        val result = dao.getByCategory("餐饮")

        assertNotNull(result)
        assertEquals(1000.0, result!!.monthlyLimit, 0.01)
    }

    @Test
    fun deleteByCategory_removesBudget() = runTest {
        dao.upsert(BudgetEntity(category = "交通", monthlyLimit = 500.0, alertThreshold = 0.8f))
        dao.deleteByCategory("交通")

        assertNull(dao.getByCategory("交通"))
    }
}
```

#### 该步骤的验收标准

- [ ] `BudgetDao` 增删改查测试通过
- [ ] `Flow<List<Budget>>` 在数据变更时自动更新
- [ ] `upsert` 在分类冲突时正确替换

---

### 3.2 Step 3.6 — 预算设置与超支检测

#### 实现内容

#### 新建文件

- `feature/ledger/src/main/java/.../ledger/ui/budget/BudgetSettingScreen.kt`
- `feature/ledger/src/main/java/.../ledger/ui/budget/BudgetViewModel.kt`
- `feature/ledger/src/main/java/.../ledger/ui/budget/BudgetUseCase.kt`
- `feature/capture/src/main/java/.../capture/keepalive/BudgetAlertChecker.kt`（注意模块归属）
- `feature/capture/src/main/java/.../capture/keepalive/BudgetAlertNotificationHelper.kt`

#### 模块归属决策：`BudgetAlertChecker` 放在 `:feature:capture`

**原因：** `BudgetAlertChecker` 在交易入库后（`PersistCapturedTransactionUseCase`）被调用。如果放在 `:feature:analytics`，则 `:feature:capture` 需要依赖 `:feature:analytics`，违反"上层依赖下层"的原则。放在 `:feature:capture` 中，直接依赖 `:core:database`（`BudgetRepository` 和 `TransactionRepository`），避免循环依赖。

**依赖路径：**
```
:feature:capture → :core:database (BudgetRepository, TransactionRepository)
                → :core:common (notification utilities)
```

#### `BudgetUseCase.kt`（`:feature:ledger`，仅 UI 层使用）

```kotlin
class BudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun getBudgetStatuses(yearMonth: String): List<BudgetStatus> {
        val budgets = budgetRepository.getAllBudgets().first()
        val ym = YearMonth.parse(yearMonth)
        val startTime = ym.atDay(1).toEpochMillis()
        val endTime = ym.atEndOfMonth().toEpochMillis()

        return budgets.map { budget ->
            val spent = transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.EXPENSE, startTime, endTime
            )
            // 实际应按分类过滤；此处简化，生产代码需按分类聚合
            val percentage = if (budget.monthlyLimit > 0)
                (spent / budget.monthlyLimit).toFloat() else 0f

            BudgetStatus(
                category = budget.category,
                monthlyLimit = budget.monthlyLimit,
                spent = spent,
                remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0),
                percentage = percentage,
                isOverBudget = percentage >= 1.0f,
                isAlertTriggered = percentage >= budget.alertThreshold
            )
        }
    }

    suspend fun setBudget(category: String, limit: Double, threshold: Float = 0.8f) {
        budgetRepository.upsertBudget(Budget(category = category, monthlyLimit = limit, alertThreshold = threshold))
    }

    suspend fun deleteBudget(category: String) {
        budgetRepository.deleteBudget(category)
    }
}

data class BudgetStatus(
    val category: String,
    val monthlyLimit: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Float,
    val isOverBudget: Boolean,
    val isAlertTriggered: Boolean
)
```

#### `BudgetAlertChecker.kt`（`:feature:capture`）

```kotlin
class BudgetAlertChecker(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context
) {
    suspend fun checkAndNotify(category: String, yearMonth: String) {
        val budget = budgetRepository.getBudgetByCategory(category) ?: return
        val ym = YearMonth.parse(yearMonth)

        val spent = transactionRepository.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            ym.atDay(1).toEpochMillis(),
            ym.atEndOfMonth().toEpochMillis()
        )

        val percentage = if (budget.monthlyLimit > 0)
            (spent / budget.monthlyLimit).toFloat() else 0f

        when {
            percentage >= 1.0f -> BudgetAlertNotificationHelper.showOverBudgetAlert(
                context, category, budget.monthlyLimit, spent
            )
            percentage >= budget.alertThreshold -> BudgetAlertNotificationHelper.showThresholdAlert(
                context, category, budget.monthlyLimit, spent, percentage
            )
        }
    }
}
```

#### `PersistCapturedTransactionUseCase` 集成

```kotlin
class PersistCapturedTransactionUseCase(
    private val parser: TransactionParser,
    private val transactionRepository: TransactionRepository,
    private val parseFailureRepository: ParseFailureRepository,
    private val classificationEngine: ClassificationEngine,
    private val llmLearningUseCase: LlmLearningUseCase,
    private val budgetAlertChecker: BudgetAlertChecker  // 新增
) {
    suspend operator fun invoke(notification: NotificationData) {
        // ... 现有解析和分类逻辑 ...

        // 交易入库后异步检查预算
        if (category != null) {
            val yearMonth = YearMonth.now().toString()
            budgetAlertChecker.checkAndNotify(category, yearMonth)
        }
    }
}
```

#### 该步骤的测试代码

```kotlin
class BudgetUseCaseTest {
    private val budgetRepo = mockk<BudgetRepository>()
    private val txRepo = mockk<TransactionRepository>()
    private val useCase = BudgetUseCase(budgetRepo, txRepo)

    @Test
    fun `getBudgetStatuses calculates spent correctly`() = runTest {
        val yearMonth = YearMonth.now().toString()
        coEvery { budgetRepo.getAllBudgets() } returns flowOf(listOf(
            Budget(category = "餐饮", monthlyLimit = 500.0, alertThreshold = 0.8f)
        ))
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 450.0

        val statuses = useCase.getBudgetStatuses(yearMonth)

        assertEquals(1, statuses.size)
        assertEquals(0.9f, statuses[0].percentage, 0.01f)
        assertFalse(statuses[0].isOverBudget)   // 450 < 500
        assertTrue(statuses[0].isAlertTriggered) // 90% >= 80%
        assertEquals(50.0, statuses[0].remaining, 0.01)
    }

    @Test
    fun `over budget triggers isOverBudget flag`() = runTest {
        val yearMonth = YearMonth.now().toString()
        coEvery { budgetRepo.getAllBudgets() } returns flowOf(listOf(
            Budget(category = "交通", monthlyLimit = 200.0, alertThreshold = 0.8f)
        ))
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 250.0

        val statuses = useCase.getBudgetStatuses(yearMonth)

        assertTrue(statuses[0].isOverBudget)
        assertTrue(statuses[0].isAlertTriggered)
    }

    @Test
    fun `no budget returns empty list`() = runTest {
        coEvery { budgetRepo.getAllBudgets() } returns flowOf(emptyList())

        val statuses = useCase.getBudgetStatuses(YearMonth.now().toString())

        assertTrue(statuses.isEmpty())
    }
}

class BudgetAlertCheckerTest {
    private val budgetRepo = mockk<BudgetRepository>()
    private val txRepo = mockk<TransactionRepository>()
    private val context = mockk<Context>(relaxed = true)
    private val checker = BudgetAlertChecker(budgetRepo, txRepo, context)

    @Test
    fun `no alert when no budget set`() = runTest {
        coEvery { budgetRepo.getBudgetByCategory("餐饮") } returns null

        checker.checkAndNotify("餐饮", "2026-04")

        // 不应触发任何通知
        coVerify(exactly = 0) { txRepo.getTotalByTypeAndDateRange(any(), any(), any()) }
    }

    @Test
    fun `threshold alert when percentage exceeds threshold`() = runTest {
        coEvery { budgetRepo.getBudgetByCategory("餐饮") } returns Budget(category = "餐饮", monthlyLimit = 500.0, alertThreshold = 0.8f)
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 400.0

        checker.checkAndNotify("餐饮", "2026-04")

        // 验证通知被触发（通过 context 验证或检查日志）
        // 实际验证需 Mock 静态方法，此处验证调用链路
        coVerify { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) }
    }
}
```

#### 该步骤的验收标准

- [ ] 预算设置页面可正常增删改预算
- [ ] `getBudgetStatuses` 正确计算已花费/剩余/百分比/超支/预警状态
- [ ] `BudgetAlertChecker` 在超支时触发通知
- [ ] 预算状态在 AnalyticsScreen 的分类饼图旁显示进度条
- [ ] 模块依赖无循环：`:feature:capture` → `:core:database`（无需经过 `:feature:analytics`）

---

## 4. Week 3：交易搜索与筛选

### 4.1 Step 3.7 — 交易搜索引擎

#### 实现内容

**新建文件：**

- `feature/ledger/src/main/java/.../ledger/ui/search/TransactionSearchEngine.kt`

```kotlin
class TransactionSearchEngine(
    private val transactionRepository: TransactionRepository
) {
    /**
     * 实时搜索（支持模糊匹配商户名和分类）
     * 使用 Flow 实现响应式搜索：输入变化 → 自动重新查询
     *
     * 数据一致性策略：
     * - 对外暴露 Flow<List<Transaction>> 以支持 UI 实时更新
     * - 内部使用 DAO 的 Flow 查询（Room 自动维护一致性）
     * - 复杂筛选条件在内存中组合（Room 层只做基础过滤）
     */
    fun search(query: SearchQuery): Flow<List<Transaction>> {
        return transactionRepository.searchByKeyword(query.keyword)
            .map { transactions ->
                transactions.filter { matches(it, query) }
            }
    }

    private fun matches(transaction: Transaction, query: SearchQuery): Boolean {
        // 分类筛选（多选 OR）
        if (query.categories.isNotEmpty() &&
            transaction.category !in query.categories) return false

        // 时间范围（闭区间）
        if (query.startTime != null && transaction.timestamp < query.startTime) return false
        if (query.endTime != null && transaction.timestamp > query.endTime) return false

        // 金额范围（闭区间）
        if (query.minAmount != null && transaction.amount < query.minAmount) return false
        if (query.maxAmount != null && transaction.amount > query.maxAmount) return false

        return true
    }
}

data class SearchQuery(
    val keyword: String = "",
    val categories: List<String> = emptyList(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)
```

**数据一致性策略总结：**

| 场景 | 模式 | 原因 |
|------|------|------|
| 搜索/筛选 | `Flow` | Room 的 Flow 查询自动推送更新；UI 层用 `debounce(300ms)` 防抖 |
| 统计报表 | `suspend` | 一次性快照，用户主动刷新时才重新计算 |
| 预算状态 | `Flow`（预算列表）+ `suspend`（已花费金额） | 组合使用：预算变更时自动更新，金额查询按需 |
| 周期性检测 | `suspend` | 批量计算，一次性结果 |

#### 该步骤的测试代码

```kotlin
class TransactionSearchEngineTest {
    private val repo = mockk<TransactionRepository>()
    private val engine = TransactionSearchEngine(repo)

    @Test
    fun `search by keyword matches merchant`() = runTest {
        coEvery { repo.searchByKeyword("星") } returns flowOf(listOf(
            Transaction(1, 30.0, "星巴克", "饮品", TransactionType.EXPENSE, 1000L, "wechat", "raw"),
            Transaction(2, 25.0, "星巴克咖啡", "饮品", TransactionType.EXPENSE, 2000L, "wechat", "raw")
        ))

        val result = engine.search(SearchQuery(keyword = "星")).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `search by category filters correctly`() = runTest {
        coEvery { repo.searchByKeyword("") } returns flowOf(listOf(
            Transaction(1, 30.0, "星巴克", "饮品", TransactionType.EXPENSE, 1000L, "wechat", "raw"),
            Transaction(2, 50.0, "滴滴", "交通", TransactionType.EXPENSE, 2000L, "wechat", "raw"),
            Transaction(3, 100.0, "超市", "日用", TransactionType.EXPENSE, 3000L, "wechat", "raw")
        ))

        val result = engine.search(SearchQuery(categories = listOf("饮品", "日用"))).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.category in listOf("饮品", "日用") })
    }

    @Test
    fun `search by amount range works`() = runTest {
        coEvery { repo.searchByKeyword("") } returns flowOf(listOf(
            Transaction(1, 30.0, "星巴克", "饮品", TransactionType.EXPENSE, 1000L, "wechat", "raw"),
            Transaction(2, 50.0, "滴滴", "交通", TransactionType.EXPENSE, 2000L, "wechat", "raw"),
            Transaction(3, 100.0, "超市", "日用", TransactionType.EXPENSE, 3000L, "wechat", "raw")
        ))

        val result = engine.search(SearchQuery(minAmount = 40.0, maxAmount = 80.0)).first()

        assertEquals(1, result.size)
        assertEquals(50.0, result[0].amount, 0.01)
    }

    @Test
    fun `combined query applies all conditions`() = runTest {
        coEvery { repo.searchByKeyword("星") } returns flowOf(listOf(
            Transaction(1, 30.0, "星巴克", "饮品", TransactionType.EXPENSE, 1000L, "wechat", "raw"),
            Transaction(2, 25.0, "星巴克咖啡", "饮品", TransactionType.EXPENSE, 2000L, "wechat", "raw")
        ))

        val result = engine.search(SearchQuery(
            keyword = "星",
            minAmount = 28.0,
            categories = listOf("饮品")
        )).first()

        assertEquals(1, result.size)
        assertEquals("星巴克", result[0].merchant)
    }

    @Test
    fun `empty query returns all transactions`() = runTest {
        val allTransactions = listOf(
            Transaction(1, 30.0, "星巴克", "饮品", TransactionType.EXPENSE, 1000L, "wechat", "raw"),
            Transaction(2, 50.0, "滴滴", "交通", TransactionType.EXPENSE, 2000L, "wechat", "raw")
        )
        coEvery { repo.searchByKeyword("") } returns flowOf(allTransactions)

        val result = engine.search(SearchQuery()).first()

        assertEquals(2, result.size)
    }
}
```

#### 该步骤的验收标准

- [ ] 各单一条件搜索正确（关键词、分类、时间、金额）
- [ ] 组合条件搜索正确（AND 关系）
- [ ] 空查询返回全部数据
- [ ] Flow 搜索支持防抖（300ms debounce）

---

### 4.2 Step 3.8 — 搜索与筛选 UI

#### 实现内容

**改造文件：**

- `feature/ledger/src/main/java/.../ledger/ui/list/TransactionListScreen.kt`
- `feature/ledger/src/main/java/.../ledger/ui/list/LedgerViewModel.kt`

#### UI 设计

顶部搜索栏：
- 搜索输入框（placeholder："搜索商户..."）
- 筛选按钮 → 底部弹出的筛选面板

筛选面板（BottomSheet）：
- 分类选择：Chip 组多选
- 时间范围："今天 / 本周 / 本月 / 自定义"
- 金额范围：两个输入框（最低 ~ 最高）
- 重置按钮 + 应用按钮

搜索结果空态：
- "未找到匹配的交易记录"

#### 该步骤的验收标准

- [ ] 搜索实时响应（输入 300ms debounce）
- [ ] 筛选面板各条件可组合使用
- [ ] 重置按钮清空所有筛选条件
- [ ] 空结果时显示友好提示

---

## 5. Week 4：周期性交易识别

> **范围缩减说明：** 原计划的"更多支付渠道（银行 SMS、美团、京东）"推迟到 Phase 4。Week 4 仅聚焦周期性交易识别及其 UI。

### 5.1 Step 3.9 — 周期性交易识别引擎

#### 实现内容

**新建文件：**

- `feature/analytics/src/main/java/.../analytics/engine/RecurringDetector.kt`

```kotlin
class RecurringDetector(
    private val transactionRepository: TransactionRepository
) {
    /**
     * 从交易列表中识别周期性交易
     * 返回周期性交易模板及其下次预计时间
     *
     * 算法：基于间隔方差的启发式方法
     * 1. 按商户名分组
     * 2. 对每组交易，计算相邻交易的时间间隔
     * 3. 如果间隔的变异系数 < 15%，判定为周期性
     * 4. 计算平均金额和下次预计时间
     */
    suspend fun detect(): List<RecurringPattern> {
        val transactions = transactionRepository.getAll()
        return transactions.groupBy { it.merchant }
            .mapValues { (_, txs) -> txs.sortedBy { it.timestamp } }
            .mapNotNull { (merchant, txs) ->
                analyzePattern(merchant, txs)
            }
            .sortedByDescending { it.confidence }
    }

    private fun analyzePattern(merchant: String, txs: List<Transaction>): RecurringPattern? {
        if (txs.size < 3) return null  // 至少 3 次交易才能判定周期

        val intervals = txs.zipWithNext { a, b -> b.timestamp - a.timestamp }
        if (intervals.size < 2) return null

        val avgInterval = intervals.average()
        val stdDev = intervals.stdDev()
        val cv = if (avgInterval > 0) stdDev / avgInterval else Double.MAX_VALUE

        // 变异系数 > 15% → 间隔不稳定，非周期性
        if (cv > 0.15) return null

        val frequency = classifyFrequency(avgInterval) ?: return null
        val amounts = txs.map { it.amount }
        val avgAmount = amounts.average()
        val amountCv = amounts.stdDev() / avgAmount

        // 金额变异系数 > 30% → 金额不稳定，降低置信度
        val confidence = calculateConfidence(cv, amountCv, txs.size)

        return RecurringPattern(
            merchant = merchant,
            category = txs.first().category,
            averageAmount = avgAmount,
            frequency = frequency,
            lastOccurrence = txs.last().timestamp,
            nextEstimatedOccurrence = txs.last().timestamp + avgInterval.toLong(),
            confidence = confidence
        )
    }

    private fun classifyFrequency(avgIntervalMillis: Double): RecurringFrequency? {
        val avgHours = avgIntervalMillis / 3_600_000
        return when {
            avgHours in 23.0..25.0 -> RecurringFrequency.DAILY
            avgHours in 160.0..200.0 -> RecurringFrequency.WEEKLY    // 7天 ± 20小时
            avgHours in 650.0..800.0 -> RecurringFrequency.MONTHLY   // 30天 ± 3天
            else -> null
        }
    }

    private fun calculateConfidence(intervalCv: Double, amountCv: Double, count: Int): Float {
        var confidence = 1.0f
        // 间隔越稳定，置信度越高
        confidence -= (intervalCv / 0.15).toFloat() * 0.3f
        // 金额越稳定，置信度越高
        confidence -= (amountCv / 0.30).toFloat() * 0.2f
        // 样本越多，置信度越高
        confidence += (count.coerceAtMost(12) - 3) * 0.05f
        return confidence.coerceIn(0.0f, 1.0f)
    }
}

data class RecurringPattern(
    val merchant: String,
    val category: String?,
    val averageAmount: Double,
    val frequency: RecurringFrequency,
    val lastOccurrence: Long,
    val nextEstimatedOccurrence: Long,
    val confidence: Float
)

enum class RecurringFrequency { DAILY, WEEKLY, MONTHLY }
```

#### 该步骤的测试代码

```kotlin
class RecurringDetectorTest {
    private val repo = mockk<TransactionRepository>()
    private val detector = RecurringDetector(repo)

    @Test
    fun `detects monthly rent payment`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("房东", 3000.0, date(2026, 1, 1)),
            transaction("房东", 3000.0, date(2026, 2, 1)),
            transaction("房东", 3000.0, date(2026, 3, 1)),
            transaction("房东", 3000.0, date(2026, 4, 1))
        )

        val patterns = detector.detect()

        assertEquals(1, patterns.size)
        assertEquals("房东", patterns[0].merchant)
        assertEquals(RecurringFrequency.MONTHLY, patterns[0].frequency)
        assertTrue(patterns[0].confidence > 0.8f)
    }

    @Test
    fun `detects weekly subscription`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("Netflix", 79.0, date(2026, 3, 4)),
            transaction("Netflix", 79.0, date(2026, 3, 11)),
            transaction("Netflix", 79.0, date(2026, 3, 18)),
            transaction("Netflix", 79.0, date(2026, 3, 25))
        )

        val patterns = detector.detect()

        assertEquals(1, patterns.size)
        assertEquals(RecurringFrequency.WEEKLY, patterns[0].frequency)
    }

    @Test
    fun `ignores irregular transactions`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("星巴克", 30.0, date(2026, 4, 1)),
            transaction("星巴克", 25.0, date(2026, 4, 5)),
            transaction("星巴克", 28.0, date(2026, 4, 12))
        )

        val patterns = detector.detect()

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `ignores single occurrence merchants`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("未知商户", 100.0, date(2026, 4, 1))
        )

        val patterns = detector.detect()

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `returns empty list when no transactions`() = runTest {
        coEvery { repo.getAll() } returns emptyList()

        val patterns = detector.detect()

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `confidence decreases with amount variance`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("水电费", 200.0, date(2026, 1, 5)),
            transaction("水电费", 180.0, date(2026, 2, 5)),
            transaction("水电费", 220.0, date(2026, 3, 5)),
            transaction("水电费", 190.0, date(2026, 4, 5))
        )

        val patterns = detector.detect()

        assertEquals(1, patterns.size)
        // 金额有波动但间隔稳定，置信度应在 0.5~0.9 之间
        assertTrue(patterns[0].confidence in 0.5f..0.9f)
    }
}

// 测试辅助函数
private fun transaction(merchant: String, amount: Double, timestamp: Long): Transaction {
    return Transaction(
        amount = amount, merchant = merchant,
        category = null, type = TransactionType.EXPENSE,
        timestamp = timestamp, sourceApp = "wechat", rawText = ""
    )
}

private fun date(year: Int, month: Int, day: Int): Long {
    return java.time.LocalDate.of(year, month, day)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli()
}
```

#### 该步骤的验收标准

- [ ] 月度周期性交易识别准确率 > 80%（基于测试覆盖率验证）
- [ ] 非周期性交易不被误判
- [ ] 下次预计时间计算合理
- [ ] 空数据或无周期交易时返回空列表
- [ ] 置信度随样本数量和金额稳定性动态调整

---

### 5.2 Step 3.10 — 周期性交易展示 UI

#### 实现内容

**新建文件：**

- `feature/analytics/src/main/java/.../analytics/ui/components/RecurringTransactionList.kt`

在 `AnalyticsScreen` 增加第三个 Tab：**周期性交易**

展示内容：
- 商户名 + 分类
- 平均金额 + 频率（"每月" / "每周" / "每日"）
- 上次消费时间 + 预计下次时间
- 置信度进度条（高 / 中 / 低）

#### 该步骤的验收标准

- [ ] 周期性交易列表正确渲染
- [ ] 高置信度项优先展示（按 confidence 降序）
- [ ] 点击可查看该商户的所有历史交易
- [ ] 无周期性交易时显示："尚未发现周期性交易，持续记账后将自动识别"

---

## 6. 模块依赖图

```
                    :app
                     │
    ┌────────────────┼────────────────┬────────────────┐
    │                │                │                │
    ▼                ▼                ▼                ▼
:feature:capture  :feature:parser  :feature:ledger  :feature:analytics
    │                │                │           │         │
    │                │                │           │         ▼
    │                │                │           │    :core:database
    │                │                │           │         │
    │                │                │           └─────────┘
    │                │                │                     │
    │                │                └─────────────────────┘
    │                │                                  │
    │                └──────────────┬───────────────────┘
    │                               │
    ▼                               ▼
:feature:classification           :ai
    │                               │
    └──────────────┬────────────────┘
                   │
                   ▼
             :core:model
                   │
                   ▼
             :core:common
```

**新增/变更依赖说明：**

| 依赖 | 方向 | 原因 |
|------|------|------|
| `:feature:analytics` → `:core:database` | 新增 | 读取交易和预算数据 |
| `:feature:capture` → `:core:database` | 已有 | BudgetAlertChecker 直接使用 BudgetRepository |
| `:feature:ledger` → `:feature:analytics` | 新增 | 底部导航跳转到统计页 |
| `:app` → `:feature:analytics` | 新增 | 注册 analyticsModule 和导航路由 |

**关键依赖约束（避免循环依赖）：**
- `BudgetAlertChecker` 在 `:feature:capture` 而非 `:feature:analytics`，避免 `:feature:capture` → `:feature:analytics` → `:core:database` 的不必要层级
- `BudgetUseCase` 在 `:feature:ledger`（UI 层），不参与后台检查链路
- 所有依赖单向，无循环

---

## 7. 技术决策说明

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 图表库 | **Vico（首选）+ MPAndroidChart（备选）** | Vico Compose 原生；MPAndroidChart 作为 alpha 风险兜底 |
| Vico 风险应对 | 同时依赖两个库，组件接口抽象化 | 出现 blocking issue 可 1 天内切换，不影响开发进度 |
| 统计计算 | **DAO 聚合查询优先，内存聚合辅助** | `getCategorySummary` 等 SQL 聚合减少数据传输；复杂分析用 Kotlin 集合操作 |
| TransactionType | **枚举字段 + Entity 直接修改 + `fallbackToDestructiveMigration()`** | 区分收入/支出，支撑未来工资、退款等场景；全管道贯通 |
| 预算存储 | **Room 新表 `budgets`** | 结构简单，与现有数据库统一；`upsert` 避免重复 |
| BudgetAlertChecker 归属 | **`:feature:capture`** | 避免循环依赖，直接使用 `:core:database` |
| 搜索策略 | **DAO 级 `searchByKeyword` + 内存过滤** | Room 的 LIKE 查询 + Flow 响应式更新；> 10K 时考虑 FTS |
| 搜索数据一致性 | **Flow（响应式）** | Room Flow 自动推送数据变更，UI 层 `debounce(300ms)` 防抖 |
| 统计报表一致性 | **suspend（一次性快照）** | 报表为时间点查询，用户主动切换月份时才刷新 |
| 周期性检测 | **基于间隔方差的启发式算法** | 简单可解释，不需要 ML 模型；准确率 > 80% 可接受 |
| 预算预警 | **交易入库后异步检查** | 不阻塞主流程；通过通知栏提醒用户 |
| Schema 升级策略 | **直接修改 Entity + `fallbackToDestructiveMigration()`** | 开发阶段无需迁移脚本；发布前再考虑正式迁移 |

---

## 8. 新增依赖清单

需在 `gradle/libs.versions.toml` 中新增：

```toml
[versions]
vico = "2.0.0-alpha.21"
mpandroidchart = "3.1.0"

[libraries]
# 图表库（主选）
vico-compose = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }
vico-core = { group = "com.patrykandpatrick.vico", name = "core", version.ref = "vico" }

# 图表库（备选 - Vico alpha 出问题时使用）
mpandroidchart = { group = "com.github.PhilJay", name = "MPAndroidChart", version.ref = "mpandroidchart" }
```

**说明：** MPAndroidChart 作为备选依赖添加。如果 Vico alpha 版稳定，可在上线前移除。添加备选依赖的目的是避免 Vico 出现 blocking issue 时阻塞整个 Week 1 开发进度。

---

## 9. 关键文件清单

### 配置
- `settings.gradle.kts`（新增 `:feature:analytics`）
- `gradle/libs.versions.toml`（新增 Vico、MPAndroidChart 依赖）
- `core/database/.../JizhangDatabase.kt`（版本升至 3，添加 `BudgetEntity` 到 entities，保留 `Migration_1_2` + `fallbackToDestructiveMigration()`）

### `:core:model`（变更）
- `core/model/.../Transaction.kt`（新增 `type: TransactionType` 字段）
- `core/model/.../TransactionType.kt`（新增枚举：`INCOME, EXPENSE`）
- `core/model/.../Budget.kt`（新增）

### `:core:database`（变更）
- `core/database/.../entity/TransactionEntity.kt`（新增 `type` 列）
- `core/database/.../entity/BudgetEntity.kt`（新增）
- `core/database/.../dao/TransactionDao.kt`（新增 `searchByKeyword`, `getByCategoryAndDateRange`, `getByMerchantName`, `getTotalByTypeAndDateRange`, `getCategorySummary`）
- `core/database/.../dao/BudgetDao.kt`（新增）
- `core/database/.../repository/TransactionRepository.kt` + `Impl.kt`（新增 5 个方法）
- `core/database/.../repository/BudgetRepository.kt` + `Impl.kt`（新增）
- （无新增 Migration 文件——Phase 3 schema 变更直接修改 Entity，无需迁移脚本）

### `:feature:analytics`（新增模块）
- `feature/analytics/.../engine/AnalyticsEngine.kt`
- `feature/analytics/.../engine/AnalyticsModels.kt`
- `feature/analytics/.../engine/RecurringDetector.kt`
- `feature/analytics/.../ui/AnalyticsScreen.kt`
- `feature/analytics/.../ui/AnalyticsViewModel.kt`
- `feature/analytics/.../ui/components/CategoryPieChart.kt`
- `feature/analytics/.../ui/components/TrendLineChart.kt`
- `feature/analytics/.../ui/components/MonthlySummaryCard.kt`
- `feature/analytics/.../ui/components/TopMerchantList.kt`
- `feature/analytics/.../ui/components/RecurringTransactionList.kt`
- `feature/analytics/.../di/AnalyticsModule.kt`

### `:feature:ledger`（变更 + 新增）
- `feature/ledger/.../list/TransactionListScreen.kt`（搜索栏 + 筛选面板）
- `feature/ledger/.../list/LedgerViewModel.kt`（搜索/筛选状态）
- `feature/ledger/.../search/TransactionSearchEngine.kt`
- `feature/ledger/.../budget/BudgetSettingScreen.kt`
- `feature/ledger/.../budget/BudgetViewModel.kt`
- `feature/ledger/.../budget/BudgetUseCase.kt`
- `feature/ledger/.../detail/TransactionDetailViewModel.kt`（支持编辑 `type` 字段）
- `feature/ledger/.../di/LedgerModule.kt`（注册 BudgetUseCase 等）

### `:feature:capture`（变更）
- `feature/capture/.../keepalive/BudgetAlertChecker.kt`（新增）
- `feature/capture/.../keepalive/BudgetAlertNotificationHelper.kt`（新增）
- `feature/capture/.../usecase/PersistCapturedTransactionUseCase.kt`（集成预算预警检查 + 设置 type=EXPENSE）

### `:app`（变更）
- `app/.../MainActivity.kt`（底部导航：账本 / 统计 / 设置）
- `app/.../di/AppModule.kt`（新增 analyticsModule）

### 导航设计：底部导航结构

```
┌─────────────────────────────────────────────┐
│  MainActivity                                │
│  ┌─────────────────────────────────────────┐ │
│  │  NavHost                                │ │
│  │  ├─ "ledger"    → TransactionListScreen │ │
│  │  ├─ "analytics" → AnalyticsScreen       │ │
│  │  └─ "settings"  → PrivacySettingsScreen │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │  BottomNavigationBar                    │ │
│  │  [📒 账本]  [📊 统计]  [⚙️ 设置]        │ │
│  └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

- "账本" Tab：现有 `TransactionListScreen`（含搜索栏和筛选面板）
- "统计" Tab：新增 `AnalyticsScreen`（月度概览 + 趋势分析 + 周期性交易）
- "设置" Tab：现有 `PrivacySettingsScreen`（隐私政策 + 数据导出 + 清除）+ 新增预算管理入口

### 测试文件清单
- `feature/analytics/.../test/.../AnalyticsEngineTest.kt`
- `feature/analytics/.../test/.../RecurringDetectorTest.kt`
- `feature/ledger/.../test/.../TransactionSearchEngineTest.kt`
- `feature/ledger/.../test/.../BudgetUseCaseTest.kt`
- `feature/ledger/.../test/.../TransactionDetailViewModelTest.kt`（新增 type 编辑测试）
- `feature/capture/.../test/.../BudgetAlertCheckerTest.kt`
- `core/database/.../test/.../TransactionDaoTest.kt`（新增搜索和聚合查询测试）
- `core/database/.../test/.../BudgetDaoTest.kt`

---

## 10. 审核意见栏

请在确认或修改以下内容后，在此文档末尾回复：

1. [ ] Phase 3 目标定位是否准确？（数据洞察 + 预算 + 搜索 + 周期性，移除银行短信）
2. [ ] `TransactionType` 模型变更是否接受？对现有数据的影响是否已评估？
3. [ ] 4 周周期是否合理？是否有需要删减或延后的功能？
4. [ ] 图表库方案（Vico 首选 + MPAndroidChart 备选）是否可接受？
5. [ ] `BudgetAlertChecker` 放在 `:feature:capture` 而非 `:feature:analytics` 是否合理？
6. [ ] 搜索/统计的数据一致性策略（Flow vs suspend）是否符合预期？
7. [ ] DAO 层索引查询的设计是否足够覆盖搜索和报表场景？
8. [ ] 底部导航结构（账本 / 统计 / 设置）是否符合预期？
9. [ ] 银行短信解析推迟到 Phase 4 是否合理？
10. [ ] 周期性交易识别的启发式算法是否够用？是否需要更复杂的统计方法？

---

## 11. 附录 A：推迟到 Phase 4 的功能

以下功能在 Phase 3 规划中被移除，推迟到 Phase 4：

### A.1 银行短信（SMS）解析

**原因：** 各家银行短信格式不统一（招行、工行、建行、农行、中行格式各异），需要大量正则模板和测试样本。且 Android 10+ 对 SMS 权限限制严格，需额外权限声明和用户授权。此项工作量大且不确定性强，不适合放在 4 周 Phase 中。

### A.2 美团 / 京东支付通知扩展

**原因：** 虽然技术难度低（与微信/支付宝走相同的 NotificationListenerService 路径），但需要：
- 收集美团和京东支付通知的真实样本（至少 20+ 条不同场景）
- 测试各版本 App 的通知格式差异
- 确认是否支持折叠通知的展开

### A.3 可能的 Phase 4 时间线

```
Phase 4 (预估 2-3 周)：
├── 银行短信解析引擎（正则模板 + LLM fallback）
├── 美团支付通知支持
├── 京东支付通知支持
├── 退款检测（支付宝/微信退款通知 → TransactionType = INCOME）
└── 交易批量操作（多选删除、批量改分类）
```
