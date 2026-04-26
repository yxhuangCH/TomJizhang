package com.yxhuang.jizhang.feature.analytics.engine

data class MonthlySummary(
    val yearMonth: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val transactionCount: Int
)

data class CategoryBreakdown(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class TrendPoint(
    val label: String,
    val amount: Double
)

data class MerchantRanking(
    val merchant: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val category: String?
)
