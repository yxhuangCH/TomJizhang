package com.yxhuang.jizhang.feature.ledger.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val completed = onboardingPreferences.onboardingCompleted.first()
            _uiState.value = OnboardingUiState(
                showOnboarding = !completed,
                currentPage = 0
            )
        }
    }

    fun nextPage() {
        val current = _uiState.value.currentPage
        if (current < 2) {
            _uiState.value = _uiState.value.copy(currentPage = current + 1)
        }
    }

    fun complete() {
        viewModelScope.launch {
            onboardingPreferences.setCompleted()
            _uiState.value = _uiState.value.copy(showOnboarding = false)
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

data class OnboardingUiState(
    val showOnboarding: Boolean = true,
    val currentPage: Int = 0
)
