package com.yxhuang.jizhang.feature.capture.usecase

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.feature.parser.TransactionParser
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PersistCapturedTransactionUseCaseTest {
    private val parser = TransactionParser
    private val transactionRepo: TransactionRepository = mockk(relaxed = true)
    private val failureRepo: ParseFailureRepository = mockk(relaxed = true)
    private val useCase = PersistCapturedTransactionUseCase(parser, transactionRepo, failureRepo)

    @Test
    fun `valid payment inserts transaction`() = runTest {
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
}
