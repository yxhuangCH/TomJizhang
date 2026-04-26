package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.CategorySummary
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    suspend fun getAll(): List<Transaction>
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun getById(id: Long): Transaction?
    suspend fun getUnclassifiedByMerchantKeyword(keyword: String): List<Transaction>
    suspend fun deleteAll()

    // Phase 3
    fun searchByKeyword(keyword: String): Flow<List<Transaction>>
    suspend fun getByCategoryAndDateRange(category: String, startTime: Long, endTime: Long): List<Transaction>
    suspend fun getByMerchantName(merchant: String): List<Transaction>
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startTime: Long, endTime: Long): Double
    suspend fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary>
}
