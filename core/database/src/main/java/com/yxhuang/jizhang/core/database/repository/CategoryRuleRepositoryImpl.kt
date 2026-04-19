package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.CategoryRuleDao
import com.yxhuang.jizhang.core.database.entity.CategoryRuleEntity
import com.yxhuang.jizhang.core.model.CategoryRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRuleRepositoryImpl(
    private val dao: CategoryRuleDao
) : CategoryRuleRepository {

    override fun observeAll(): Flow<List<CategoryRule>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insert(rule: CategoryRule): Long {
        return dao.insert(rule.toEntity())
    }

    override suspend fun getById(id: Long): CategoryRule? {
        return dao.getById(id)?.toDomain()
    }

    private fun CategoryRuleEntity.toDomain(): CategoryRule {
        return CategoryRule(
            id = id,
            keyword = keyword,
            category = category,
            confidence = confidence
        )
    }

    private fun CategoryRule.toEntity(): CategoryRuleEntity {
        return CategoryRuleEntity(
            id = id,
            keyword = keyword,
            category = category,
            confidence = confidence
        )
    }
}
