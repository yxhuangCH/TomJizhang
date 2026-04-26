package com.yxhuang.jizhang.feature.ledger.ui.budget

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class BudgetViewModelTest {

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true and empty data`() {
        val useCase = mockk<BudgetUseCase>()
        val viewModel = BudgetViewModel(useCase)
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertTrue(state.statuses.isEmpty())
        assertFalse(state.showAddDialog)
        assertEquals(YearMonth.now().toString(), state.yearMonth)
    }

    @Test
    fun `loadBudgetStatuses populates state`() = runTest {
        val useCase = mockk<BudgetUseCase>()
        coEvery { useCase.getBudgetStatuses(any()) } returns listOf(
            BudgetStatus("餐饮", 500.0, 300.0, 200.0, 0.6f, false, false)
        )

        val viewModel = BudgetViewModel(useCase)
        viewModel.loadBudgetStatuses(YearMonth.now().toString())

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.statuses.size)
        assertEquals("餐饮", state.statuses[0].category)
        assertEquals(500.0, state.statuses[0].monthlyLimit, 0.01)
        assertEquals(300.0, state.statuses[0].spent, 0.01)
    }

    @Test
    fun `loadBudgetStatuses handles empty budgets`() = runTest {
        val useCase = mockk<BudgetUseCase>()
        coEvery { useCase.getBudgetStatuses(any()) } returns emptyList()

        val viewModel = BudgetViewModel(useCase)
        viewModel.loadBudgetStatuses(YearMonth.now().toString())

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.statuses.isEmpty())
    }

    @Test
    fun `showAddDialog updates state`() {
        val useCase = mockk<BudgetUseCase>()
        val viewModel = BudgetViewModel(useCase)
        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `dismissDialog updates state`() {
        val useCase = mockk<BudgetUseCase>()
        val viewModel = BudgetViewModel(useCase)
        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddDialog)
        viewModel.dismissDialog()
        assertFalse(viewModel.uiState.value.showAddDialog)
    }

    @Test
    fun `setBudget delegates to useCase and refreshes`() = runTest {
        val useCase = mockk<BudgetUseCase>()
        coEvery { useCase.setBudget(any(), any(), any()) } returns Unit
        coEvery { useCase.getBudgetStatuses(any()) } returns emptyList()

        val viewModel = BudgetViewModel(useCase)
        viewModel.loadBudgetStatuses(YearMonth.now().toString())
        viewModel.setBudget("交通", 300.0, 0.8f)

        coVerify { useCase.setBudget("交通", 300.0, 0.8f) }
        coVerify(atLeast = 2) { useCase.getBudgetStatuses(any()) }
    }

    @Test
    fun `deleteBudget delegates to useCase and refreshes`() = runTest {
        val useCase = mockk<BudgetUseCase>()
        coEvery { useCase.deleteBudget(any()) } returns Unit
        coEvery { useCase.getBudgetStatuses(any()) } returns emptyList()

        val viewModel = BudgetViewModel(useCase)
        viewModel.loadBudgetStatuses(YearMonth.now().toString())
        viewModel.deleteBudget("餐饮")

        coVerify { useCase.deleteBudget("餐饮") }
        coVerify(atLeast = 2) { useCase.getBudgetStatuses(any()) }
    }
}
