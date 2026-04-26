package com.yxhuang.jizhang.feature.analytics.di

import com.yxhuang.jizhang.feature.analytics.engine.AnalyticsEngine
import com.yxhuang.jizhang.feature.analytics.engine.RecurringDetector
import com.yxhuang.jizhang.feature.analytics.ui.AnalyticsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val analyticsModule = module {
    single { AnalyticsEngine(get()) }
    single { RecurringDetector(get()) }
    viewModel { AnalyticsViewModel(get(), get()) }
}
