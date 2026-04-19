package com.yxhuang.jizhang.feature.parser

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.core.model.ParsedTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TransactionParserTest {

    @Test
    fun `parse wechat payment with amount and merchant`() {
        val notification = NotificationData(
            timestamp = 1000L,
            packageName = "com.tencent.mm",
            tickerText = null,
            title = "微信支付",
            text = "微信支付收款25.00元星巴克",
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
    fun `parse wechat payment with merchant before amount`() {
        val notification = NotificationData(
            timestamp = 2000L,
            packageName = "com.tencent.mm",
            tickerText = null,
            title = "微信支付",
            text = "微信支付 星巴克 38.00元",
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertEquals("38.00", result.amount)
        assertEquals("星巴克", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse alipay payment`() {
        val notification = NotificationData(
            timestamp = 3000L,
            packageName = "com.eg.android.AlipayGphone",
            tickerText = null,
            title = "支付宝",
            text = "支付宝 滴滴出行 18.50",
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertEquals("18.50", result.amount)
        assertEquals("滴滴出行", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse alipay payment with yuan suffix`() {
        val notification = NotificationData(
            timestamp = 4000L,
            packageName = "com.eg.android.AlipayGphone",
            tickerText = null,
            title = "支付宝",
            text = "支付宝 盒马鲜生 128.00元",
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertEquals("128.00", result.amount)
        assertEquals("盒马鲜生", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse falls back to generic pattern`() {
        val notification = NotificationData(
            timestamp = 5000L,
            packageName = "com.tencent.mm",
            tickerText = null,
            title = "微信支付",
            text = "您已向 瑞幸咖啡 支付 ¥19.00",
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertEquals("19.00", result.amount)
        assertEquals("瑞幸咖啡", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse unknown package returns null fields`() {
        val notification = NotificationData(
            timestamp = 6000L,
            packageName = "com.unknown.app",
            tickerText = null,
            title = null,
            text = "微信支付 星巴克 25.00元",
            subText = null,
            bigText = null,
            summaryText = null
        )
        val result = TransactionParser.parse(notification)
        assertNull(result.amount)
        assertNull(result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `isPaymentText returns true for payment keywords`() {
        assertTrue(TransactionParser.isPaymentText("微信支付成功"))
        assertTrue(TransactionParser.isPaymentText("付款完成"))
        assertTrue(TransactionParser.isPaymentText("收款到账"))
        assertTrue(TransactionParser.isPaymentText("交易记录"))
    }

    @Test
    fun `isPaymentText returns false for non payment text`() {
        assertFalse(TransactionParser.isPaymentText("您有一条新消息"))
        assertFalse(TransactionParser.isPaymentText("天气预报"))
    }

    @Test
    fun `extractAmountFallback finds first decimal number`() {
        assertEquals("25.00", TransactionParser.extractAmountFallback("支付 25.00 元"))
        assertEquals("128.50", TransactionParser.extractAmountFallback("共 128.50 元，优惠 10.00 元"))
    }

    @Test
    fun `extractAmountFallback returns null when no decimal`() {
        assertNull(TransactionParser.extractAmountFallback("支付成功"))
    }

    @Test
    fun `extractMerchantFallback finds valid word`() {
        assertEquals("星巴克", TransactionParser.extractMerchantFallback("微信支付 星巴克 25.00元"))
    }

    @Test
    fun `extractMerchantFallback filters out wechat and alipay`() {
        assertNull(TransactionParser.extractMerchantFallback("微信支付宝 25.00元"))
    }
}
