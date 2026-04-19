package com.yxhuang.jizhang.feature.ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LedgerViewModel(
    repository: TransactionRepository,
    private val reducer: LedgerReducer
) : ViewModel() {

    val uiState: StateFlow<LedgerUiState> = repository.observeAll()
        .map { reducer.reduce(LedgerUiState(isLoading = true), it) }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LedgerUiState(isLoading = true)
        )
}
