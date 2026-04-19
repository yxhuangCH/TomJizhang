package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun getById(id: Long): Transaction?
}
