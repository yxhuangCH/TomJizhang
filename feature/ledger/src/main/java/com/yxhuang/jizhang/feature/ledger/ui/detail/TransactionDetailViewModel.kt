package com.yxhuang.jizhang.feature.ledger.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionDetailViewModel(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState(isLoading = true))
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            val transaction = transactionRepository.getById(transactionId)
            if (transaction != null) {
                _uiState.update {
                    it.copy(
                        id = transaction.id,
                        amountText = String.format(java.util.Locale.US, "%.2f", transaction.amount),
                        merchant = transaction.merchant,
                        category = transaction.category ?: "其他",
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun save(merchant: String, category: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val current = transactionRepository.getById(transactionId)
            if (current != null) {
                transactionRepository.update(
                    current.copy(merchant = merchant, category = category)
                )
                _uiState.update {
                    it.copy(
                        merchant = merchant,
                        category = category,
                        isSaving = false,
                        saveCompleted = true
                    )
                }
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
