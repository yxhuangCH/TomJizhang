package com.yxhuang.jizhang.feature.ledger.ui.budget

import com.yxhuang.jizhang.core.database.repository.BudgetRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Budget
import com.yxhuang.jizhang.core.model.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BudgetUseCaseTest {
    private val budgetRepo = mockk<BudgetRepository>()
    private val txRepo = mockk<TransactionRepository>()
    private val useCase = BudgetUseCase(budgetRepo, txRepo)

    @Test
    fun `getBudgetStatuses calculates spent correctly`() = runTest {
        val yearMonth = YearMonth.now().toString()
        coEvery { budgetRepo.observeAll() } returns flowOf(listOf(
            Budget(category = "餐饮", monthlyLimit = 500.0, alertThreshold = 0.8f)
        ))
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 450.0

        val statuses = useCase.getBudgetStatuses(yearMonth)

        assertEquals(1, statuses.size)
        assertEquals(0.9f, statuses[0].percentage, 0.01f)
        assertFalse(statuses[0].isOverBudget)
        assertTrue(statuses[0].isAlertTriggered)
        assertEquals(50.0, statuses[0].remaining, 0.01)
    }

    @Test
    fun `over budget triggers isOverBudget flag`() = runTest {
        val yearMonth = YearMonth.now().toString()
        coEvery { budgetRepo.observeAll() } returns flowOf(listOf(
            Budget(category = "交通", monthlyLimit = 200.0, alertThreshold = 0.8f)
        ))
        coEvery { txRepo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 250.0

        val statuses = useCase.getBudgetStatuses(yearMonth)

        assertTrue(statuses[0].isOverBudget)
        assertTrue(statuses[0].isAlertTriggered)
    }

    @Test
    fun `no budget returns empty list`() = runTest {
        coEvery { budgetRepo.observeAll() } returns flowOf(emptyList())

        val statuses = useCase.getBudgetStatuses(YearMonth.now().toString())

        assertTrue(statuses.isEmpty())
    }

    @Test
    fun `setBudget delegates to repository`() = runTest {
        coEvery { budgetRepo.upsert(any()) } returns 1L

        useCase.setBudget("餐饮", 1000.0, 0.8f)
    }

    @Test
    fun `deleteBudget delegates to repository`() = runTest {
        coEvery { budgetRepo.deleteByCategory(any()) } returns Unit

        useCase.deleteBudget("餐饮")
    }
}
