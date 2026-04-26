package com.yxhuang.jizhang.feature.ledger.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

@Composable
fun BudgetSettingScreen(
    viewModel: BudgetViewModel = koinViewModel(),
    yearMonth: String = YearMonth.now().toString()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(yearMonth) {
        if (uiState.statuses.isEmpty() || uiState.yearMonth != yearMonth) {
            viewModel.loadBudgetStatuses(yearMonth)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "预算管理",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "为每个支出分类设置月度预算上限",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.statuses.isEmpty()) {
            Text(
                text = "暂无预算设置，点击下方按钮添加",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = uiState.statuses,
                    key = { it.category }
                ) { status ->
                    BudgetStatusCard(
                        status = status,
                        onDelete = { viewModel.deleteBudget(status.category) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.showAddDialog() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("添加预算")
        }
    }

    if (uiState.showAddDialog) {
        AddBudgetDialog(
            onConfirm = { category, limit, threshold ->
                viewModel.setBudget(category, limit, threshold)
            },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    uiState.error?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun BudgetStatusCard(
    status: BudgetStatus,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.category,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "¥${String.format("%.0f", status.spent)} / ¥${String.format("%.0f", status.monthlyLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(status.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            status.isOverBudget -> MaterialTheme.colorScheme.error
                            status.isAlertTriggered -> Color(0xFFFFC107)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = "剩余 ¥${String.format("%.0f", status.remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progressColor = when {
                status.isOverBudget -> MaterialTheme.colorScheme.error
                status.isAlertTriggered -> Color(0xFFFFC107)
                else -> MaterialTheme.colorScheme.primary
            }
            LinearProgressIndicator(
                progress = { status.percentage.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddBudgetDialog(
    onConfirm: (category: String, limit: Double, threshold: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var thresholdText by remember { mutableStateOf("80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加预算") },
        text = {
            Column {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("月度预算金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("预警阈值 (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("达到该比例时发送预警通知") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    val threshold = (thresholdText.toIntOrNull() ?: 80) / 100f
                    if (category.isNotBlank() && limit != null && limit > 0) {
                        onConfirm(category, limit, threshold)
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
