package com.yxhuang.jizhang.core.database.repository

import com.yxhuang.jizhang.core.database.dao.CategoryRuleDao
import com.yxhuang.jizhang.core.database.entity.CategoryRuleEntity
import com.yxhuang.jizhang.core.model.CategoryRule
import com.yxhuang.jizhang.core.model.MatchType
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

    override suspend fun getAllRules(): List<CategoryRule> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun insert(rule: CategoryRule): Long {
        return dao.insert(rule.toEntity())
    }

    override suspend fun getById(id: Long): CategoryRule? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun count(): Int {
        return dao.count()
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    override suspend fun hasRuleCovering(merchant: String): Boolean {
        val rules = dao.getAll()
        return rules.any { rule ->
            when (rule.matchType) {
                MatchType.EXACT.name -> merchant == rule.keyword
                else -> merchant.contains(rule.keyword)
            }
        }
    }

    private fun CategoryRuleEntity.toDomain(): CategoryRule {
        return CategoryRule(
            id = id,
            keyword = keyword,
            category = category,
            confidence = confidence,
            matchType = try {
                MatchType.valueOf(matchType)
            } catch (_: IllegalArgumentException) {
                MatchType.CONTAINS
            }
        )
    }

    private fun CategoryRule.toEntity(): CategoryRuleEntity {
        return CategoryRuleEntity(
            id = id,
            keyword = keyword,
            category = category,
            confidence = confidence,
            matchType = matchType.name
        )
    }
}
