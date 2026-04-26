package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.BudgetDao
import com.yxhuang.jizhang.core.database.entity.BudgetEntity
import com.yxhuang.jizhang.core.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun observeAll(): Flow<List<Budget>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getByCategory(category: String): Budget? {
        return dao.getByCategory(category)?.toDomain()
    }

    override suspend fun upsert(budget: Budget): Long {
        return dao.upsert(budget.toEntity())
    }

    override suspend fun deleteByCategory(category: String) {
        dao.deleteByCategory(category)
    }

    private fun BudgetEntity.toDomain(): Budget {
        return Budget(
            id = id,
            category = category,
            monthlyLimit = monthlyLimit,
            alertThreshold = alertThreshold
        )
    }

    private fun Budget.toEntity(): BudgetEntity {
        return BudgetEntity(
            id = id,
            category = category,
            monthlyLimit = monthlyLimit,
            alertThreshold = alertThreshold
        )
    }
}
