package com.yxhuang.jizhang.feature.ledger.ui

import app.cash.turbine.test
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val repository: TransactionRepository = mockk()
    private val flow = MutableStateFlow<List<Transaction>>(emptyList())

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits loading then items`() = runTest {
        every { repository.observeAll() } returns flow
        val viewModel = LedgerViewModel(repository, LedgerReducer())

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            flow.value = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(1, state.items.size)
            assertEquals("星巴克", state.items[0].merchant)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState emits empty list when no transactions`() = runTest {
        every { repository.observeAll() } returns flow
        val viewModel = LedgerViewModel(repository, LedgerReducer())

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            flow.value = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))
            val nonEmptyState = awaitItem()
            assertEquals(1, nonEmptyState.items.size)

            flow.value = emptyList()
            val state = awaitItem()
            assertEquals(0, state.items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
