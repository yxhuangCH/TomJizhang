package com.yxhuang.jizhang.feature.classification

import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.model.CategoryRule
import com.yxhuang.jizhang.core.model.MatchType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassificationEngineTest {
    private val ruleRepo = mockk<CategoryRuleRepository>()
    private val engine = ClassificationEngine(ruleRepo)

    @Test
    fun `exact match returns highest confidence`() = runTest {
        coEvery { ruleRepo.getAllRules() } returns listOf(
            CategoryRule(1, "星巴克", "饮品", 1.0f, MatchType.EXACT),
            CategoryRule(2, "滴滴", "交通", 0.9f, MatchType.CONTAINS)
        )

        val result = engine.classify("星巴克")
        assertEquals(ClassificationResult.Classified("饮品", 1.0f), result)
    }

    @Test
    fun `contains match when no exact`() = runTest {
        coEvery { ruleRepo.getAllRules() } returns listOf(
            CategoryRule(1, "滴滴", "交通", 0.9f, MatchType.CONTAINS)
        )

        val result = engine.classify("滴滴快车")
        assertEquals(ClassificationResult.Classified("交通", 0.9f), result)
    }

    @Test
    fun `no match returns unclassified`() = runTest {
        coEvery { ruleRepo.getAllRules() } returns emptyList()

        val result = engine.classify("未知商户XYZ")
        assertEquals(ClassificationResult.Unclassified, result)
    }

    @Test
    fun `contains match respects confidence ordering`() = runTest {
        coEvery { ruleRepo.getAllRules() } returns listOf(
            CategoryRule(1, "滴滴", "交通", 0.8f, MatchType.CONTAINS),
            CategoryRule(2, "滴滴", "出行", 0.95f, MatchType.CONTAINS)
        )

        val result = engine.classify("滴滴顺风车")
        assertEquals(ClassificationResult.Classified("出行", 0.95f), result)
    }

    @Test
    fun `exact match takes precedence over contains`() = runTest {
        coEvery { ruleRepo.getAllRules() } returns listOf(
            CategoryRule(1, "星巴克", "饮品", 1.0f, MatchType.EXACT),
            CategoryRule(2, "星巴克", "咖啡", 0.9f, MatchType.CONTAINS)
        )

        val result = engine.classify("星巴克")
        assertEquals(ClassificationResult.Classified("饮品", 1.0f), result)
    }
}
