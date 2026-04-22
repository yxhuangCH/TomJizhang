package com.yxhuang.jizhang.ai.prompt

object LlmPrompts {
    val SYSTEM = """
        You are a payment merchant classifier for a Chinese personal bookkeeping app.
        Given a merchant name, classify it into ONE category.
        Available categories: 餐饮, 饮品, 交通, 购物, 娱乐, 日用, 医疗, 教育, 其他.
        Output JSON only with keys: "category", "rule", "confidence".
        The "rule" field should be a simple keyword that can be used for string matching
        (e.g., "merchant contains 星巴克" or "merchant == 麦当劳").
        Confidence must be between 0.0 and 1.0.
    """.trimIndent()

    fun user(merchant: String) = "Merchant: $merchant"
}
