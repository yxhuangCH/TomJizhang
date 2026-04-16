package com.yxhuang.jizhang.poc.accessibility

object AccessibilityExtractor {

    val TARGET_PACKAGES = setOf(
        "com.tencent.mm",
        "com.eg.android.AlipayGphone"
    )

    val SUCCESS_KEYWORDS = listOf(
        "支付成功", "付款成功", "收钱成功", "支付完成",
        "转账成功", "收款成功"
    )

    private val amountPattern = Regex("[¥￥]\\d+\\.\\d{2}|\\d+\\.\\d{2}元?")

    fun shouldCapture(packageName: String, allTexts: List<String>): Boolean {
        if (packageName !in TARGET_PACKAGES) return false
        return SUCCESS_KEYWORDS.any { keyword ->
            allTexts.any { it.contains(keyword) }
        }
    }

    fun extractAmount(allTexts: List<String>): String? {
        return allTexts.firstOrNull { amountPattern.containsMatchIn(it) }
    }

    fun extractMerchant(allTexts: List<String>): String? {
        return allTexts.firstOrNull { text ->
            text.length in 2..15 &&
                    !text.contains(Regex("[¥￥\\d\\.]")) &&
                    !SUCCESS_KEYWORDS.any { text.contains(it) } &&
                    !text.contains("返回") &&
                    !text.contains("完成")
        }
    }
}
