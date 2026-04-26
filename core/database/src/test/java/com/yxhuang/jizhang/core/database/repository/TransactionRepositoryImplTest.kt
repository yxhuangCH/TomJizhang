package com.yxhuang.jizhang.core.database.repository

import app.cash.turbine.test
import com.yxhuang.jizhang.core.database.dao.TransactionDao
import com.yxhuang.jizhang.core.database.entity.TransactionEntity
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TransactionRepositoryImplTest {
    private val dao: TransactionDao = mockk(relaxed = true)
    private val repository = TransactionRepositoryImpl(dao)

    @Test
    fun `observeAll emits domain models mapped from entities`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(TransactionEntity(1L, 25.0, "星巴克", null, "EXPENSE", 1L, "wechat", "test", 1L))
        )

        repository.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("星巴克", list[0].merchant)
            assertEquals(25.0, list[0].amount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert converts domain model to entity and delegates to dao`() = runTest {
        coEvery { dao.insert(any()) } returns 42L

        val id = repository.insert(
            Transaction(
                amount = 18.5,
                merchant = "滴滴",
                category = null,
                type = TransactionType.EXPENSE,
                timestamp = 2L,
                sourceApp = "alipay",
                rawText = "test"
            )
        )

        assertEquals(42L, id)
        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun `getById returns mapped domain model`() = runTest {
        coEvery { dao.getById(1L) } returns TransactionEntity(
            1L, 25.0, "星巴克", "餐饮", "EXPENSE", 1L, "wechat", "test", 1L
        )

        val result = repository.getById(1L)

        assertNotNull(result)
        assertEquals("星巴克", result?.merchant)
        assertEquals("餐饮", result?.category)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(999L) } returns null

        val result = repository.getById(999L)

        assertNull(result)
    }

    @Test
    fun `update converts domain model to entity`() = runTest {
        coEvery { dao.update(any()) } returns Unit

        repository.update(
            Transaction(
                id = 1L,
                amount = 30.0,
                merchant = "瑞幸咖啡",
                category = "饮品",
                type = TransactionType.EXPENSE,
                timestamp = 1L,
                sourceApp = "wechat",
                rawText = "raw"
            )
        )

        coVerify(exactly = 1) { dao.update(any()) }
    }

    @Test
    fun `observeAll emits empty list when no transactions`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())

        repository.observeAll().test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
