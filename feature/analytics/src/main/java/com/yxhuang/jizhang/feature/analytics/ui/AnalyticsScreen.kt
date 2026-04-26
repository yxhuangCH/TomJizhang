package com.yxhuang.jizhang.feature.analytics.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yxhuang.jizhang.feature.analytics.ui.components.CategoryPieChart
import com.yxhuang.jizhang.feature.analytics.ui.components.MonthlySummaryCard
import com.yxhuang.jizhang.feature.analytics.ui.components.RecurringTransactionList
import com.yxhuang.jizhang.feature.analytics.ui.components.TopMerchantList
import com.yxhuang.jizhang.feature.analytics.ui.components.TrendLineChart
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("月度概览", "趋势分析", "周期性交易")

    LaunchedEffect(uiState.yearMonth) {
        if (uiState.monthlySummaries.isEmpty() && !uiState.isLoading) {
            viewModel.loadData(uiState.yearMonth)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计") },
                actions = {
                    MonthSelector(
                        yearMonth = uiState.yearMonth,
                        onPrevious = {
                            val prev = YearMonth.parse(uiState.yearMonth).minusMonths(1)
                            viewModel.selectYearMonth(prev.toString())
                        },
                        onNext = {
                            val next = YearMonth.parse(uiState.yearMonth).plusMonths(1)
                            viewModel.selectYearMonth(next.toString())
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.switchTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            when (uiState.selectedTab) {
                0 -> OverviewTab(uiState)
                1 -> TrendTab(uiState)
                2 -> RecurringTab(uiState)
            }
        }
    }
}

@Composable
private fun OverviewTab(uiState: AnalyticsUiState) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.monthlySummaries.isNotEmpty()) {
            MonthlySummaryCard(
                summary = uiState.monthlySummaries.first(),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        CategoryPieChart(
            data = uiState.categoryBreakdown,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TopMerchantList(merchants = uiState.topMerchants)
    }
}

@Composable
private fun TrendTab(uiState: AnalyticsUiState) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TrendLineChart(data = uiState.dailyTrend)
    }
}

@Composable
private fun RecurringTab(uiState: AnalyticsUiState) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    RecurringTransactionList(
        patterns = uiState.recurringPatterns,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun MonthSelector(
    yearMonth: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月")
        }
        Text(
            text = yearMonth,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月")
        }
    }
}
