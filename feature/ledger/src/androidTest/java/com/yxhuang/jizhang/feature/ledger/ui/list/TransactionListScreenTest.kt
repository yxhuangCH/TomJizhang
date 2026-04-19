package com.yxhuang.jizhang.feature.ledger.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.yxhuang.jizhang.feature.ledger.ui.LedgerUiState
import com.yxhuang.jizhang.feature.ledger.ui.TransactionItem

class TransactionListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `transaction list renders correct item count`() {
        val items = persistentListOf(
            TransactionItem(1L, "25.00", "星巴克", "餐饮", "10:30"),
            TransactionItem(2L, "18.50", "滴滴出行", "交通", "14:20")
        )
        composeTestRule.setContent {
            TransactionListScreen(
                uiState = LedgerUiState(items = items),
                onTransactionClick = {}
            )
        }
        composeTestRule.onNodeWithText("星巴克").assertIsDisplayed()
        composeTestRule.onNodeWithText("滴滴出行").assertIsDisplayed()
    }

    @Test
    fun `clicking item triggers callback`() {
        var clickedId: Long? = null
        val items = persistentListOf(TransactionItem(1L, "25.00", "星巴克", "餐饮", "10:30"))
        composeTestRule.setContent {
            TransactionListScreen(
                uiState = LedgerUiState(items = items),
                onTransactionClick = { clickedId = it }
            )
        }
        composeTestRule.onNodeWithText("星巴克").performClick()
        assertEquals(1L, clickedId)
    }
}
