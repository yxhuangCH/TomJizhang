package com.yxhuang.jizhang.feature.classification.quota

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyQuotaLimiterTest {
    private val testContext = ApplicationProvider.getApplicationContext<Context>()
    private val dataStore = PreferenceDataStoreFactory.create {
        File(testContext.cacheDir, "test_quota.preferences_pb")
    }
    private val limiter = DailyQuotaLimiter(dataStore, maxCallsPerDay = 3)

    @Test
    fun `canCall returns true when no calls made`() = runTest {
        assertTrue(limiter.canCall())
    }

    @Test
    fun `canCall returns false after reaching limit`() = runTest {
        repeat(3) { limiter.recordCall() }
        assertFalse(limiter.canCall())
    }

    @Test
    fun `canCall resets on new day`() = runTest {
        repeat(3) { limiter.recordCall() }
        assertFalse(limiter.canCall())
        // Simulate cross-day: directly modify DataStore
        dataStore.edit { it[stringPreferencesKey("llm_last_call_date")] = "2024-01-01" }
        assertTrue(limiter.canCall())
    }

    @Test
    fun `remainingCalls decreases after record`() = runTest {
        assertEquals(3, limiter.remainingCalls())
        limiter.recordCall()
        assertEquals(2, limiter.remainingCalls())
    }
}
