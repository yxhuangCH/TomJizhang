package com.yxhuang.jizhang.feature.capture.notification

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.feature.capture.dedup.NotificationDeduplicator
import com.yxhuang.jizhang.feature.capture.usecase.PersistCapturedTransactionUseCase

class CaptureNotificationHandler(
    private val deduplicator: NotificationDeduplicator,
    private val persistUseCase: PersistCapturedTransactionUseCase
) {
    suspend fun handle(notificationData: NotificationData): Boolean {
        if (!NotificationExtractor.shouldCapture(notificationData.packageName)) {
            return false
        }
        if (deduplicator.isDuplicate(notificationData)) {
            return false
        }
        persistUseCase(notificationData)
        return true
    }
}
