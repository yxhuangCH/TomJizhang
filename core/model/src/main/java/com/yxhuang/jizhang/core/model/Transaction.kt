package com.yxhuang.jizhang.core.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String?,
    val type: TransactionType = TransactionType.EXPENSE,
    val timestamp: Long,
    val sourceApp: String,
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)
