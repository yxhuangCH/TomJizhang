package com.yxhuang.jizhang.core.model

data class ParsedTransaction(
    val amount: String?,
    val merchant: String?,
    val isPayment: Boolean,
    val rawText: String,
    val sourceApp: String,
    val timestamp: Long
)
