package com.yxhuang.jizhang.feature.ledger.ui

import app.cash.turbine.test
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import com.yxhuang.jizhang.feature.ledger.ui.search.SearchQuery
import com.yxhuang.jizhang.feature.ledger.ui.search.TransactionSearchEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LedgerViewModelTest {
    private val searchEngine: TransactionSearchEngine = mockk()
    private val reducer = LedgerReducer()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchByKeyword updates uiState`() = runTest {
        every { searchEngine.search(SearchQuery(keyword = "")) } returns flowOf(listOf(
            Transaction(1L, 25.0, "星巴克", "饮品", TransactionType.EXPENSE, 1L, "wechat", "raw"),
            Transaction(2L, 100.0, "超市", "日用", TransactionType.EXPENSE, 2L, "wechat", "raw")
        ))
        every { searchEngine.search(SearchQuery(keyword = "星")) } returns flowOf(listOf(
            Transaction(1L, 25.0, "星巴克", "饮品", TransactionType.EXPENSE, 1L, "wechat", "raw")
        ))

        val viewModel = LedgerViewModel(searchEngine, reducer)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(2, initial.items.size)
            assertFalse(initial.isSearchActive)

            viewModel.setSearchKeyword("星")
            val filteredState = awaitItem()
            assertEquals(1, filteredState.items.size)
            assertEquals("星巴克", filteredState.items[0].merchant)
            assertTrue(filteredState.isSearchActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing search returns to full list`() = runTest {
        every { searchEngine.search(SearchQuery(keyword = "")) } returns flowOf(listOf(
            Transaction(1L, 25.0, "星巴克", "饮品", TransactionType.EXPENSE, 1L, "wechat", "raw"),
            Transaction(2L, 100.0, "超市", "日用", TransactionType.EXPENSE, 2L, "wechat", "raw")
        ))
        every { searchEngine.search(SearchQuery(keyword = "星")) } returns flowOf(listOf(
            Transaction(1L, 25.0, "星巴克", "饮品", TransactionType.EXPENSE, 1L, "wechat", "raw")
        ))

        val viewModel = LedgerViewModel(searchEngine, reducer)

        viewModel.uiState.test {
            awaitItem()

            viewModel.setSearchKeyword("星")
            awaitItem()

            every { searchEngine.search(SearchQuery(keyword = "")) } returns flowOf(listOf(
                Transaction(1L, 25.0, "星巴克", "饮品", TransactionType.EXPENSE, 1L, "wechat", "raw"),
                Transaction(2L, 100.0, "超市", "日用", TransactionType.EXPENSE, 2L, "wechat", "raw")
            ))
            viewModel.setSearchKeyword("")
            val fullState = awaitItem()
            assertEquals(2, fullState.items.size)
            assertFalse(fullState.isSearchActive)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
