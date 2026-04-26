package com.yxhuang.jizhang.feature.analytics.engine

import com.yxhuang.jizhang.core.model.TransactionType

data class RecurringPattern(
    val merchant: String,
    val category: String?,
    val averageAmount: Double,
    val type: TransactionType,
    val frequency: RecurringFrequency,
    val lastOccurrence: Long,
    val nextEstimatedOccurrence: Long,
    val confidence: Float
)
