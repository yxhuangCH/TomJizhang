package com.yxhuang.jizhang.feature.ledger.ui

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LedgerUiState(
    val items: ImmutableList<TransactionItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TransactionItem(
    val id: Long,
    val amountText: String,
    val merchant: String,
    val category: String?,
    val timeText: String
)
