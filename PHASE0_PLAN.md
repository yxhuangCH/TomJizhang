# Phase 0：技术预研与 PoC 实施计划

> 周期：第 1–2 周（共 10 个工作日）  
> 目标：零架构代码，仅验证核心假设——**能否稳定捕获并解析微信/支付宝支付通知**。  
> 产出：Go / No-Go 决策依据 + 50+ 真实样本库。

---

## 目录

1. [Phase 0 总体目标](#1-phase-0-总体目标)
2. [成功标准（Go/No-Go）](#2-成功标准gono-go)
3. [人员与设备](#3-人员与设备)
4. [Week 1 详细计划](#4-week-1-详细计划)
5. [Week 2 详细计划](#5-week-2-详细计划)
6. [技术实现清单](#6-技术实现清单)
7. [样本收集规范](#7-样本收集规范)
8. [分析方法与模板](#8-分析方法与模板)
9. [交付物清单](#9-交付物清单)
10. [风险应对预案](#10-风险应对预案)
11. [每日站会检查清单](#11-每日站会检查清单)

---

## 1. Phase 0 总体目标

### 核心假设验证

| # | 假设 | 验证方式 |
|---|------|----------|
| H1 | `NotificationListenerService` 能在主流机型上收到微信/支付宝的支付通知 | 安装 PoC APK，日常使用 1 周后统计捕获率 |
| H2 | 通知内容中包含足够的金额、商户信息用于解析 | 分析收集到的样本，统计可解析率 |
| H3 | 同一笔交易不会产生过多重复/干扰通知 | 分析样本中的去重情况 |
| H4 | 如果 H1/H2 不成立，`AccessibilityService` 能作为有效 fallback | 编写最小 Accessibility Demo 验证 |

### 范围边界

- **做**：通知监听、样本收集、正则解析实验、Accessibility fallback 验证。
- **不做**：模块化架构、Room 数据库、Compose UI、LLM 调用、后台保活工程。

---

## 2. 成功标准（Go/No-Go）

### Go 标准（满足以下全部，进入 Phase 1）

- [ ] **G1** 微信支付通知捕获率 ≥ **90%**（以测试者主动记录的真实交易笔数为分母）。
- [ ] **G2** 支付宝支付通知捕获率 ≥ **85%**。
- [ ] **G3** 至少 **80%** 的捕获样本能直接提取出「金额 + 商户/商品名」。
- [ ] **G4** 样本中去重后的有效交易与原始通知数量比 ≥ **70%**（即重复/干扰通知可控）。

### No-Go 标准（触发以下任一，暂停并决策）

- [ ] **N1** 微信支付通知捕获率 < **70%**，且 AccessibilityService fallback 验证也失败。
- [ ] **N2** 主流厂商机型（如华为/小米）在开启所有权限后，捕获率仍 < **60%**。
- [ ] **N3** 支付宝/微信通知内容被系统加密/折叠，无法提取金额或商户信息。

### 灰色地带处理

如果结果介于 Go 和 No-Go 之间（如捕获率 75%-85%）：
- **决策 A**：如果 AccessibilityService fallback 验证成功，可 Go，但 Phase 1 必须将该方案纳入架构。
- **决策 B**：如果仅特定机型（如某一代鸿蒙）有问题，可 Go，但需在文档中明确「不支持机型列表」。

---

## 3. 人员与设备

### 人员分工

| 角色 | 职责 | 投入 |
|------|------|------|
| Android 开发 | 编写 PoC Demo、解析实验、输出技术报告 | 全职 2 周 |
| 测试者 A | 日常使用微信/支付宝，记录每笔交易时间/金额/商户 | 兼职（每天正常使用即可） |
| 测试者 B | 同上，使用不同品牌手机 | 兼职 |
| 测试者 C | 同上，使用不同品牌手机 | 兼职 |

### 测试设备清单（最低要求）

| 设备 | 系统 / ROM | 必须验证的场景 |
|------|------------|----------------|
| 小米手机 | HyperOS / MIUI | 杀后台策略、通知折叠 |
| 华为手机 | HarmonyOS / EMUI | 通知权限管理、微信通知加密 |
| OPPO / vivo / 一加 | ColorOS / OriginOS | 自启动权限、电池优化 |

**建议**：如果条件允许，增加一台 **Pixel / Samsung** 作为原生 Android 对照组。

---

## 4. Week 1 详细计划

### Day 1（周一）：工程搭建

- [ ] **D1-T1** 创建空 Android 项目 `poc-notification-capture`。
- [ ] **D1-T2** 添加 `NotificationListenerService` 骨架。
- [ ] **D1-T3** 实现通知序列化逻辑：将 `Notification` 的 `tickerText`、`title`、`text`、`subText`、`bigText` 等字段提取为 JSON。
- [ ] **D1-T4** 将 JSON 写入 App 私有目录 `/sdcard/Android/data/.../files/notifications/`（方便测试者直接导出）。
- [ ] **D1-T5** 配置包名过滤：仅监听 `com.tencent.mm`（微信）和 `com.eg.android.AlipayGphone`（支付宝）。

### Day 2（周二）：安装与首次测试

- [ ] **D2-T1** 在 3 台测试机上安装 PoC APK。
- [ ] **D2-T2** 引导测试者开启「通知使用权」。
- [ ] **D2-T3** 每台手机手动发起一笔微信转账/红包，验证通知是否被捕获。
- [ ] **D2-T4** 记录首次安装过程中的权限引导摩擦点（哪些步骤容易被用户拒绝）。

### Day 3（周三）：日常使用样本收集启动

- [ ] **D3-T1** 给每位测试者发放《交易记录表》模板（见第 7 节）。
- [ ] **D3-T2** 测试者开始正常使用微信支付/支付宝，每完成一笔交易在表格中记录：时间、金额、商户/对象、交易类型（扫码/转账/红包/线上支付）。
- [ ] **D3-T3** 开发侧同步检查每台手机的日志文件，确认服务没有崩溃或停止。

### Day 4（周四）：初步样本分析

- [ ] **D4-T1** 收集前 3 天的通知日志，汇总到 `poc_samples/` 目录。
- [ ] **D4-T2** 按「时间相近」原则将通知与人工记录的交易进行匹配。
- [ ] **D4-T3** 统计初步捕获率（捕获数 / 人工记录数）。
- [ ] **D4-T4** 标记「捕获但未解析」的异常样本（通知来了，但内容里没有金额）。

### Day 5（周五）：Week 1 复盘

- [ ] **D5-T1** 输出《Week 1  interim report.md》，包含：
  - 每台设备的捕获率统计。
  - 发现的系统级异常（如服务被杀、通知折叠）。
  - 已解析的样本结构规律。
- [ ] **D5-T2** 团队会议决定：Week 2 是否需要增加测试机型或调整测试重点。

---

## 5. Week 2 详细计划

### Day 6（周一）：AccessibilityService Fallback 验证

- [ ] **D6-T1** 创建第二个 PoC Demo：`poc-accessibility-capture`。
- [ ] **D6-T2** 实现 `AccessibilityService`，监听微信/支付宝页面切换事件。
- [ ] **D6-T3** 尝试读取「支付成功」页面中的 `NodeInfo`，提取金额和商户文本。
- [ ] **D6-T4** 在 3 台手机上分别测试 5 笔真实交易，记录可读取的字段和成功率。

### Day 7（周二）：正则解析实验

- [ ] **D7-T1** 基于 Week 1 收集的样本，编写 3–5 条核心正则：
  - 微信支付正则（覆盖扫码支付、转账、线上购物）。
  - 支付宝支付正则（覆盖扫码支付、转账、线上购物）。
- [ ] **D7-T2** 用 Python 或 Kotlin 脚本对全部样本跑批量解析实验。
- [ ] **D7-T3** 统计正则解析成功率，标记误匹配和漏匹配的样本。

### Day 8（周三）：通知去重与干扰分析

- [ ] **D8-T1** 分析样本中「同一笔交易产生多条通知」的情况。
- [ ] **D8-T2** 设计去重启发式规则（如：5 秒内、同一金额、同一 App 的通知视为同一笔）。
- [ ] **D8-T3** 验证去重规则在现有样本上的效果。
- [ ] **D8-T4** 记录常见干扰通知（如支付宝的「能量收取提醒」「账单生成提醒」）。

### Day 9（周四）：完整数据汇总

- [ ] **D9-T1** 汇总全部两周样本，按设备、App、交易类型分类统计。
- [ ] **D9-T2** 整理《通知格式分析报告》：列出每种交易类型的典型通知结构。
- [ ] **D9-T3** 统计最终 Go/No-Go 指标。

### Day 10（周五）：决策与文档输出

- [ ] **D10-T1** 召开 Phase 0 评审会，基于数据做 Go/No-Go 决策。
- [ ] **D10-T2** 输出 `docs/poc_report.md`（见第 9 节模板）。
- [ ] **D10-T3** 如果决策为 Go，输出 Phase 1 需要的技术调整清单（如是否需要同时支持 AccessibilityService）。
- [ ] **D10-T4** 如果决策为 No-Go，输出项目终止报告或备选方向分析。

---

## 6. 技术实现清单

### 6.1 NotificationListenerService 最小实现

```kotlin
class PocNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in setOf("com.tencent.mm", "com.eg.android.AlipayGphone")) return

        val extras = sbn.notification.extras
        val data = NotificationData(
            timestamp = System.currentTimeMillis(),
            packageName = pkg,
            tickerText = sbn.notification.tickerText?.toString(),
            title = extras.getString(Notification.EXTRA_TITLE),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        )

        writeToFile(data)
    }

    private fun writeToFile(data: NotificationData) {
        val dir = getExternalFilesDir(null) ?: return
        val file = File(dir, "notifications/${data.packageName}_${data.timestamp}.json")
        file.parentFile?.mkdirs()
        file.writeText(Json.encodeToString(data))
    }
}
```

### 6.2 通知数据模型

```kotlin
@Serializable
data class NotificationData(
    val timestamp: Long,
    val packageName: String,
    val tickerText: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val summaryText: String?
)
```

### 6.3 AccessibilityService 最小实现

```kotlin
class PocAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg !in setOf("com.tencent.mm", "com.eg.android.AlipayGphone")) return

        // 尝试查找包含 "支付成功" / "付款成功" 等关键字的节点
        val successNode = findNodeWithText(root, listOf("支付成功", "付款成功", "收钱成功"))
        if (successNode != null) {
            // 遍历同级/子节点，尝试提取金额和商户
            val amount = findNodeWithText(root.parent, regex = Regex("[¥￥]\\d+\\.\\d{2}"))
            val merchant = findNodeWithText(root.parent, regex = Regex(".{2,20} (店|超市|餐厅|商家)"))
            writeAccessibilityLog(amount, merchant)
        }
    }

    override fun onInterrupt() {}
}
```

### 6.4 批量解析实验脚本（Kotlin 脚本示例）

```kotlin
// 放在 poc/ 目录下，用于对收集的 JSON 样本跑批量解析
fun main() {
    val samplesDir = File("poc_samples")
    val samples = samplesDir.listFiles { it.extension == "json" } ?: return

    val wechatRegex = Regex("微信支付.*?(\\d+\\.\\d{2}).*?([\\u4e00-\\u9fa5a-zA-Z0-9]+)")
    val alipayRegex = Regex("支付宝.*?(\\d+\\.\\d{2}).*?([\\u4e00-\\u9fa5a-zA-Z0-9]+)")

    var total = 0
    var parsed = 0

    samples.forEach { file ->
        val data = Json.decodeFromString<NotificationData>(file.readText())
        val text = listOfNotNull(data.title, data.text, data.bigText).joinToString(" ")
        val regex = if (data.packageName == "com.tencent.mm") wechatRegex else alipayRegex
        val match = regex.find(text)

        total++
        if (match != null) {
            parsed++
            println("✅ ${file.name} -> amount=${match.groupValues[1]}, merchant=${match.groupValues[2]}")
        } else {
            println("❌ ${file.name} -> $text")
        }
    }

    println("解析成功率: $parsed / $total = ${parsed * 100 / total}%")
}
```

---

## 7. 样本收集规范

### 7.1 测试者交易记录表模板

每位测试者使用以下表格手动记录真实交易，用于与通知日志进行对账：

| 序号 | 交易时间 | 金额（元） | 商户/对象 | 支付方式 | 交易类型 | 备注 |
|------|----------|------------|-----------|----------|----------|------|
| 1 | 2026-04-13 10:23 | 25.00 | 星巴克 | 微信 | 扫码支付 | |
| 2 | 2026-04-13 14:05 | 18.50 | 滴滴出行 | 支付宝 | 线上支付 | 打车 |
| 3 | 2026-04-13 20:10 | 200.00 | 张三 | 微信 | 转账 | 红包 |

### 7.2 通知日志文件命名规范

```
poc_samples/
├── wechat/
│   ├── 20260413_102300_payment.json
│   ├── 20260413_102305_update.json
│   └── ...
├── alipay/
│   ├── 20260413_140500_payment.json
│   └── ...
└── unmatched/          # 无法与人工记录匹配的通知
    └── ...
```

### 7.3 样本标注规范

对每个样本 JSON 文件，补充一个同名的 `.meta.json` 文件：

```json
{
  "matchedTransactionId": 1,
  "isDuplicate": false,
  "isValidPayment": true,
  "extractedAmount": "25.00",
  "extractedMerchant": "星巴克",
  "parserStatus": "success",
  "notes": "bigText 包含完整信息"
}
```

字段说明：
- `matchedTransactionId`：对应人工记录表的序号。
- `isDuplicate`：是否为重复通知。
- `isValidPayment`：是否为有效支付通知（过滤掉营销通知、能量提醒等）。
- `parserStatus`：`success` / `partial` / `failed`。

---

## 8. 分析方法与模板

### 8.1 捕获率统计公式

```
App 捕获率 = 该 App 的有效去重通知数 / 人工记录的交易笔数 × 100%

整体解析成功率 = 解析成功的样本数 / 有效去重通知数 × 100%
```

### 8.2 分维度统计表

| 维度 | 微信 | 支付宝 |
|------|------|--------|
| 总交易笔数（人工记录） | | |
| 有效通知数 | | |
| 重复/干扰通知数 | | |
| 捕获率 | | |
| 解析成功率 | | |
| 扫码支付解析率 | | |
| 线上支付解析率 | | |
| 转账/红包解析率 | | |

### 8.3 机型差异统计表

| 机型 | 系统版本 | 微信捕获率 | 支付宝捕获率 | 主要问题 |
|------|----------|------------|--------------|----------|
| 小米 14 | HyperOS 2.0 | | | |
| 华为 P60 | HarmonyOS 4.0 | | | |
| OPPO Find X7 | ColorOS 14 | | | |

### 8.4 问题分类标签

对每一个异常样本，打上一个或多个标签：

- `NO_NOTIFICATION`：完全没有收到通知。
- `FOLDED`：通知被系统折叠，只有摘要（如"你收到一条新消息"）。
- `NO_AMOUNT`：通知原文中没有金额字段。
- `NO_MERCHANT`：通知原文中没有商户字段。
- `DUPLICATE`：同一笔交易出现多条内容相似的通知。
- `NOISE`：非支付通知被误捕获（如营销推送）。
- `SERVICE_KILLED`：通知服务停止工作，期间所有交易缺失。

---

## 9. 交付物清单

### 9.1 代码类

| 文件名 | 位置 | 说明 |
|--------|------|------|
| `PocNotificationService.kt` | `poc-notification-capture/app/...` | 通知监听 PoC 源码 |
| `PocAccessibilityService.kt` | `poc-accessibility-capture/app/...` | 无障碍服务 PoC 源码 |
| `BatchParser.kt` | `poc-scripts/` | 批量解析实验脚本 |

### 9.2 数据类

| 文件名 | 位置 | 说明 |
|--------|------|------|
| `raw_samples/` | `poc-data/week1/` 和 `poc-data/week2/` | 原始通知 JSON |
| `annotated_samples/` | `poc-data/annotated/` | 带标注的样本 |
| `transaction_logs/` | `poc-data/` | 3 位测试者的人工记录表 |

### 9.3 文档类

| 文件名 | 位置 | 说明 |
|--------|------|------|
| `week1_interim_report.md` | `docs/poc/` | Week 1 中期报告 |
| `notification_samples.md` | `docs/poc/` | 50+ 样本结构分析 |
| `poc_report.md` | `docs/poc/` | Phase 0 最终决策报告 |

### 9.4 `poc_report.md` 模板

```markdown
# Phase 0 PoC 报告

## 1. 执行摘要
- 测试周期：2026/04/13 - 2026/04/24
- 测试设备：3 台
- 人工记录交易：XX 笔
- 最终决策：Go / No-Go / Go with conditions

## 2. 核心指标
| 指标 | 结果 | 目标 | 状态 |
|------|------|------|------|
| 微信捕获率 | XX% | ≥90% | ✅/❌ |
| 支付宝捕获率 | XX% | ≥85% | ✅/❌ |
| 解析成功率 | XX% | ≥80% | ✅/❌ |
| 有效通知占比 | XX% | ≥70% | ✅/❌ |

## 3. 样本分析
（按交易类型和机型分类的详细统计）

## 4. 发现的问题
（列出所有带标签的异常样本及影响）

## 5. Accessibility Fallback 结果
- 成功率：XX%
- 可行性结论：可作为主方案 / 可作为 fallback / 不可行

## 6. 决策与下一步
- 决策：Go / No-Go
- 理由：...
- Phase 1 必须解决的技术债务：...
```

---

## 10. 风险应对预案

### 风险 R1：NotificationListenerService 在测试机上频繁被杀

- **现象**：服务运行几小时后自动停止，后续交易全部丢失。
- **应对**：
  1. 引导测试者手动设置「自启动」「电池优化白名单」「锁后台」。
  2. 如果设置后仍然被杀，记录被杀的规律（如锁屏后 30 分钟、特定 App 启动后）。
  3. 如果某机型无论如何都无法存活，将该机型标记为「高风险机型」。

### 风险 R2：微信/支付宝通知内容被系统折叠

- **现象**：`Notification` 中只能拿到「微信支付」或「你收到一条新消息」，没有金额。
- **应对**：
  1. 检查 `bigText`、`summaryText` 等字段是否包含完整内容。
  2. 如果所有字段都被折叠，立即启动 AccessibilityService fallback 验证。
  3. 如果 Accessibility 也拿不到，记录该机型和系统版本，作为「不支持机型」。

### 风险 R3：测试者忘记记录交易，导致分母（人工记录笔数）缺失

- **应对**：
  1. 每天晚 9 点提醒测试者核对当天的微信支付/支付宝账单，补录遗漏交易。
  2. 以 App 内账单（微信：我 → 服务 → 钱包 → 账单）作为最终分母的对账依据。

### 风险 R4：正则解析实验发现大量样本结构不一致

- **应对**：
  1. 将样本按「App + 交易类型 + 系统版本」分组，观察是否是分组内规律一致、组间差异大。
  2. 如果是，说明只需为不同场景写多套正则，仍在可控范围内。
  3. 如果同一分组内也极不稳定，则 Parser 工程复杂度将大幅上升，需重新评估项目可行性。

---

## 11. 每日站会检查清单

每天 10 分钟站会，只回答以下 5 个问题：

1. **昨天收集了多少新样本？**（每台设备各多少）
2. **服务有被杀死的情况吗？**（哪台设备、什么时候、是否已恢复）
3. **有没有出现全新的异常类型？**（如之前没见过的通知折叠方式）
4. **Accessibility fallback 进展如何？**（Day 6 之后开始关注）
5. **今天计划验证什么？**（明确的 1–2 个目标）

---

## 附录：Phase 0 快速启动命令

```bash
# 1. 创建项目目录
mkdir -p poc-notification-capture poc-accessibility-capture poc-data docs/poc

# 2. 创建样本收集目录
mkdir -p poc-data/week1 poc-data/week2 poc-data/annotated

# 3. 每日同步测试者日志（示例）
adb pull /sdcard/Android/data/com.example.poc/files/notifications/ poc-data/week1/
```
