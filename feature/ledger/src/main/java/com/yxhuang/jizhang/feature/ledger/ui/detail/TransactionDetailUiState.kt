package com.yxhuang.jizhang.feature.ledger.ui.detail

data class TransactionDetailUiState(
    val id: Long = 0,
    val amountText: String = "",
    val merchant: String = "",
    val category: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false
)
