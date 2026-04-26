package com.yxhuang.jizhang.feature.ledger.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetUiState(
    val isLoading: Boolean = true,
    val statuses: List<BudgetStatus> = emptyList(),
    val yearMonth: String = YearMonth.now().toString(),
    val showAddDialog: Boolean = false,
    val error: String? = null
)

class BudgetViewModel(
    private val budgetUseCase: BudgetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState

    fun loadBudgetStatuses(yearMonth: String) {
        _uiState.update { it.copy(isLoading = true, yearMonth = yearMonth) }
        viewModelScope.launch {
            try {
                val statuses = budgetUseCase.getBudgetStatuses(yearMonth)
                _uiState.update { it.copy(isLoading = false, statuses = statuses, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun setBudget(category: String, limit: Double, threshold: Float = 0.8f) {
        viewModelScope.launch {
            try {
                budgetUseCase.setBudget(category, limit, threshold)
                dismissDialog()
                loadBudgetStatuses(_uiState.value.yearMonth)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            try {
                budgetUseCase.deleteBudget(category)
                loadBudgetStatuses(_uiState.value.yearMonth)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
