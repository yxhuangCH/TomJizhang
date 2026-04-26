package com.yxhuang.jizhang.feature.analytics.engine

import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZoneId

class RecurringDetectorTest {
    private val repo = mockk<TransactionRepository>()
    private val detector = RecurringDetector(repo)

    @Test
    fun `detects monthly rent payment`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("房东", 3000.0, date(2026, 1, 1)),
            transaction("房东", 3000.0, date(2026, 2, 1)),
            transaction("房东", 3000.0, date(2026, 3, 1)),
            transaction("房东", 3000.0, date(2026, 4, 1))
        )

        val patterns = detector.detect()

        assertEquals(1, patterns.size)
        assertEquals("房东", patterns[0].merchant)
        assertEquals(RecurringFrequency.MONTHLY, patterns[0].frequency)
        assertTrue(patterns[0].confidence > 0.8f)
    }

    @Test
    fun `detects weekly subscription`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("Netflix", 79.0, date(2026, 3, 4)),
            transaction("Netflix", 79.0, date(2026, 3, 11)),
            transaction("Netflix", 79.0, date(2026, 3, 18)),
            transaction("Netflix", 79.0, date(2026, 3, 25))
        )

        val patterns = detector.detect()

        assertEquals(1, patterns.size)
        assertEquals(RecurringFrequency.WEEKLY, patterns[0].frequency)
    }

    @Test
    fun `ignores irregular transactions`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("星巴克", 30.0, date(2026, 4, 1)),
            transaction("星巴克", 25.0, date(2026, 4, 5)),
            transaction("星巴克", 28.0, date(2026, 4, 12))
        )

        val patterns = detector.detect()

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `ignores single occurrence merchants`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("未知商户", 100.0, date(2026, 4, 1))
        )

        val patterns = detector.detect()

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `returns empty list when no transactions`() = runTest {
        coEvery { repo.getAll() } returns emptyList()

        val patterns = detector.detect()

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `confidence decreases with amount variance`() = runTest {
        coEvery { repo.getAll() } returns listOf(
            transaction("水电费", 200.0, date(2026, 1, 5)),
            transaction("水电费", 180.0, date(2026, 2, 5)),
            transaction("水电费", 220.0, date(2026, 3, 5)),
            transaction("水电费", 190.0, date(2026, 4, 5))
        )

        val patterns = detector.detect()

        assertEquals(1, patterns.size)
        assertTrue(patterns[0].confidence in 0.5f..0.9f)
    }

    private fun transaction(merchant: String, amount: Double, timestamp: Long): Transaction {
        return Transaction(
            amount = amount, merchant = merchant,
            category = null, type = TransactionType.EXPENSE,
            timestamp = timestamp, sourceApp = "wechat", rawText = ""
        )
    }

    private fun date(year: Int, month: Int, day: Int): Long {
        return java.time.LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
    }
}
