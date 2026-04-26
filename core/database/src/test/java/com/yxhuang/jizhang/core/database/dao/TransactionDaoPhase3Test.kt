package com.yxhuang.jizhang.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yxhuang.jizhang.core.database.JizhangDatabase
import com.yxhuang.jizhang.core.database.entity.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionDaoPhase3Test {
    private lateinit var db: JizhangDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, JizhangDatabase::class.java).build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `searchByKeyword returns matching merchants`() = runTest {
        dao.insert(tx(merchant = "星巴克"))
        dao.insert(tx(merchant = "瑞幸咖啡"))
        dao.insert(tx(merchant = "滴滴出行"))

        val result = dao.searchByKeyword("星").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("星巴克", list[0].merchant)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchByKeyword with empty string returns all`() = runTest {
        dao.insert(tx(merchant = "A"))
        dao.insert(tx(merchant = "B"))

        dao.searchByKeyword("").test {
            val list = awaitItem()
            assertEquals(2, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getByCategoryAndDateRange filters correctly`() = runTest {
        dao.insert(tx(category = "餐饮", timestamp = 1000L))
        dao.insert(tx(category = "餐饮", timestamp = 2000L))
        dao.insert(tx(category = "交通", timestamp = 1500L))

        val result = dao.getByCategoryAndDateRange("餐饮", 500L, 2500L)

        assertEquals(2, result.size)
        assertTrue(result.all { it.category == "餐饮" })
    }

    @Test
    fun `getByCategoryAndDateRange excludes out of range`() = runTest {
        dao.insert(tx(category = "餐饮", timestamp = 100L))
        dao.insert(tx(category = "餐饮", timestamp = 5000L))

        val result = dao.getByCategoryAndDateRange("餐饮", 1000L, 2000L)

        assertEquals(0, result.size)
    }

    @Test
    fun `getByMerchantName returns exact matches`() = runTest {
        dao.insert(tx(merchant = "星巴克"))
        dao.insert(tx(merchant = "星巴克咖啡"))

        val result = dao.getByMerchantName("星巴克")

        assertEquals(1, result.size)
        assertEquals("星巴克", result[0].merchant)
    }

    @Test
    fun `getTotalByTypeAndDateRange sums correctly`() = runTest {
        dao.insert(tx(type = "EXPENSE", amount = 100.0, timestamp = 1000L))
        dao.insert(tx(type = "EXPENSE", amount = 50.0, timestamp = 2000L))
        dao.insert(tx(type = "INCOME", amount = 5000.0, timestamp = 1500L))

        val expenseTotal = dao.getTotalByTypeAndDateRange("EXPENSE", 500L, 2500L)
        val incomeTotal = dao.getTotalByTypeAndDateRange("INCOME", 500L, 2500L)

        assertEquals(150.0, expenseTotal, 0.01)
        assertEquals(5000.0, incomeTotal, 0.01)
    }

    @Test
    fun `getTotalByTypeAndDateRange returns zero when no matches`() = runTest {
        val total = dao.getTotalByTypeAndDateRange("EXPENSE", 1000L, 2000L)
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun `getCategorySummary groups and orders correctly`() = runTest {
        dao.insert(tx(category = "餐饮", amount = 100.0, type = "EXPENSE"))
        dao.insert(tx(category = "餐饮", amount = 50.0, type = "EXPENSE"))
        dao.insert(tx(category = "交通", amount = 80.0, type = "EXPENSE"))
        dao.insert(tx(category = "餐饮", amount = 30.0, type = "INCOME")) // should be excluded

        val result = dao.getCategorySummary(0L, Long.MAX_VALUE)

        assertEquals(2, result.size)
        assertEquals("餐饮", result[0].category)
        assertEquals(150.0, result[0].total, 0.01)
        assertEquals(2, result[0].count)
        assertEquals("交通", result[1].category)
        assertEquals(80.0, result[1].total, 0.01)
        assertEquals(1, result[1].count)
    }

    @Test
    fun `getCategorySummary returns empty when no expense data`() = runTest {
        val result = dao.getCategorySummary(0L, Long.MAX_VALUE)
        assertTrue(result.isEmpty())
    }

    private fun tx(
        merchant: String = "test",
        amount: Double = 10.0,
        category: String? = null,
        type: String = "EXPENSE",
        timestamp: Long = 1L
    ): TransactionEntity = TransactionEntity(
        amount = amount,
        merchant = merchant,
        category = category,
        type = type,
        timestamp = timestamp,
        sourceApp = "wechat",
        rawText = "raw"
    )
}
