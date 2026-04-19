package com.yxhuang.jizhang.core.model

data class ParseFailureLog(
    val id: Long = 0,
    val rawText: String,
    val sourceApp: String,
    val timestamp: Long,
    val reason: String?
)
