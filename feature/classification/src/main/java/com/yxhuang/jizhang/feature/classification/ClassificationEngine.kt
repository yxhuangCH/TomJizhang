package com.yxhuang.jizhang.feature.classification

import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.model.MatchType

class ClassificationEngine(
    private val ruleRepository: CategoryRuleRepository
) {
    suspend fun classify(merchant: String): ClassificationResult {
        val allRules = ruleRepository.getAllRules()

        // Layer 1: Exact Match
        allRules
            .filter { it.matchType == MatchType.EXACT }
            .firstOrNull { merchant == it.keyword }
            ?.let { return ClassificationResult.Classified(it.category, it.confidence) }

        // Layer 2: Contains Match（按 confidence 降序，命中第一个）
        allRules
            .filter { it.matchType == MatchType.CONTAINS }
            .sortedByDescending { it.confidence }
            .firstOrNull { merchant.contains(it.keyword) }
            ?.let { return ClassificationResult.Classified(it.category, it.confidence) }

        // Layer 3: Unclassified → 触发 LLM 学习
        return ClassificationResult.Unclassified
    }
}
