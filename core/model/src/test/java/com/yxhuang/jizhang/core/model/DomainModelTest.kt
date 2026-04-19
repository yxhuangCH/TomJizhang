package com.yxhuang.jizhang.core.model

import kotlinx.serialization.json.Json
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
    fun `transaction copy works correctly`() {
        val tx = Transaction(
            id = 1,
            amount = 25.0,
            merchant = "星巴克",
            category = "餐饮",
            timestamp = 1000L,
            sourceApp = "com.tencent.mm",
            rawText = "raw"
        )
        val updated = tx.copy(category = "饮品")
        assertEquals("饮品", updated.category)
        assertEquals("星巴克", updated.merchant)
        assertEquals(1, updated.id)
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

    @Test
    fun `parseFailureLog has correct defaults`() {
        val log = ParseFailureLog(
            rawText = "failed",
            sourceApp = "wechat",
            timestamp = 1000L,
            reason = "No amount"
        )
        assertEquals(0L, log.id)
    }

    @Test
    fun `notificationData serializes and deserializes correctly`() {
        val data = NotificationData(
            timestamp = 1000L,
            packageName = "com.tencent.mm",
            tickerText = "ticker",
            title = "微信支付",
            text = "25.00元 星巴克",
            subText = "sub",
            bigText = "big",
            summaryText = "summary"
        )
        val json = Json.encodeToString(NotificationData.serializer(), data)
        val decoded = Json.decodeFromString(NotificationData.serializer(), json)
        assertEquals(data, decoded)
    }

    @Test
    fun `notificationData handles null fields`() {
        val data = NotificationData(
            timestamp = 1000L,
            packageName = "com.tencent.mm",
            tickerText = null,
            title = null,
            text = null,
            subText = null,
            bigText = null,
            summaryText = null
        )
        val json = Json.encodeToString(NotificationData.serializer(), data)
        val decoded = Json.decodeFromString(NotificationData.serializer(), json)
        assertNull(decoded.title)
        assertNull(decoded.text)
    }
}
