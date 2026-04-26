package com.yxhuang.jizhang.feature.capture.usecase

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.feature.capture.keepalive.BudgetAlertChecker
import com.yxhuang.jizhang.feature.classification.ClassificationEngine
import com.yxhuang.jizhang.feature.classification.ClassificationResult
import com.yxhuang.jizhang.feature.classification.learn.LlmLearningUseCase
import com.yxhuang.jizhang.feature.parser.TransactionParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PersistCapturedTransactionUseCaseTest {
    private val parser = TransactionParser
    private val transactionRepo: TransactionRepository = mockk(relaxed = true)
    private val failureRepo: ParseFailureRepository = mockk(relaxed = true)
    private val classificationEngine = mockk<ClassificationEngine>()
    private val llmLearningUseCase = mockk<LlmLearningUseCase>(relaxed = true)
    private val budgetAlertChecker = mockk<BudgetAlertChecker>(relaxed = true)
    private val useCase = PersistCapturedTransactionUseCase(
        parser, transactionRepo, failureRepo, classificationEngine, llmLearningUseCase
    )
    private val useCaseWithBudgetAlert = PersistCapturedTransactionUseCase(
        parser, transactionRepo, failureRepo, classificationEngine, llmLearningUseCase, budgetAlertChecker
    )

    @Test
    fun `valid payment inserts transaction`() = runTest {
        coEvery { classificationEngine.classify(any()) } returns ClassificationResult.Unclassified
        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCase(notification)
        coVerify(exactly = 1) { transactionRepo.insert(any()) }
        coVerify(exactly = 0) { failureRepo.insert(any()) }
    }

    @Test
    fun `non payment inserts parse failure`() = runTest {
        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信", "你收到一条新消息", null, null, null
        )
        useCase(notification)
        coVerify(exactly = 0) { transactionRepo.insert(any()) }
        coVerify(exactly = 1) { failureRepo.insert(match { it.reason == "Not a payment" }) }
    }

    @Test
    fun `missing merchant inserts parse failure`() = runTest {
        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元", null, null, null
        )
        useCase(notification)
        coVerify(exactly = 0) { transactionRepo.insert(any()) }
        coVerify(exactly = 1) { failureRepo.insert(match { it.reason == "Missing amount or merchant" }) }
    }

    @Test
    fun `valid payment with classified merchant includes category`() = runTest {
        coEvery { classificationEngine.classify("星巴克") } returns
            ClassificationResult.Classified("饮品", 1.0f)

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCase(notification)

        coVerify {
            transactionRepo.insert(match {
                it.merchant == "星巴克" && it.category == "饮品"
            })
        }
    }

    @Test
    fun `valid payment with unclassified merchant has null category`() = runTest {
        coEvery { classificationEngine.classify("未知商户") } returns
            ClassificationResult.Unclassified

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "18.00元 未知商户", null, null, null
        )
        useCase(notification)

        coVerify {
            transactionRepo.insert(match {
                it.merchant == "未知商户" && it.category == null
            })
        }
    }

    @Test
    fun `unclassified merchant triggers llm learning`() = runTest {
        coEvery { classificationEngine.classify("未知商户") } returns
            ClassificationResult.Unclassified

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "18.00元 未知商户", null, null, null
        )
        useCase(notification)

        coVerify { llmLearningUseCase.learnForMerchant("未知商户") }
    }

    @Test
    fun `classified merchant does not trigger llm learning`() = runTest {
        coEvery { classificationEngine.classify("星巴克") } returns
            ClassificationResult.Classified("饮品", 1.0f)

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCase(notification)

        coVerify(exactly = 0) { llmLearningUseCase.learnForMerchant(any()) }
    }

    @Test
    fun `classified merchant with category triggers budget alert check`() = runTest {
        coEvery { classificationEngine.classify("星巴克") } returns
            ClassificationResult.Classified("饮品", 1.0f)

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCaseWithBudgetAlert(notification)

        coVerify { budgetAlertChecker.checkAndNotify("饮品", any()) }
    }

    @Test
    fun `unclassified merchant does not trigger budget alert check`() = runTest {
        coEvery { classificationEngine.classify(any()) } returns
            ClassificationResult.Unclassified

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCaseWithBudgetAlert(notification)

        coVerify(exactly = 0) { budgetAlertChecker.checkAndNotify(any(), any()) }
    }

    @Test
    fun `classified merchant without budget alert checker does not throw`() = runTest {
        coEvery { classificationEngine.classify("星巴克") } returns
            ClassificationResult.Classified("饮品", 1.0f)

        val notification = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25.00元 星巴克", null, null, null
        )
        useCase(notification)

        coVerify(exactly = 0) { budgetAlertChecker.checkAndNotify(any(), any()) }
    }
}
