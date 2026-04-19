package com.yxhuang.jizhang.feature.capture.di

import com.yxhuang.jizhang.feature.capture.dedup.NotificationDeduplicator
import com.yxhuang.jizhang.feature.capture.notification.CaptureNotificationHandler
import com.yxhuang.jizhang.feature.capture.usecase.PersistCapturedTransactionUseCase
import org.koin.dsl.module

val captureModule = module {
    single { NotificationDeduplicator() }
    single { PersistCapturedTransactionUseCase(get(), get(), get()) }
    single { CaptureNotificationHandler(get(), get()) }
}
