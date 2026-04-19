package com.yxhuang.jizhang.feature.capture.notification

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.feature.capture.dedup.NotificationDeduplicator
import com.yxhuang.jizhang.feature.capture.usecase.PersistCapturedTransactionUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CaptureNotificationHandlerTest {
    private val deduplicator = mockk<NotificationDeduplicator>()
    private val persistUseCase = mockk<PersistCapturedTransactionUseCase>(relaxed = true)
    private val handler = CaptureNotificationHandler(deduplicator, persistUseCase)

    @Test
    fun `processes target package notification`() = runTest {
        every { deduplicator.isDuplicate(any()) } returns false
        val data = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25元 星巴克", null, null, null
        )

        handler.handle(data)

        coVerify(exactly = 1) { persistUseCase(data) }
    }

    @Test
    fun `skips non-target package`() = runTest {
        val data = NotificationData(
            1000L, "com.some.other.app", null, null, null, null, null, null
        )

        handler.handle(data)

        coVerify(exactly = 0) { persistUseCase(any()) }
        verify(exactly = 0) { deduplicator.isDuplicate(any()) }
    }

    @Test
    fun `skips duplicate notification`() = runTest {
        every { deduplicator.isDuplicate(any()) } returns true
        val data = NotificationData(
            1000L, "com.tencent.mm", null, "微信支付", "25元 星巴克", null, null, null
        )

        handler.handle(data)

        coVerify(exactly = 0) { persistUseCase(any()) }
        verify(exactly = 1) { deduplicator.isDuplicate(data) }
    }
}
