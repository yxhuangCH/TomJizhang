package com.yxhuang.jizhang.feature.ledger.ui.budget

import com.yxhuang.jizhang.core.database.repository.BudgetRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Budget
import com.yxhuang.jizhang.core.model.TransactionType
import kotlinx.coroutines.flow.first
import java.time.YearMonth

class BudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun getBudgetStatuses(yearMonth: String): List<BudgetStatus> {
        val budgets = budgetRepository.observeAll().first()
        val ym = YearMonth.parse(yearMonth)
        val startTime = ym.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = ym.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        return budgets.map { budget ->
            val spent = transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.EXPENSE, startTime, endTime
            )
            val percentage = if (budget.monthlyLimit > 0)
                (spent / budget.monthlyLimit).toFloat().coerceAtLeast(0f) else 0f

            BudgetStatus(
                category = budget.category,
                monthlyLimit = budget.monthlyLimit,
                spent = spent,
                remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0),
                percentage = percentage,
                isOverBudget = percentage >= 1.0f,
                isAlertTriggered = percentage >= budget.alertThreshold
            )
        }
    }

    suspend fun setBudget(category: String, limit: Double, threshold: Float = 0.8f) {
        budgetRepository.upsert(Budget(category = category, monthlyLimit = limit, alertThreshold = threshold))
    }

    suspend fun deleteBudget(category: String) {
        budgetRepository.deleteByCategory(category)
    }
}
