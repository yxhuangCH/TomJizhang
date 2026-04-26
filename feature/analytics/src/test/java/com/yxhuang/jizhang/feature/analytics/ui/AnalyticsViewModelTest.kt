package com.yxhuang.jizhang.feature.analytics.ui

import com.yxhuang.jizhang.core.database.dao.CategorySummary
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.TransactionType
import com.yxhuang.jizhang.feature.analytics.engine.AnalyticsEngine
import com.yxhuang.jizhang.feature.analytics.engine.RecurringDetector
import com.yxhuang.jizhang.feature.analytics.engine.RecurringFrequency
import com.yxhuang.jizhang.feature.analytics.engine.RecurringPattern
import io.mockk.coEvery
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

class AnalyticsViewModelTest {

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has current yearMonth and empty data`() {
        val viewModel = AnalyticsViewModel(AnalyticsEngine(mockk(relaxed = true)), mockk(relaxed = true))
        val state = viewModel.uiState.value
        assertTrue(state.yearMonth.isNotEmpty())
        assertTrue(state.isLoading)
        assertTrue(state.monthlySummaries.isEmpty())
        assertTrue(state.categoryBreakdown.isEmpty())
        assertTrue(state.recurringPatterns.isEmpty())
    }

    @Test
    fun `loadData populates monthly summaries and category breakdown`() = runTest {
        val repo = mockk<TransactionRepository>()
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 150.0
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.INCOME, any(), any()) } returns 5000.0
        coEvery { repo.getCategorySummary(any(), any()) } returns listOf(
            CategorySummary("餐饮", 150.0, 3)
        )
        coEvery { repo.getAll() } returns emptyList()

        val viewModel = AnalyticsViewModel(AnalyticsEngine(repo), mockk(relaxed = true))
        viewModel.loadData("2026-04")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.monthlySummaries.size)
        assertEquals(150.0, state.monthlySummaries[0].totalExpense, 0.01)
        assertEquals(1, state.categoryBreakdown.size)
        assertEquals("餐饮", state.categoryBreakdown[0].category)
    }

    @Test
    fun `selectYearMonth updates state`() {
        val viewModel = AnalyticsViewModel(AnalyticsEngine(mockk(relaxed = true)), mockk(relaxed = true))
        viewModel.selectYearMonth("2026-03")

        val state = viewModel.uiState.value
        assertEquals("2026-03", state.yearMonth)
    }

    @Test
    fun `switchTab changes selected tab index`() {
        val viewModel = AnalyticsViewModel(AnalyticsEngine(mockk(relaxed = true)), mockk(relaxed = true))
        viewModel.switchTab(1)
        assertEquals(1, viewModel.uiState.value.selectedTab)

        viewModel.switchTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `switching to recurring tab triggers pattern loading`() = runTest {
        val detector = mockk<RecurringDetector>()
        coEvery { detector.detect() } returns listOf(
            RecurringPattern(
                merchant = "房东",
                category = "住房",
                averageAmount = 3000.0,
                type = TransactionType.EXPENSE,
                frequency = RecurringFrequency.MONTHLY,
                lastOccurrence = 1000L,
                nextEstimatedOccurrence = 2000L,
                confidence = 0.95f
            )
        )

        val viewModel = AnalyticsViewModel(AnalyticsEngine(mockk(relaxed = true)), detector)
        viewModel.switchTab(2)

        val state = viewModel.uiState.value
        assertEquals(2, state.selectedTab)
        assertEquals(1, state.recurringPatterns.size)
        assertEquals("房东", state.recurringPatterns[0].merchant)
        assertEquals(0.95f, state.recurringPatterns[0].confidence)
    }

    @Test
    fun `recurring tab shows empty list when no patterns detected`() = runTest {
        val detector = mockk<RecurringDetector>()
        coEvery { detector.detect() } returns emptyList()

        val viewModel = AnalyticsViewModel(AnalyticsEngine(mockk(relaxed = true)), detector)
        viewModel.switchTab(2)

        val state = viewModel.uiState.value
        assertEquals(2, state.selectedTab)
        assertTrue(state.recurringPatterns.isEmpty())
    }
}
