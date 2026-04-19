package com.yxhuang.jizhang.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.yxhuang.jizhang.core.database.dao.CategoryRuleDao
import com.yxhuang.jizhang.core.database.dao.ParseFailureLogDao
import com.yxhuang.jizhang.core.database.dao.TransactionDao
import com.yxhuang.jizhang.core.database.entity.CategoryRuleEntity
import com.yxhuang.jizhang.core.database.entity.ParseFailureLogEntity
import com.yxhuang.jizhang.core.database.entity.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JizhangDatabaseTest {
    private lateinit var db: JizhangDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryRuleDao: CategoryRuleDao
    private lateinit var parseFailureLogDao: ParseFailureLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, JizhangDatabase::class.java).build()
        transactionDao = db.transactionDao()
        categoryRuleDao = db.categoryRuleDao()
        parseFailureLogDao = db.parseFailureLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and observe transactions via flow`() = runTest {
        val entity = TransactionEntity(
            amount = 25.0,
            merchant = "星巴克",
            category = null,
            timestamp = 1000L,
            sourceApp = "wechat",
            rawText = "test"
        )
        transactionDao.insert(entity)

        transactionDao.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("星巴克", list[0].merchant)
            assertEquals(25.0, list[0].amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `update transaction changes merchant`() = runTest {
        val id = transactionDao.insert(
            TransactionEntity(
                amount = 10.0,
                merchant = "A",
                category = null,
                timestamp = 1L,
                sourceApp = "wechat",
                rawText = "test"
            )
        )
        transactionDao.update(
            TransactionEntity(
                id = id,
                amount = 10.0,
                merchant = "B",
                category = null,
                timestamp = 1L,
                sourceApp = "wechat",
                rawText = "test"
            )
        )
        val updated = transactionDao.getById(id)
        assertEquals("B", updated?.merchant)
    }

    @Test
    fun `getById returns null for nonexistent transaction`() = runTest {
        val result = transactionDao.getById(999L)
        assertNull(result)
    }

    @Test
    fun `insert and observe category rules`() = runTest {
        val rule = CategoryRuleEntity(
            keyword = "星巴克",
            category = "饮品",
            confidence = 1.0f
        )
        categoryRuleDao.insert(rule)

        categoryRuleDao.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("星巴克", list[0].keyword)
            assertEquals("饮品", list[0].category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert parse failure log`() = runTest {
        val log = ParseFailureLogEntity(
            rawText = "failed text",
            sourceApp = "wechat",
            timestamp = 1000L,
            reason = "No amount"
        )
        val id = parseFailureLogDao.insert(log)

        val retrieved = parseFailureLogDao.getById(id)
        assertEquals("failed text", retrieved?.rawText)
        assertEquals("No amount", retrieved?.reason)
    }

    @Test
    fun `transactions ordered by timestamp descending`() = runTest {
        transactionDao.insert(
            TransactionEntity(
                amount = 10.0,
                merchant = "Older",
                category = null,
                timestamp = 100L,
                sourceApp = "wechat",
                rawText = "test"
            )
        )
        transactionDao.insert(
            TransactionEntity(
                amount = 20.0,
                merchant = "Newer",
                category = null,
                timestamp = 200L,
                sourceApp = "wechat",
                rawText = "test"
            )
        )

        transactionDao.observeAll().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals("Newer", list[0].merchant)
            assertEquals("Older", list[1].merchant)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
