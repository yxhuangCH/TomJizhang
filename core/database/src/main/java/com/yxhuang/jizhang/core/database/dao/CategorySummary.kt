package com.yxhuang.jizhang.core.database.dao

/**
 * DTO for category aggregation queries.
 */
data class CategorySummary(
    val category: String?,
    val total: Double,
    val count: Int
)
