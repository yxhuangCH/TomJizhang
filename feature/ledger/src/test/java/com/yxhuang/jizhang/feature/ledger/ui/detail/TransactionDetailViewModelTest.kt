package com.yxhuang.jizhang.feature.ledger.ui.detail

import app.cash.turbine.test
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.MatchType
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
    private val ruleRepo = mockk<CategoryRuleRepository>(relaxed = true)

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

        val viewModel = TransactionDetailViewModel(1L, repo, ruleRepo)

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

        val viewModel = TransactionDetailViewModel(1L, repo, ruleRepo)
        viewModel.save(merchant = "瑞幸咖啡", category = "饮品")

        coVerify {
            repo.update(match { it.merchant == "瑞幸咖啡" && it.category == "饮品" })
        }
    }

    @Test
    fun `save with changed category creates new exact rule`() = runTest {
        coEvery { repo.getById(1L) } returns Transaction(
            1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test"
        )
        coEvery { repo.update(any()) } returns Unit

        val viewModel = TransactionDetailViewModel(1L, repo, ruleRepo)
        viewModel.save(merchant = "星巴克", category = "饮品")

        coVerify {
            ruleRepo.insert(match {
                it.keyword == "星巴克" && it.category == "饮品" &&
                    it.confidence == 1.0f && it.matchType == MatchType.EXACT
            })
        }
        coVerify {
            repo.update(match { it.category == "饮品" })
        }
    }

    @Test
    fun `save with same category does not create rule`() = runTest {
        coEvery { repo.getById(1L) } returns Transaction(
            1L, 25.0, "星巴克", "饮品", 1L, "wechat", "test"
        )
        coEvery { repo.update(any()) } returns Unit

        val viewModel = TransactionDetailViewModel(1L, repo, ruleRepo)
        viewModel.save(merchant = "星巴克", category = "饮品")

        coVerify(exactly = 0) { ruleRepo.insert(any()) }
        coVerify { repo.update(any()) }
    }
}
