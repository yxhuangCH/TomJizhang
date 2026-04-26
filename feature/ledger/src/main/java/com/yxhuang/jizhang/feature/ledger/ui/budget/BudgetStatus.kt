package com.yxhuang.jizhang.feature.ledger.ui.budget

data class BudgetStatus(
    val category: String,
    val monthlyLimit: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Float,
    val isOverBudget: Boolean,
    val isAlertTriggered: Boolean
)
