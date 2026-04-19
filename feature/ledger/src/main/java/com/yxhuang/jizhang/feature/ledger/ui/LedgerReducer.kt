package com.yxhuang.jizhang.feature.ledger.ui

import com.yxhuang.jizhang.core.model.Transaction
import kotlinx.collections.immutable.toImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LedgerReducer {

    fun reduce(old: LedgerUiState, transactions: List<Transaction>): LedgerUiState {
        val mapped = transactions.map { it.toUiItem() }
        if (old.items == mapped) return old
        return old.copy(
            items = mapped.toImmutableList(),
            isLoading = false
        )
    }

    private fun Transaction.toUiItem(): TransactionItem {
        return TransactionItem(
            id = id,
            amountText = String.format(Locale.US, "%.2f", amount),
            merchant = merchant,
            category = category,
            timeText = formatter.format(Date(timestamp))
        )
    }

    companion object {
        private val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    }
}
