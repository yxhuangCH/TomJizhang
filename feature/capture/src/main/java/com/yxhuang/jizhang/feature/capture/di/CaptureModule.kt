package com.yxhuang.jizhang.feature.capture.di

import com.yxhuang.jizhang.feature.capture.dedup.NotificationDeduplicator
import com.yxhuang.jizhang.feature.capture.keepalive.BudgetAlertChecker
import com.yxhuang.jizhang.feature.capture.notification.CaptureNotificationHandler
import com.yxhuang.jizhang.feature.capture.usecase.PersistCapturedTransactionUseCase
import org.koin.dsl.module

val captureModule = module {
    single { NotificationDeduplicator() }
    single { CaptureNotificationHandler(get(), get()) }
    single { BudgetAlertChecker(get(), get(), get()) }
    single { PersistCapturedTransactionUseCase(get(), get(), get(), get(), get(), get()) }
}
