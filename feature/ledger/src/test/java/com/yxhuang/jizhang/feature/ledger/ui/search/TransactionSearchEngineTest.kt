package com.yxhuang.jizhang.feature.ledger.ui.search

import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransactionSearchEngineTest {
    private val repo = mockk<TransactionRepository>()
    private val engine = TransactionSearchEngine(repo)

    @Test
    fun `search by keyword matches merchant`() = runTest {
        every { repo.searchByKeyword("星") } returns flowOf(listOf(
            tx(1, 30.0, "星巴克", "饮品"),
            tx(2, 25.0, "星巴克咖啡", "饮品")
        ))

        val result = engine.search(SearchQuery(keyword = "星")).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `search by category filters correctly`() = runTest {
        every { repo.searchByKeyword("") } returns flowOf(listOf(
            tx(1, 30.0, "星巴克", "饮品"),
            tx(2, 50.0, "滴滴", "交通"),
            tx(3, 100.0, "超市", "日用")
        ))

        val result = engine.search(SearchQuery(categories = listOf("饮品", "日用"))).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.category in listOf("饮品", "日用") })
    }

    @Test
    fun `search by amount range works`() = runTest {
        every { repo.searchByKeyword("") } returns flowOf(listOf(
            tx(1, 30.0, "星巴克", "饮品"),
            tx(2, 50.0, "滴滴", "交通"),
            tx(3, 100.0, "超市", "日用")
        ))

        val result = engine.search(SearchQuery(minAmount = 40.0, maxAmount = 80.0)).first()

        assertEquals(1, result.size)
        assertEquals(50.0, result[0].amount, 0.01)
    }

    @Test
    fun `combined query applies all conditions`() = runTest {
        every { repo.searchByKeyword("星") } returns flowOf(listOf(
            tx(1, 30.0, "星巴克", "饮品"),
            tx(2, 25.0, "星巴克咖啡", "饮品")
        ))

        val result = engine.search(SearchQuery(
            keyword = "星",
            minAmount = 28.0,
            categories = listOf("饮品")
        )).first()

        assertEquals(1, result.size)
        assertEquals("星巴克", result[0].merchant)
    }

    @Test
    fun `empty query returns all transactions`() = runTest {
        val allTransactions = listOf(
            tx(1, 30.0, "星巴克", "饮品"),
            tx(2, 50.0, "滴滴", "交通")
        )
        every { repo.searchByKeyword("") } returns flowOf(allTransactions)

        val result = engine.search(SearchQuery()).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `search by time range filters correctly`() = runTest {
        every { repo.searchByKeyword("") } returns flowOf(listOf(
            tx(1, 30.0, "A", "餐饮", timestamp = 1000L),
            tx(2, 50.0, "B", "餐饮", timestamp = 2000L),
            tx(3, 80.0, "C", "餐饮", timestamp = 3000L)
        ))

        val result = engine.search(SearchQuery(startTime = 1500L, endTime = 2500L)).first()

        assertEquals(1, result.size)
        assertEquals("B", result[0].merchant)
    }

    private fun tx(id: Long, amount: Double, merchant: String, category: String?, timestamp: Long = 1L): Transaction {
        return Transaction(id, amount, merchant, category, TransactionType.EXPENSE, timestamp, "wechat", "raw")
    }
}
