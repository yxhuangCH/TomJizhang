package com.yxhuang.jizhang.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yxhuang.jizhang.core.database.JizhangDatabase
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.feature.capture.dedup.NotificationDeduplicator
import com.yxhuang.jizhang.feature.parser.TransactionParser
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KoinModuleCheckTest {

    @Test
    fun `check all modules can be created`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val koin = koinApplication {
            androidContext(context)
            modules(appModules)
        }.koin

        assertNotNull(koin.getOrNull<JizhangDatabase>())
        assertNotNull(koin.getOrNull<TransactionRepository>())
        assertNotNull(koin.getOrNull<TransactionParser>())
        assertNotNull(koin.getOrNull<NotificationDeduplicator>())

        stopKoin()
    }
}
