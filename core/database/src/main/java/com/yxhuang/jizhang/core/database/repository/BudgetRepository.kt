package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    suspend fun getByCategory(category: String): Budget?
    suspend fun upsert(budget: Budget): Long
    suspend fun deleteByCategory(category: String)
}
