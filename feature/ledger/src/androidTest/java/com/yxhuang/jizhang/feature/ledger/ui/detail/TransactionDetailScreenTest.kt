package com.yxhuang.jizhang.feature.ledger.ui.detail

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class TransactionDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `save button enabled when input is valid`() {
        composeTestRule.setContent {
            TransactionDetailScreen(
                uiState = TransactionDetailUiState(
                    merchant = "星巴克",
                    amountText = "25.00",
                    isLoading = false
                ),
                onBackClick = {},
                onSave = { _, _ -> }
            )
        }
        composeTestRule.onNodeWithText("保存").assertIsEnabled()
    }

    @Test
    fun `save button disabled when merchant is blank`() {
        composeTestRule.setContent {
            TransactionDetailScreen(
                uiState = TransactionDetailUiState(
                    merchant = "",
                    amountText = "25.00",
                    isLoading = false
                ),
                onBackClick = {},
                onSave = { _, _ -> }
            )
        }
        composeTestRule.onNodeWithText("保存").assertIsNotEnabled()
    }
}
