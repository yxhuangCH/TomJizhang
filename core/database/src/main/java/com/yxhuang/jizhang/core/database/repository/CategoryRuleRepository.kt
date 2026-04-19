package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.model.CategoryRule
import kotlinx.coroutines.flow.Flow

interface CategoryRuleRepository {
    fun observeAll(): Flow<List<CategoryRule>>
    suspend fun insert(rule: CategoryRule): Long
    suspend fun getById(id: Long): CategoryRule?
}
