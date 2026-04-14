package com.yxhuang.jizhang.poc.accessibility

import org.junit.Assert.*
import org.junit.Test

class AccessibilityExtractorTest {

    @Test
    fun `shouldCapture returns true when success keyword present`() {
        val texts = listOf("支付成功", "¥25.00", "星巴克")
        assertTrue(AccessibilityExtractor.shouldCapture("com.tencent.mm", texts))
    }

    @Test
    fun `shouldCapture returns false when no success keyword`() {
        val texts = listOf("微信支付", "¥25.00", "星巴克")
        assertFalse(AccessibilityExtractor.shouldCapture("com.tencent.mm", texts))
    }

    @Test
    fun `shouldCapture returns false for unknown package`() {
        val texts = listOf("支付成功", "¥25.00")
        assertFalse(AccessibilityExtractor.shouldCapture("com.unknown.app", texts))
    }

    @Test
    fun `extractAmount finds amount with yen symbol`() {
        val texts = listOf("支付成功", "¥25.00", "星巴克")
        assertEquals("¥25.00", AccessibilityExtractor.extractAmount(texts))
    }

    @Test
    fun `extractAmount finds amount with yuan suffix`() {
        val texts = listOf("付款成功", "18.50元", "滴滴出行")
        assertEquals("18.50元", AccessibilityExtractor.extractAmount(texts))
    }

    @Test
    fun `extractAmount returns null when no amount`() {
        val texts = listOf("支付成功", "星巴克")
        assertNull(AccessibilityExtractor.extractAmount(texts))
    }

    @Test
    fun `extractMerchant finds valid merchant`() {
        val texts = listOf("支付成功", "¥25.00", "星巴克")
        assertEquals("星巴克", AccessibilityExtractor.extractMerchant(texts))
    }

    @Test
    fun `extractMerchant skips success keywords`() {
        val texts = listOf("支付成功", "付款成功", "¥25.00")
        assertNull(AccessibilityExtractor.extractMerchant(texts))
    }

    @Test
    fun `extractMerchant skips navigation texts`() {
        val texts = listOf("返回", "完成", "¥25.00")
        assertNull(AccessibilityExtractor.extractMerchant(texts))
    }

    @Test
    fun `extractMerchant skips texts with digits or symbols`() {
        val texts = listOf("¥25.00", "18.50元", "2.5折")
        assertNull(AccessibilityExtractor.extractMerchant(texts))
    }

    @Test
    fun `extractMerchant filters by length`() {
        val texts = listOf("a", "星巴克", "星巴克咖啡餐厅上海店")
        // "a" too short, "星巴克咖啡餐厅上海店" too long (>15)
        assertEquals("星巴克", AccessibilityExtractor.extractMerchant(texts))
    }
}
