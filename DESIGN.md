# Android 自动记账 App — Production 技术方案

> 可直接落地、可长期维护、可开源发布的完整技术设计。  
> 覆盖：架构设计、模块划分、AI 分类、数据流、测试策略与工程规范。

---

## 目录

1. [项目目标](#1-项目目标)
2. [系统总体架构](#2-系统总体架构)
3. [模块化 Gradle 结构](#3-模块化-gradle-结构)
4. [AI 自动分类设计](#4-ai-自动分类设计)
5. [Compose + Flow 最优数据流](#5-compose--flow-最优数据流)
6. [Production 目录结构](#6-production-目录结构)
7. [自动测试策略](#7-自动测试策略)
8. [README（开源版本）](#8-readme开源版本)
9. [架构设计原则总结](#9-架构设计原则总结)
10. [后续演进路线](#10-后续演进路线)

---

## 1. 项目目标

### 核心问题

用户使用微信支付 / 支付宝完成付款，但记账需要 **打开另一个 App → 手动输入**。
结果：长期放弃记账。

### MVP 目标

```
支付完成
   ↓
自动识别
   ↓
自动分类
   ↓
自动入账
```

用户无需打开 App。

### 非目标（V1 不做）

- 多端同步
- 登录系统
- 社区功能
- 复杂统计

---

## 2. 系统总体架构

### 高层架构

```
Notification Capture
       ↓
Transaction Parser
       ↓
Local Rule Engine
       ↓
LLM Learning (rare)
       ↓
Room Database
       ↓
   Flow Pipeline
       ↓
   Compose UI
```

### 数据流（Single Direction）

```
Data Source → Domain → Reducer → UiState → UI
```

**UI 永远不反向修改数据。**

---

## 3. 模块化 Gradle 结构

采用 **Clean Modular Architecture**，`settings.gradle` 示例：

```gradle
include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:common",

    ":feature:capture",
    ":feature:parser",
    ":feature:classification",
    ":feature:ledger",

    ":ai",
    ":testing"
)
```

### 模块职责

| 模块 | 职责 |
|------|------|
| `app` | Application 入口 |
| `core:model` | 数据模型 |
| `core:database` | Room 数据库 |
| `core:common` | 工具类 / Log |
| `feature:capture` | 通知监听 |
| `feature:parser` | 文本解析 |
| `feature:classification` | 分类引擎 |
| `feature:ledger` | UI 层 |
| `ai` | LLM 调用封装 |
| `testing` | 测试工具 / Fake |

### 依赖方向（必须单向）

```
feature → core
feature → ai
app     → all
```

**禁止：** `core → feature`。

---

## 4. AI 自动分类设计

### 核心理念

- **LLM = Teacher**：只负责学习，不实时分类。
- **Local Rules = Runtime Engine**：命中规则直接本地分类。

### 三层分类结构

| Layer | 机制 | 命中率 |
|-------|------|--------|
| 1 | 精确匹配 | 70% |
| 2 | 模糊规则 | 25% |
| 3 | LLM 学习 | <5% |

### 调用策略

- **错误**：每笔交易调用 LLM。
- **正确**：新商户首次出现 → 调用 LLM 学习 → 生成本地规则。

### Prompt 设计

**System Prompt**

```text
You classify payment merchants into spending categories.
Output JSON only.
Choose ONE category.
```

**输出格式**

```json
{
  "category": "饮品",
  "rule": "merchant contains 霸王茶姬"
}
```

### Rule Engine

```kotlin
sealed interface Rule {
    fun match(merchant: String): Boolean
}

data class ContainsRule(
    val keyword: String
) : Rule {
    override fun match(m: String) = m.contains(keyword)
}
```

### 学习闭环

```
LLM → 新规则 → 本地保存 → 永久自动分类
```

### 数据表

```kotlin
@Entity
data class CategoryRule(
    @PrimaryKey val id: Long = 0,
    val keyword: String,
    val category: String,
    val confidence: Float
)
```

### 成本模型

- 1200 笔交易 / 月
- ≈ 40 次 LLM 调用
- **≈ 97% 成本降低**

---

## 5. Compose + Flow 最优数据流

### 错误模式

直接 `flow.collectAsState()` 会导致整列表重组。

### 正确数据流

```
Room Flow
   ↓
Repository
   ↓
Reducer
   ↓
StateFlow<UiState>
   ↓
Compose
```

### UI State

```kotlin
data class TransactionUiState(
    val items: ImmutableList<TransactionItem>
)
```

使用 `kotlinx.collections.immutable`。

### Reducer

```kotlin
class TransactionReducer {
    fun reduce(old: UiState, data: List<Transaction>): UiState {
        val mapped = data.map { it.toUi() }
        if (old.items == mapped) return old
        return old.copy(items = mapped.toImmutableList())
    }
}
```

### ViewModel Flow

```kotlin
val uiState = repository.observe()
    .map { reducer.reduce(current, it) }
    .distinctUntilChanged()
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState.EMPTY
    )
```

### LazyColumn

```kotlin
items(
    items = state.items,
    key = { it.id }
)
```

### UI 原则

| 规则 | 原因 |
|------|------|
| UI 无逻辑 | 防止重组 |
| Immutable model | 快速 `==` 比较 |
| Stable key | 滚动性能 |

---

## 6. Production 目录结构

```
app/
core/
 ├── model/
 ├── database/
 └── common/

feature/
 ├── capture/
 ├── parser/
 ├── classification/
 └── ledger/

ai/
testing/
```

### feature 示例

```
feature/parser
 ├── src/main/java/.../parser/
 │    ├── Parser.kt
 │    ├── RegexParser.kt
 │    ├── ParserUseCase.kt
 │    └── model/
 └── build.gradle.kts
```

---

## 7. 自动测试策略

### Parser 测试（最重要）

支付文案变化是最大的回归风险。

```kotlin
@Test
fun `parse wechat payment`() {
    val text = "微信支付 收款 25.00元 星巴克"
    val result = parser.parse(text)

    assertEquals("星巴克", result.merchant)
}
```

### Rule Engine 测试

```kotlin
@Test
fun `contains rule match`() {
    val rule = ContainsRule("滴滴")
    assertTrue(rule.match("滴滴出行"))
}
```

### Snapshot Test（推荐）

保存真实通知样本，防止解析回归：

```
testing/snapshots/wechat_2026.txt
```

### Flow 测试

使用 [Turbine](https://github.com/cashapp/turbine)：

```kotlin
flow.test {
    awaitItem()
}
```

---

## 8. README（开源版本）

### 项目介绍

Automatic bookkeeping Android app powered by:

- Notification parsing
- Local AI learning
- Offline-first architecture
- Jetpack Compose + Kotlin Flow

### Features

- Zero manual bookkeeping
- AI auto classification
- Offline first
- Privacy friendly

### Architecture

```
Capture → Parse → Classify → Store → UI
```

### Tech Stack

- Kotlin
- Jetpack Compose
- Coroutines + Flow
- Room
- LLM (optional learning)

### Why This Project Exists

Manual bookkeeping fails because **friction > motivation**.  
This project removes friction entirely.

---

## 9. 架构设计原则总结

| 原则 | 解释 |
|------|------|
| Offline First | 无网络可运行 |
| AI 辅助非依赖 | 不阻塞主流程 |
| 单向数据流 | 易维护 |
| 模块隔离 | 可扩展 |
| 可测试 | 长期稳定 |

---

## 10. 后续演进路线

推荐顺序：

1. 通知解析鲁棒性
2. 自动分类学习优化
3. 周消费 AI 报告
4. 云同步（CRDT）
5. 自动预算预测

---

## 最终结果

你将得到：

- 自动记账系统
- 极低 AI 成本
- Production 级 Android 架构
- 可长期维护项目
- 可直接开源

**本项目本质：Personal Financial Data Pipeline**
