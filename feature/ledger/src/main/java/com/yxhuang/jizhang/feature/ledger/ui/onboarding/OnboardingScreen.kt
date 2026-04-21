package com.yxhuang.jizhang.feature.ledger.ui.onboarding

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    currentPage: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    onOpenNotificationSettings: (Context) -> Unit,
    onOpenBatterySettings: (Context) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (currentPage) {
            0 -> WelcomePage(onNext)
            1 -> NotificationPermissionPage(context, onNext, onOpenNotificationSettings)
            2 -> BatteryOptimizationPage(context, onComplete, onOpenBatterySettings)
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Text(
        text = "自动记账，无需手动",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "捕获微信/支付宝通知，自动分类入账",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(48.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("开始使用")
    }
}

@Composable
private fun NotificationPermissionPage(
    context: Context,
    onNext: () -> Unit,
    onOpenNotificationSettings: (Context) -> Unit
) {
    Text(
        text = "开启通知使用权",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "我们需要监听支付通知来自动记账。您的通知内容仅保存在本地，不会上传。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(48.dp))
    Button(
        onClick = { onOpenNotificationSettings(context) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("去开启")
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("下一步")
    }
}

@Composable
private fun BatteryOptimizationPage(
    context: Context,
    onComplete: () -> Unit,
    onOpenBatterySettings: (Context) -> Unit
) {
    Text(
        text = "保持后台运行",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "为防止系统杀死记账服务，请将本应用加入电池优化白名单。",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(48.dp))
    Button(
        onClick = { onOpenBatterySettings(context) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("去设置")
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
        Text("完成")
    }
}
