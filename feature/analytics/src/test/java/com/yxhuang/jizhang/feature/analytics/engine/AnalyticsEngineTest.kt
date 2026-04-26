package com.yxhuang.jizhang.feature.analytics.engine

import com.yxhuang.jizhang.core.database.dao.CategorySummary
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZoneId

class AnalyticsEngineTest {
    private val repo = mockk<TransactionRepository>()
    private val engine = AnalyticsEngine(repo)

    @Test
    fun `getMonthlySummaries calculates totals correctly`() = runTest {
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 150.0
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.INCOME, any(), any()) } returns 5000.0

        val result = engine.getMonthlySummaries("2026-04", "2026-04")

        assertEquals(1, result.size)
        assertEquals("2026-04", result[0].yearMonth)
        assertEquals(150.0, result[0].totalExpense, 0.01)
        assertEquals(5000.0, result[0].totalIncome, 0.01)
        assertEquals(4850.0, result[0].netBalance, 0.01)
    }

    @Test
    fun `getCategoryBreakdown calculates percentages`() = runTest {
        coEvery { repo.getCategorySummary(any(), any()) } returns listOf(
            CategorySummary("餐饮", 150.0, 3),
            CategorySummary("交通", 50.0, 1)
        )

        val result = engine.getCategoryBreakdown("2026-04")

        assertEquals(2, result.size)
        assertEquals("餐饮", result[0].category)
        assertEquals(150.0, result[0].amount, 0.01)
        assertEquals(0.75f, result[0].percentage, 0.01f)
        assertEquals(3, result[0].transactionCount)
        assertEquals("交通", result[1].category)
        assertEquals(0.25f, result[1].percentage, 0.01f)
    }

    @Test
    fun `getCategoryBreakdown returns empty list when no data`() = runTest {
        coEvery { repo.getCategorySummary(any(), any()) } returns emptyList()

        val result = engine.getCategoryBreakdown("2026-04")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCategoryBreakdown handles null category as unclassified`() = runTest {
        coEvery { repo.getCategorySummary(any(), any()) } returns listOf(
            CategorySummary(null, 100.0, 2)
        )

        val result = engine.getCategoryBreakdown("2026-04")

        assertEquals(1, result.size)
        assertEquals("未分类", result[0].category)
    }

    @Test
    fun `getTopMerchants returns ordered list with merged amounts`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            tx(1, 100.0, "星巴克", "饮品", date(2026, 4, 1)),
            tx(2, 80.0, "星巴克", "饮品", date(2026, 4, 5)),
            tx(3, 30.0, "滴滴", "交通", date(2026, 4, 3)),
            tx(4, 50.0, "星巴克", "饮品", date(2026, 3, 1)) // different month, filtered out
        )

        val result = engine.getTopMerchants("2026-04", limit = 2)

        assertEquals(2, result.size)
        assertEquals("星巴克", result[0].merchant)
        assertEquals(180.0, result[0].totalAmount, 0.01)
        assertEquals(2, result[0].transactionCount)
        assertEquals("饮品", result[0].category)
        assertEquals("滴滴", result[1].merchant)
        assertEquals(30.0, result[1].totalAmount, 0.01)
    }

    @Test
    fun `getTopMerchants returns empty list when no transactions`() = runTest {
        coEvery { repo.getAll() } returns emptyList()

        val result = engine.getTopMerchants("2026-04")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getDailyTrend returns daily aggregated amounts`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            tx(1, 30.0, "A", "餐饮", date(2026, 4, 1)),
            tx(2, 50.0, "B", "餐饮", date(2026, 4, 1)),
            tx(3, 20.0, "C", "交通", date(2026, 4, 2))
        )

        val result = engine.getDailyTrend("2026-04")

        assertEquals(2, result.size)
        assertEquals("04-01", result[0].label)
        assertEquals(80.0, result[0].amount, 0.01)
        assertEquals("04-02", result[1].label)
        assertEquals(20.0, result[1].amount, 0.01)
    }

    @Test
    fun `getMonthlySummaries for multi month returns correct count`() = runTest {
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.EXPENSE, any(), any()) } returns 100.0
        coEvery { repo.getTotalByTypeAndDateRange(TransactionType.INCOME, any(), any()) } returns 200.0

        val result = engine.getMonthlySummaries("2026-01", "2026-03")

        assertEquals(3, result.size)
        assertEquals("2026-01", result[0].yearMonth)
        assertEquals("2026-02", result[1].yearMonth)
        assertEquals("2026-03", result[2].yearMonth)
    }

    private fun tx(id: Long, amount: Double, merchant: String, category: String?, timestamp: Long): Transaction {
        return Transaction(id, amount, merchant, category, TransactionType.EXPENSE, timestamp, "wechat", "raw")
    }

    private fun date(year: Int, month: Int, day: Int): Long {
        return java.time.LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
