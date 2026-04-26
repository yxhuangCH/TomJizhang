package com.yxhuang.jizhang.feature.classification.learn

import com.yxhuang.jizhang.ai.llm.LlmClassificationResult
import com.yxhuang.jizhang.ai.llm.LlmClient
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.CategoryRule
import com.yxhuang.jizhang.core.model.MatchType
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import com.yxhuang.jizhang.feature.classification.quota.DailyQuotaLimiter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LlmLearningUseCaseTest {
    private val llmClient = mockk<LlmClient>()
    private val ruleRepo = mockk<CategoryRuleRepository>(relaxed = true)
    private val txRepo = mockk<TransactionRepository>(relaxed = true)
    private val quotaLimiter = mockk<DailyQuotaLimiter>(relaxed = true)
    private val useCase = LlmLearningUseCase(llmClient, ruleRepo, txRepo, quotaLimiter)

    @BeforeEach
    fun setup() {
        LearningQueue.dequeueAll()
    }

    @Test
    fun `learnForMerchant creates rule and backfills when quota available`() = runTest {
        coEvery { quotaLimiter.canCall() } returns true
        coEvery { ruleRepo.hasRuleCovering("星巴克") } returns false
        coEvery { llmClient.classify("星巴克") } returns
            LlmClassificationResult("饮品", "merchant contains 星巴克", 0.95f)
        coEvery { txRepo.getUnclassifiedByMerchantKeyword("星巴克") } returns listOf(
            Transaction(1, 25.0, "星巴克", null, TransactionType.EXPENSE, 1000L, "wechat", "raw"),
            Transaction(2, 30.0, "星巴克 coffee", null, TransactionType.EXPENSE, 2000L, "wechat", "raw")
        )

        val success = useCase.learnForMerchant("星巴克")

        assertTrue(success)
        coVerify { quotaLimiter.recordCall() }
        coVerify { ruleRepo.insert(match { it.category == "饮品" && it.keyword == "星巴克" }) }
        coVerify { txRepo.update(match { it.id == 1L && it.category == "饮品" }) }
        coVerify { txRepo.update(match { it.id == 2L && it.category == "饮品" }) }
    }

    @Test
    fun `learnForMerchant caps confidence at 0_9 for llm rules`() = runTest {
        coEvery { quotaLimiter.canCall() } returns true
        coEvery { ruleRepo.hasRuleCovering("星巴克") } returns false
        coEvery { llmClient.classify("星巴克") } returns
            LlmClassificationResult("饮品", "merchant contains 星巴克", 0.95f)
        coEvery { txRepo.getUnclassifiedByMerchantKeyword("星巴克") } returns emptyList()

        useCase.learnForMerchant("星巴克")

        coVerify { ruleRepo.insert(match { it.confidence == 0.9f }) }
    }

    @Test
    fun `learnForMerchant returns false and enqueues when quota exhausted`() = runTest {
        coEvery { quotaLimiter.canCall() } returns false

        val success = useCase.learnForMerchant("星巴克")

        assertFalse(success)
        coVerify(exactly = 0) { llmClient.classify(any()) }
        assertEquals(listOf("星巴克"), LearningQueue.dequeueAll())
    }

    @Test
    fun `learnForMerchant skips when rule already exists`() = runTest {
        coEvery { quotaLimiter.canCall() } returns true
        coEvery { ruleRepo.hasRuleCovering("星巴克") } returns true

        val success = useCase.learnForMerchant("星巴克")

        assertTrue(success)
        coVerify(exactly = 0) { llmClient.classify(any()) }
        coVerify(exactly = 0) { quotaLimiter.recordCall() }
    }

    @Test
    fun `learnForMerchant handles llm exception gracefully`() = runTest {
        coEvery { quotaLimiter.canCall() } returns true
        coEvery { ruleRepo.hasRuleCovering("星巴克") } returns false
        coEvery { llmClient.classify("星巴克") } throws RuntimeException("Network error")

        val success = useCase.learnForMerchant("星巴克")

        assertFalse(success)
        coVerify(exactly = 0) { ruleRepo.insert(any()) }
        coVerify(exactly = 0) { quotaLimiter.recordCall() }
    }
}
