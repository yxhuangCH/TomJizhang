package com.yxhuang.jizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yxhuang.jizhang.feature.ledger.ui.LedgerViewModel
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailViewModel
import com.yxhuang.jizhang.feature.ledger.ui.list.TransactionListScreen
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailScreen
import com.yxhuang.jizhang.poc.ui.theme.JizhangPocTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JizhangPocTheme {
                var selectedTransactionId by remember { mutableStateOf<Long?>(null) }

                when (val id = selectedTransactionId) {
                    null -> {
                        val viewModel = koinViewModel<LedgerViewModel>()
                        val uiState by viewModel.uiState.collectAsState()
                        TransactionListScreen(
                            uiState = uiState,
                            onTransactionClick = { transactionId ->
                                selectedTransactionId = transactionId
                            }
                        )
                    }
                    else -> {
                        val viewModel = koinViewModel<TransactionDetailViewModel> { parametersOf(id) }
                        val uiState by viewModel.uiState.collectAsState()
                        TransactionDetailScreen(
                            uiState = uiState,
                            onBackClick = { selectedTransactionId = null },
                            onSave = { merchant, category ->
                                viewModel.save(merchant, category)
                            }
                        )
                    }
                }
            }
        }
    }
}
