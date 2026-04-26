package com.yxhuang.jizhang.feature.capture.keepalive

import android.content.Context
import com.yxhuang.jizhang.core.database.repository.BudgetRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.TransactionType
import java.time.YearMonth

class BudgetAlertChecker(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val context: Context,
    private val notifier: BudgetNotifier = BudgetAlertNotificationHelper
) {
    suspend fun checkAndNotify(category: String, yearMonth: String) {
        val budget = budgetRepository.getByCategory(category) ?: return
        val ym = YearMonth.parse(yearMonth)

        val startTime = ym.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = ym.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val spent = transactionRepository.getTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            startTime,
            endTime
        )

        val percentage = if (budget.monthlyLimit > 0)
            (spent / budget.monthlyLimit).toFloat().coerceAtLeast(0f) else 0f

        when {
            percentage >= 1.0f -> notifier.showOverBudgetAlert(
                context, category, budget.monthlyLimit, spent
            )
            percentage >= budget.alertThreshold -> notifier.showThresholdAlert(
                context, category, budget.monthlyLimit, spent, percentage
            )
        }
    }
}
