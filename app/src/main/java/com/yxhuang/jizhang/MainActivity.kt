package com.yxhuang.jizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yxhuang.jizhang.feature.analytics.ui.AnalyticsScreen
import com.yxhuang.jizhang.feature.capture.keepalive.KeepAliveService
import com.yxhuang.jizhang.feature.capture.keepalive.ServiceAliveChecker
import com.yxhuang.jizhang.feature.ledger.ui.LedgerViewModel
import com.yxhuang.jizhang.feature.ledger.ui.components.ServiceStatusBanner
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailViewModel
import com.yxhuang.jizhang.feature.ledger.ui.list.TransactionListScreen
import com.yxhuang.jizhang.feature.ledger.ui.detail.TransactionDetailScreen
import com.yxhuang.jizhang.feature.ledger.ui.onboarding.OnboardingScreen
import com.yxhuang.jizhang.feature.ledger.ui.onboarding.OnboardingViewModel
import com.yxhuang.jizhang.feature.ledger.ui.settings.PrivacySettingsScreen
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
                    var selectedNavIndex by remember { mutableStateOf(0) }
                    val navItems: List<Pair<String, ImageVector>> = listOf(
                        Pair("账本", Icons.Filled.Home),
                        Pair("统计", Icons.Filled.Star),
                        Pair("设置", Icons.Filled.Settings)
                    )

                    Scaffold(
                        bottomBar = {
                            if (selectedTransactionId == null) {
                                NavigationBar {
                                    navItems.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            icon = { Icon(item.second, contentDescription = item.first) },
                                            label = { Text(item.first) },
                                            selected = selectedNavIndex == index,
                                            onClick = { selectedNavIndex = index }
                                        )
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        Column(modifier = Modifier.padding(padding)) {
                            ServiceStatusBanner(missingPermissions = missingPermissions)

                            when (val id = selectedTransactionId) {
                                null -> {
                                    when (selectedNavIndex) {
                                        0 -> {
                                            val ledgerViewModel = koinViewModel<LedgerViewModel>()
                                            val uiState by ledgerViewModel.uiState.collectAsState()
                                            TransactionListScreen(
                                                uiState = uiState,
                                                onTransactionClick = { transactionId ->
                                                    selectedTransactionId = transactionId
                                                },
                                                onSearchKeywordChange = { keyword ->
                                                    ledgerViewModel.setSearchKeyword(keyword)
                                                }
                                            )
                                        }
                                        1 -> AnalyticsScreen()
                                        2 -> PrivacySettingsScreen()
                                    }
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
    }

    override fun onResume() {
        super.onResume()
        if (!KeepAliveService.isRunning(this)) {
            KeepAliveService.start(this)
        }
        missingPermissionsFlow.value = serviceAliveChecker.getMissingPermissions()
    }
}
