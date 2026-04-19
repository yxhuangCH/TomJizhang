package com.yxhuang.jizhang.feature.ledger.ui.detail

import app.cash.turbine.test
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionDetailViewModelTest {
    private val repo = mockk<TransactionRepository>()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load populates state with existing transaction`() = runTest {
        coEvery { repo.getById(1L) } returns Transaction(
            1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test"
        )

        val viewModel = TransactionDetailViewModel(1L, repo)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("星巴克", state.merchant)
            assertEquals("餐饮", state.category)
            assertEquals("25.00", state.amountText)
            assertEquals(false, state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save updates repository`() = runTest {
        coEvery { repo.getById(1L) } returns Transaction(
            1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test"
        )
        coEvery { repo.update(any()) } returns Unit

        val viewModel = TransactionDetailViewModel(1L, repo)
        viewModel.save(merchant = "瑞幸咖啡", category = "饮品")

        coVerify {
            repo.update(match { it.merchant == "瑞幸咖啡" && it.category == "饮品" })
        }
    }
}
