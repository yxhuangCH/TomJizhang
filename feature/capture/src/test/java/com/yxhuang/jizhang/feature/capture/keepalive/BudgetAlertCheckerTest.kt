package com.yxhuang.jizhang.feature.capture.keepalive

import android.content.Context
import com.yxhuang.jizhang.core.database.repository.BudgetRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Budget
import com.yxhuang.jizhang.core.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class BudgetAlertCheckerTest {
    private val budgetRepo = mockk<BudgetRepository>()
    private val txRepo = mockk<TransactionRepository>()
    private val context = mockk<Context>(relaxed = true)
    private val notifier = mockk<BudgetNotifier>(relaxed = true)
    private val checker = BudgetAlertChecker(budgetRepo, txRepo, context, notifier)

    @Test
    fun `no alert when no budget set`() = runTest {
        coEvery { budgetRepo.getByCategory("餐饮") } returns null

        checker.checkAndNotify("餐饮", "2026-04")

        coVerify(exactly = 0) { txRepo.getTotalByTypeAndDateRange(any(), any(), any()) }
    }

    @Test
    fun `threshold alert when percentage exceeds threshold`() = runTest {
        coEvery { budgetRepo.getByCategory("餐饮") } returns Budget(category = "餐饮", monthlyLimit = 500.0, alertThreshold = 0.8f)
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 400.0

        checker.checkAndNotify("餐饮", "2026-04")

        coVerify { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) }
    }

    @Test
    fun `over budget triggers alert`() = runTest {
        coEvery { budgetRepo.getByCategory("餐饮") } returns Budget(category = "餐饮", monthlyLimit = 500.0, alertThreshold = 0.8f)
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 600.0

        checker.checkAndNotify("餐饮", "2026-04")

        coVerify { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) }
    }
}
