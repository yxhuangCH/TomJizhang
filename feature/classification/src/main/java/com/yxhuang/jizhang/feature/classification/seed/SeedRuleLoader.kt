package com.yxhuang.jizhang.feature.classification.seed

import android.content.Context
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.model.CategoryRule
import com.yxhuang.jizhang.core.model.MatchType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SeedRuleLoader(
    private val context: Context,
    private val ruleRepository: CategoryRuleRepository
) {
    suspend fun loadIfEmpty() {
        if (ruleRepository.count() > 0) return
        val rules = parseSeedRules()
        rules.forEach { ruleRepository.insert(it) }
    }

    fun parseSeedRules(): List<CategoryRule> {
        val json = context.assets.open("seed_rules.json").bufferedReader().use { it.readText() }
        return Json.decodeFromString<List<SeedRule>>(json).map {
            CategoryRule(
                keyword = it.keyword,
                category = it.category,
                confidence = it.confidence,
                matchType = MatchType.valueOf(it.matchType.uppercase())
            )
        }
    }
}

@Serializable
private data class SeedRule(
    val keyword: String,
    val category: String,
    val confidence: Float,
    val matchType: String
)
