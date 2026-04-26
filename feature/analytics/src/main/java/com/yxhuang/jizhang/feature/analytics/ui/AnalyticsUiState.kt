package com.yxhuang.jizhang.feature.analytics.ui

import com.yxhuang.jizhang.feature.analytics.engine.CategoryBreakdown
import com.yxhuang.jizhang.feature.analytics.engine.MerchantRanking
import com.yxhuang.jizhang.feature.analytics.engine.MonthlySummary
import com.yxhuang.jizhang.feature.analytics.engine.RecurringPattern
import com.yxhuang.jizhang.feature.analytics.engine.TrendPoint

data class AnalyticsUiState(
    val yearMonth: String = "",
    val isLoading: Boolean = true,
    val selectedTab: Int = 0,
    val monthlySummaries: List<MonthlySummary> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val dailyTrend: List<TrendPoint> = emptyList(),
    val topMerchants: List<MerchantRanking> = emptyList(),
    val recurringPatterns: List<RecurringPattern> = emptyList()
)
