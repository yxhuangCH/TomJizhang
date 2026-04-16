package com.yxhuang.jizhang.poc

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yxhuang.jizhang.poc.ui.theme.JizhangPocTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JizhangPocTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterVertically
                        )
                    ) {
                        Text(
                            text = "自动记账 PoC",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "请授予以下权限以开始测试",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Button(onClick = { openNotificationSettings() }) {
                            Text("开启通知使用权")
                        }

                        Button(onClick = { openAccessibilitySettings() }) {
                            Text("开启无障碍服务 (Fallback)")
                        }

                        Text(
                            text = "日志保存路径：\n" +
                                    "/sdcard/Android/data/com.yxhuang.jizhang/files/",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
