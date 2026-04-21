package com.yxhuang.jizhang.feature.ledger.ui.settings

import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository

class ClearDataUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val parseFailureRepository: ParseFailureRepository
) {
    suspend fun clearAllTransactions() {
        transactionRepository.deleteAll()
    }

    suspend fun clearAllRules() {
        categoryRuleRepository.deleteAll()
    }

    suspend fun clearAll() {
        clearAllTransactions()
        clearAllRules()
        parseFailureRepository.deleteAll()
    }
}
