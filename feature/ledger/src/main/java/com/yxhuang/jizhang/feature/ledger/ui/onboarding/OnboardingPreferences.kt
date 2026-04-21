package com.yxhuang.jizhang.feature.ledger.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingPreferences(private val dataStore: DataStore<Preferences>) {
    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .map { it[booleanPreferencesKey("onboarding_completed")] ?: false }

    suspend fun setCompleted() {
        dataStore.edit { it[booleanPreferencesKey("onboarding_completed")] = true }
    }
}
