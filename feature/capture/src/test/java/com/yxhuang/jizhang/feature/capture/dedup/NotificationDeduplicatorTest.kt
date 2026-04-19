package com.yxhuang.jizhang.feature.capture.dedup

import com.yxhuang.jizhang.core.model.NotificationData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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

    @Test
    fun `different package is not duplicate`() {
        val n1 = NotificationData(1000L, "com.tencent.mm", null, "支付", "25.00", null, null, null)
        val n2 = NotificationData(1001L, "com.eg.android.AlipayGphone", null, "支付", "25.00", null, null, null)
        assertFalse(deduplicator.isDuplicate(n1))
        assertFalse(deduplicator.isDuplicate(n2))
    }

    @Test
    fun `ring buffer evicts old entries`() {
        val dedup = NotificationDeduplicator(windowMillis = 1000L, maxSize = 3)
        val n1 = NotificationData(1000L, "com.tencent.mm", null, "a", "1", null, null, null)
        val n2 = NotificationData(1000L, "com.tencent.mm", null, "b", "2", null, null, null)
        val n3 = NotificationData(1000L, "com.tencent.mm", null, "c", "3", null, null, null)
        val n4 = NotificationData(1000L, "com.tencent.mm", null, "d", "4", null, null, null)
        dedup.isDuplicate(n1)
        dedup.isDuplicate(n2)
        dedup.isDuplicate(n3)
        dedup.isDuplicate(n4)
        // n1 should have been evicted from buffer
        val n1Again = NotificationData(1000L, "com.tencent.mm", null, "a", "1", null, null, null)
        assertFalse(deduplicator.isDuplicate(n1Again))
    }
}
