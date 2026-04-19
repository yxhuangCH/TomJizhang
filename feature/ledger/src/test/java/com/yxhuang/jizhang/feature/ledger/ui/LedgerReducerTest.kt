package com.yxhuang.jizhang.feature.ledger.ui

import com.yxhuang.jizhang.core.model.Transaction
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LedgerReducerTest {
    private val reducer = LedgerReducer()

    @Test
    fun `reduce empty list returns empty state`() {
        val result = reducer.reduce(LedgerUiState(), emptyList())
        assertTrue(result.items.isEmpty())
        assertFalse(result.isLoading)
    }

    @Test
    fun `reduce same list returns old state reference`() {
        val transactions = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))
        val old = reducer.reduce(LedgerUiState(), transactions)
        val new = reducer.reduce(old, transactions)
        assertSame(old, new)
    }

    @Test
    fun `reduce different list returns new state`() {
        val t1 = listOf(Transaction(1L, 25.0, "星巴克", null, 1L, "wechat", "test"))
        val t2 = listOf(Transaction(1L, 25.0, "星巴克", "餐饮", 1L, "wechat", "test"))
        val old = reducer.reduce(LedgerUiState(), t1)
        val new = reducer.reduce(old, t2)
        assertNotSame(old, new)
        assertEquals("餐饮", new.items[0].category)
    }

    @Test
    fun `reduce maps transaction to item correctly`() {
        val transactions = listOf(
            Transaction(1L, 25.0, "星巴克", "饮品", 1000L, "wechat", "raw")
        )
        val result = reducer.reduce(LedgerUiState(), transactions)
        assertEquals(1, result.items.size)
        val item = result.items[0]
        assertEquals(1L, item.id)
        assertEquals("星巴克", item.merchant)
        assertEquals("饮品", item.category)
        assertEquals("25.00", item.amountText)
    }
}
