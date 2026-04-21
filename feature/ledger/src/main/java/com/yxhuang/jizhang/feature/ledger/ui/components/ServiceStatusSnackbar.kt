package com.yxhuang.jizhang.feature.ledger.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yxhuang.jizhang.feature.capture.keepalive.ServiceAliveChecker

@Composable
fun ServiceStatusBanner(missingPermissions: List<ServiceAliveChecker.MissingPermission>) {
    if (missingPermissions.isEmpty()) return

    val context = LocalContext.current
    val message = when {
        missingPermissions.contains(ServiceAliveChecker.MissingPermission.NOTIFICATION_LISTENER) ->
            "通知权限已关闭，自动记账已停止"
        missingPermissions.contains(ServiceAliveChecker.MissingPermission.FOREGROUND_SERVICE) ->
            "前台服务未运行，自动记账可能不稳定"
        missingPermissions.contains(ServiceAliveChecker.MissingPermission.BATTERY_OPTIMIZATION) ->
            "建议关闭电池优化以确保稳定记账"
        else -> ""
    }

    val isError = missingPermissions.contains(ServiceAliveChecker.MissingPermission.NOTIFICATION_LISTENER)
            || missingPermissions.contains(ServiceAliveChecker.MissingPermission.FOREGROUND_SERVICE)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
        if (isError) {
            Button(
                onClick = { openRelevantSettings(context, missingPermissions) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text("去设置")
            }
        }
    }
}

private fun openRelevantSettings(
    context: Context,
    missingPermissions: List<ServiceAliveChecker.MissingPermission>
) {
    when {
        missingPermissions.contains(ServiceAliveChecker.MissingPermission.NOTIFICATION_LISTENER) -> {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        missingPermissions.contains(ServiceAliveChecker.MissingPermission.BATTERY_OPTIMIZATION) -> {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
        else -> {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            })
        }
    }
}
