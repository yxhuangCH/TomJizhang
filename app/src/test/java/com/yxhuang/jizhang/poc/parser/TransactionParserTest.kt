package com.yxhuang.jizhang.poc.parser

import org.junit.Assert.*
import org.junit.Test

class TransactionParserTest {

    @Test
    fun `parse wechat payment with amount and merchant`() {
        val result = TransactionParser.parse(
            "com.tencent.mm",
            "微信支付收款25.00元星巴克"
        )
        assertEquals("25.00", result.amount)
        assertEquals("星巴克", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse wechat payment with merchant before amount`() {
        val result = TransactionParser.parse(
            "com.tencent.mm",
            "微信支付 星巴克 38.00元"
        )
        assertEquals("38.00", result.amount)
        assertEquals("星巴克", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse alipay payment`() {
        val result = TransactionParser.parse(
            "com.eg.android.AlipayGphone",
            "支付宝 滴滴出行 18.50"
        )
        assertEquals("18.50", result.amount)
        assertEquals("滴滴出行", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse alipay payment with yuan suffix`() {
        val result = TransactionParser.parse(
            "com.eg.android.AlipayGphone",
            "支付宝 盒马鲜生 128.00元"
        )
        assertEquals("128.00", result.amount)
        assertEquals("盒马鲜生", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse falls back to generic pattern`() {
        val result = TransactionParser.parse(
            "com.tencent.mm",
            "您已向 瑞幸咖啡 支付 ¥19.00"
        )
        assertEquals("19.00", result.amount)
        assertEquals("瑞幸咖啡", result.merchant)
        assertTrue(result.isPayment)
    }

    @Test
    fun `parse returns null for unknown package`() {
        val result = TransactionParser.parse(
            "com.unknown.app",
            "微信支付 星巴克 25.00元"
        )
        assertNull(result.amount)
        assertNull(result.merchant)
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
    fun `extractMerchantFallback finds longest valid word`() {
        assertEquals("星巴克", TransactionParser.extractMerchantFallback("微信支付 星巴克 25.00元"))
    }

    @Test
    fun `extractMerchantFallback filters out wechat and alipay`() {
        assertNull(TransactionParser.extractMerchantFallback("微信支付宝 25.00元"))
    }
}
