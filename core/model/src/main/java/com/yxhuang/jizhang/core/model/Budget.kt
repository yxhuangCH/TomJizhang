package com.yxhuang.jizhang.core.model

data class Budget(
    val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val alertThreshold: Float = 0.8f
)
