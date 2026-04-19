package com.yxhuang.jizhang.feature.ledger.di

import com.yxhuang.jizhang.feature.ledger.ui.LedgerReducer
import com.yxhuang.jizhang.feature.ledger.ui.LedgerViewModel
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val ledgerModule = module {
    single { LedgerReducer() }
    viewModel { LedgerViewModel(get(), get()) }
    viewModel { (transactionId: Long) ->
        TransactionDetailViewModel(transactionId, get())
    }
}
