package com.yxhuang.jizhang.feature.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yxhuang.jizhang.feature.analytics.engine.AnalyticsEngine
import com.yxhuang.jizhang.feature.analytics.engine.RecurringDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

class AnalyticsViewModel(
    private val engine: AnalyticsEngine,
    private val recurringDetector: RecurringDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(
        yearMonth = YearMonth.now().toString(),
        isLoading = true
    ))
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    fun loadData(yearMonth: String) {
        _uiState.update { it.copy(isLoading = true, yearMonth = yearMonth) }
        viewModelScope.launch {
            val summaries = engine.getMonthlySummaries(yearMonth, yearMonth)
            val breakdown = engine.getCategoryBreakdown(yearMonth)
            val trend = engine.getDailyTrend(yearMonth)
            val merchants = engine.getTopMerchants(yearMonth)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    monthlySummaries = summaries,
                    categoryBreakdown = breakdown,
                    dailyTrend = trend,
                    topMerchants = merchants
                )
            }
        }
    }

    fun selectYearMonth(yearMonth: String) {
        loadData(yearMonth)
    }

    fun switchTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        if (index == 2) {
            loadRecurringPatterns()
        }
    }

    private fun loadRecurringPatterns() {
        viewModelScope.launch {
            val patterns = recurringDetector.detect()
            _uiState.update { it.copy(recurringPatterns = patterns) }
        }
    }
}
