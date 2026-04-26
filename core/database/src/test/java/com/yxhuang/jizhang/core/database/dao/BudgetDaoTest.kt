package com.yxhuang.jizhang.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yxhuang.jizhang.core.database.JizhangDatabase
import com.yxhuang.jizhang.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetDaoTest {
    private lateinit var db: JizhangDatabase
    private lateinit var dao: BudgetDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, JizhangDatabase::class.java).build()
        dao = db.budgetDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsertAndQuery_returnsCorrectBudget`() = runTest {
        dao.upsert(BudgetEntity(category = "餐饮", monthlyLimit = 1000.0, alertThreshold = 0.8f))

        val result = dao.getByCategory("餐饮")

        assertNotNull(result)
        assertEquals(1000.0, result!!.monthlyLimit, 0.01)
        assertEquals(0.8f, result.alertThreshold, 0.01f)
    }

    @Test
    fun `deleteByCategory_removesBudget`() = runTest {
        dao.upsert(BudgetEntity(category = "交通", monthlyLimit = 500.0, alertThreshold = 0.8f))
        dao.deleteByCategory("交通")

        assertNull(dao.getByCategory("交通"))
    }

    @Test
    fun `observeAll_emits_budget_list`() = runTest {
        dao.upsert(BudgetEntity(category = "餐饮", monthlyLimit = 1000.0, alertThreshold = 0.8f))
        dao.upsert(BudgetEntity(category = "交通", monthlyLimit = 500.0, alertThreshold = 0.8f))

        val list = dao.observeAll().first()

        assertEquals(2, list.size)
    }

    @Test
    fun `upsert_replaces_existing_budget`() = runTest {
        dao.upsert(BudgetEntity(category = "餐饮", monthlyLimit = 1000.0, alertThreshold = 0.8f))
        dao.upsert(BudgetEntity(category = "餐饮", monthlyLimit = 1500.0, alertThreshold = 0.9f))

        val result = dao.getByCategory("餐饮")

        assertEquals(1500.0, result!!.monthlyLimit, 0.01)
        assertEquals(0.9f, result.alertThreshold, 0.01f)
    }
}
