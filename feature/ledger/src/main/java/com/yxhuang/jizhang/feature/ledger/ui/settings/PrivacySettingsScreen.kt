package com.yxhuang.jizhang.feature.ledger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PrivacySettingsScreen(
    dataExportUseCase: DataExportUseCase = koinInject(),
    clearDataUseCase: ClearDataUseCase = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "隐私与数据管理",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "数据存储",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "所有交易数据、分类规则、解析失败日志仅存储在您的设备本地（Android 内部存储 / Room 数据库）。我们不会将任何交易数据上传到远程服务器。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI 分类与网络传输",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "当您遇到新的、未分类的商户时，App 可能会调用第三方 LLM API（如 DeepSeek）来学习分类规则。上传的数据仅限商户名称，不包含交易金额、时间、身份信息或设备标识符。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    val file = dataExportUseCase.exportToCsv()
                    exportMessage = "已导出到: ${file.absolutePath}"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("导出数据（CSV）")
        }
        exportMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showClearConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("清除所有数据", color = Color.White)
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清除") },
            text = { Text("此操作将删除所有本地交易记录和分类规则，不可恢复。是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        clearDataUseCase.clearAll()
                        showClearConfirm = false
                    }
                }) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
