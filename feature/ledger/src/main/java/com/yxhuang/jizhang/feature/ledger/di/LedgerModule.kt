package com.yxhuang.jizhang.feature.ledger.di

import com.yxhuang.jizhang.feature.ledger.ui.LedgerReducer
import com.yxhuang.jizhang.feature.ledger.ui.LedgerViewModel
import com.yxhuang.jizhang.feature.ledger.ui.budget.BudgetUseCase
import com.yxhuang.jizhang.feature.ledger.ui.budget.BudgetViewModel
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailViewModel
import com.yxhuang.jizhang.feature.ledger.ui.onboarding.OnboardingPreferences
import com.yxhuang.jizhang.feature.ledger.ui.onboarding.OnboardingViewModel
import com.yxhuang.jizhang.feature.ledger.ui.search.TransactionSearchEngine
import com.yxhuang.jizhang.feature.ledger.ui.settings.ClearDataUseCase
import com.yxhuang.jizhang.feature.ledger.ui.settings.DataExportUseCase
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val ledgerModule = module {
    single { LedgerReducer() }
    single { TransactionSearchEngine(get()) }
    single { OnboardingPreferences(get()) }
    single { DataExportUseCase(get(), get()) }
    single { ClearDataUseCase(get(), get(), get()) }
    single { BudgetUseCase(get(), get()) }
    viewModel { LedgerViewModel(get(), get()) }
    viewModel { (transactionId: Long) ->
        TransactionDetailViewModel(transactionId, get(), get())
    }
    viewModel { OnboardingViewModel(get()) }
    viewModel { BudgetViewModel(get()) }
}
