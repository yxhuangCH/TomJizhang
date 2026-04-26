package com.yxhuang.jizhang.feature.ledger.ui.search

data class SearchQuery(
    val keyword: String = "",
    val categories: List<String> = emptyList(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)
