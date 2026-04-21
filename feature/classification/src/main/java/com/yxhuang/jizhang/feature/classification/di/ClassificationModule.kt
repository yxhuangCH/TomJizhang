package com.yxhuang.jizhang.feature.classification.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.yxhuang.jizhang.feature.classification.ClassificationEngine
import com.yxhuang.jizhang.feature.classification.learn.LlmLearningUseCase
import com.yxhuang.jizhang.feature.classification.quota.DailyQuotaLimiter
import com.yxhuang.jizhang.feature.classification.seed.SeedRuleLoader
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val classificationModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create {
            File(androidContext().filesDir, "datastore/jizhang_prefs.preferences_pb")
        }
    }
    single { ClassificationEngine(get()) }
    single { SeedRuleLoader(get(), get()) }
    single { DailyQuotaLimiter(get()) }
    single { LlmLearningUseCase(get(), get(), get(), get()) }
}