package com.yxhuang.jizhang.poc.parser

data class ParsedTransaction(
    val amount: String?,
    val merchant: String?,
    val isPayment: Boolean
)

object TransactionParser {

    private val wechatPatterns = listOf(
        Regex("微信支付收款(?<amount>\\d+\\.\\d{2})元(?<merchant>[^\\s]+)"),
        Regex("微信支付(?<merchant>[^\\d¥￥]+).*?(?<amount>\\d+\\.\\d{2})元"),
        Regex("(?<merchant>[^\\d¥￥]+)\\s*[-—]?\\s*¥?(?<amount>\\d+\\.\\d{2})")
    )

    private val alipayPatterns = listOf(
        Regex("支付宝(?<merchant>[^\\d¥￥]+).*?(?<amount>\\d+\\.\\d{2})元?"),
        Regex("(?<merchant>[^\\d¥￥]+)\\s*[-—]?\\s*¥?(?<amount>\\d+\\.\\d{2})")
    )

    private val paymentIndicators = listOf("支付", "付款", "收款", "收钱", "交易", "消费", "订单")
    private val skipWords = setOf("您已向", "支付完成", "付款完成", "收款成功")

    fun parse(packageName: String, text: String): ParsedTransaction {
        val patterns = when (packageName) {
            "com.tencent.mm" -> wechatPatterns
            "com.eg.android.AlipayGphone" -> alipayPatterns
            else -> return ParsedTransaction(null, null, isPaymentText(text))
        }

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val amount = match.groups["amount"]?.value
            var merchant = match.groups["merchant"]?.value?.trim()
            if (merchant != null && (skipWords.any { merchant.contains(it) } || paymentIndicators.any { merchant.contains(it) })) {
                merchant = null
            }
            if (amount != null || merchant != null) {
                return ParsedTransaction(
                    amount = amount,
                    merchant = merchant ?: extractMerchantFallback(text),
                    isPayment = isPaymentText(text)
                )
            }
        }

        return ParsedTransaction(
            amount = extractAmountFallback(text),
            merchant = extractMerchantFallback(text),
            isPayment = isPaymentText(text)
        )
    }

    fun isPaymentText(text: String): Boolean {
        return paymentIndicators.any { text.contains(it) }
    }

    fun extractAmountFallback(text: String): String? {
        val regex = Regex("(?<![\\d.])(\\d+\\.\\d{2})(?!\\d)")
        return regex.find(text)?.groupValues?.get(1)
    }

    fun extractMerchantFallback(text: String): String? {
        return text.split(Regex("[\\d¥￥,.，。！?！\\s]+"))
            .filter { it.length >= 2 && it.length <= 15 }
            .filter { word -> skipWords.none { word.contains(it) } }
            .firstOrNull { word ->
                paymentIndicators.none { word.contains(it) } &&
                        !word.contains("微信") &&
                        !word.contains("支付宝")
            }
    }
}
