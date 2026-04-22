package com.yxhuang.jizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yxhuang.jizhang.feature.capture.keepalive.KeepAliveService
import com.yxhuang.jizhang.feature.capture.keepalive.ServiceAliveChecker
import com.yxhuang.jizhang.feature.ledger.ui.LedgerViewModel
import com.yxhuang.jizhang.feature.ledger.ui.components.ServiceStatusBanner
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailViewModel
import com.yxhuang.jizhang.feature.ledger.ui.list.TransactionListScreen
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailScreen
import com.yxhuang.jizhang.feature.ledger.ui.onboarding.OnboardingScreen
import com.yxhuang.jizhang.feature.ledger.ui.onboarding.OnboardingViewModel
import com.yxhuang.jizhang.poc.ui.theme.JizhangPocTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

    private lateinit var serviceAliveChecker: ServiceAliveChecker
    private val missingPermissionsFlow = MutableStateFlow(emptyList<ServiceAliveChecker.MissingPermission>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceAliveChecker = ServiceAliveChecker(this)
        if (!KeepAliveService.isRunning(this)) {
            KeepAliveService.start(this)
        }
        setContent {
            JizhangPocTheme {
                val onboardingViewModel = koinViewModel<OnboardingViewModel>()
                val onboardingState by onboardingViewModel.uiState.collectAsState()
                val missingPermissions by missingPermissionsFlow.collectAsState()

                if (onboardingState.showOnboarding) {
                    OnboardingScreen(
                        currentPage = onboardingState.currentPage,
                        onNext = { onboardingViewModel.nextPage() },
                        onComplete = { onboardingViewModel.complete() },
                        onOpenNotificationSettings = { context ->
                            onboardingViewModel.openNotificationSettings(context)
                        },
                        onOpenBatterySettings = { context ->
                            onboardingViewModel.openBatterySettings(context)
                        }
                    )
                } else {
                    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }

                    Column {
                        ServiceStatusBanner(missingPermissions = missingPermissions)

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
    }

    override fun onResume() {
        super.onResume()
        if (!KeepAliveService.isRunning(this)) {
            KeepAliveService.start(this)
        }
        missingPermissionsFlow.value = serviceAliveChecker.getMissingPermissions()
    }
}
