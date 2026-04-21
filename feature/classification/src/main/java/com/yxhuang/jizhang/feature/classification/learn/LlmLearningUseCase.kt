package com.yxhuang.jizhang.feature.classification.learn

import com.yxhuang.jizhang.ai.llm.LlmClient
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.CategoryRule
import com.yxhuang.jizhang.core.model.MatchType
import com.yxhuang.jizhang.feature.classification.quota.DailyQuotaLimiter

class LlmLearningUseCase(
    private val llmClient: LlmClient,
    private val ruleRepository: CategoryRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val quotaLimiter: DailyQuotaLimiter
) {
    suspend fun learnForMerchant(merchant: String): Boolean {
        if (!quotaLimiter.canCall()) {
            LearningQueue.enqueue(merchant)
            return false
        }

        if (ruleRepository.hasRuleCovering(merchant)) return true

        val result = try {
            llmClient.classify(merchant)
        } catch (e: Exception) {
            return false
        }

        quotaLimiter.recordCall()

        val keyword = extractKeyword(result.ruleKeyword, merchant)
        val rule = CategoryRule(
            keyword = keyword,
            category = result.category,
            confidence = result.confidence.coerceAtMost(0.9f),
            matchType = MatchType.CONTAINS
        )
        ruleRepository.insert(rule)

        backfillUnclassifiedTransactions(rule)

        return true
    }

    private fun extractKeyword(rule: String, fallback: String): String {
        return when {
            rule.contains("contains") -> rule.substringAfter("contains").trim()
            rule.contains("==") -> rule.substringAfter("==").trim()
            else -> fallback
        }
    }

    private suspend fun backfillUnclassifiedTransactions(rule: CategoryRule) {
        val unclassified = transactionRepository.getUnclassifiedByMerchantKeyword(rule.keyword)
        unclassified.forEach { tx ->
            transactionRepository.update(tx.copy(category = rule.category))
        }
    }
}
