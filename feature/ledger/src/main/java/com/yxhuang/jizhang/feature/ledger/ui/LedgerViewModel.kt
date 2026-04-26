package com.yxhuang.jizhang.feature.ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yxhuang.jizhang.feature.ledger.ui.search.SearchQuery
import com.yxhuang.jizhang.feature.ledger.ui.search.TransactionSearchEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
    private val searchEngine: TransactionSearchEngine,
    private val reducer: LedgerReducer
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(SearchQuery())

    val uiState: StateFlow<LedgerUiState> = _searchQuery
        .flatMapLatest { query ->
            searchEngine.search(query)
        }
        .map { transactions ->
            val reduced = reducer.reduce(LedgerUiState(isLoading = false), transactions)
            val query = _searchQuery.value
            reduced.copy(
                searchKeyword = query.keyword,
                isSearchActive = query.keyword.isNotEmpty()
            )
        }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            LedgerUiState(isLoading = true)
        )

    fun setSearchKeyword(keyword: String) {
        _searchQuery.value = SearchQuery(keyword = keyword)
    }
}
