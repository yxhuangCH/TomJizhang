package com.yxhuang.jizhang.feature.ledger.ui.search

import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionSearchEngine(
    private val transactionRepository: TransactionRepository
) {
    fun search(query: SearchQuery): Flow<List<Transaction>> {
        return transactionRepository.searchByKeyword(query.keyword)
            .map { transactions ->
                transactions.filter { matches(it, query) }
            }
    }

    private fun matches(transaction: Transaction, query: SearchQuery): Boolean {
        if (query.categories.isNotEmpty() &&
            transaction.category !in query.categories) return false

        if (query.startTime != null && transaction.timestamp < query.startTime) return false
        if (query.endTime != null && transaction.timestamp > query.endTime) return false

        if (query.minAmount != null && transaction.amount < query.minAmount) return false
        if (query.maxAmount != null && transaction.amount > query.maxAmount) return false

        return true
    }
}
