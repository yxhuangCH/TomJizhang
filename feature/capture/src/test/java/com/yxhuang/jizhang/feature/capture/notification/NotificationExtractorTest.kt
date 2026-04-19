package com.yxhuang.jizhang.feature.capture.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationExtractorTest {

    @Test
    fun `shouldCapture returns true for wechat`() {
        assertTrue(NotificationExtractor.shouldCapture("com.tencent.mm"))
    }

    @Test
    fun `shouldCapture returns true for alipay`() {
        assertTrue(NotificationExtractor.shouldCapture("com.eg.android.AlipayGphone"))
    }

    @Test
    fun `shouldCapture returns false for unknown package`() {
        assertFalse(NotificationExtractor.shouldCapture("com.example.app"))
    }

    @Test
    fun `extract creates NotificationData with all fields`() {
        val data = NotificationExtractor.extract(
            packageName = "com.tencent.mm",
            tickerText = "微信支付",
            title = "微信支付",
            text = "25.00元 星巴克",
            subText = null,
            bigText = "您已支付 25.00元 给 星巴克",
            summaryText = "支付成功",
            currentTimeMillis = 12345678L
        )

        assertEquals(12345678L, data.timestamp)
        assertEquals("com.tencent.mm", data.packageName)
        assertEquals("微信支付", data.tickerText)
        assertEquals("微信支付", data.title)
        assertEquals("25.00元 星巴克", data.text)
        assertNull(data.subText)
        assertEquals("您已支付 25.00元 给 星巴克", data.bigText)
        assertEquals("支付成功", data.summaryText)
    }

    @Test
    fun `extract creates NotificationData with defaults`() {
        val data = NotificationExtractor.extract(
            packageName = "com.eg.android.AlipayGphone",
            currentTimeMillis = 1000L
        )

        assertEquals(1000L, data.timestamp)
        assertEquals("com.eg.android.AlipayGphone", data.packageName)
        assertNull(data.tickerText)
        assertNull(data.title)
        assertNull(data.text)
        assertNull(data.subText)
        assertNull(data.bigText)
        assertNull(data.summaryText)
    }
}
