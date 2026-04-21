package com.yxhuang.jizhang.core.model

enum class MatchType { EXACT, CONTAINS }

data class CategoryRule(
    val id: Long = 0,
    val keyword: String,
    val category: String,
    val confidence: Float = 1.0f,
    val matchType: MatchType = MatchType.CONTAINS
)
