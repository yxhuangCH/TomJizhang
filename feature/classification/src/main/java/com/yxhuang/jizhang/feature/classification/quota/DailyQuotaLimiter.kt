package com.yxhuang.jizhang.feature.classification.quota

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class DailyQuotaLimiter(
    private val dataStore: DataStore<Preferences>,
    private val maxCallsPerDay: Int = 10
) {
    private val dateKey = stringPreferencesKey("llm_last_call_date")
    private val countKey = intPreferencesKey("llm_call_count")

    suspend fun canCall(): Boolean {
        val prefs = dataStore.data.first()
        val lastDate = prefs[dateKey] ?: ""
        val callCount = prefs[countKey] ?: 0
        val today = LocalDate.now().toString()

        return if (lastDate != today) true else callCount < maxCallsPerDay
    }

    suspend fun recordCall() {
        val today = LocalDate.now().toString()
        dataStore.edit { prefs ->
            val lastDate = prefs[dateKey] ?: ""
            if (lastDate != today) {
                prefs[dateKey] = today
                prefs[countKey] = 1
            } else {
                prefs[countKey] = (prefs[countKey] ?: 0) + 1
            }
        }
    }

    suspend fun remainingCalls(): Int {
        val prefs = dataStore.data.first()
        val lastDate = prefs[dateKey] ?: ""
        val callCount = prefs[countKey] ?: 0
        val today = LocalDate.now().toString()

        return if (lastDate != today) maxCallsPerDay else (maxCallsPerDay - callCount).coerceAtLeast(0)
    }
}
