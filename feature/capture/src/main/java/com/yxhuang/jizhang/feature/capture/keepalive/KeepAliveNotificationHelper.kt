package com.yxhuang.jizhang.feature.capture.keepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yxhuang.jizhang.feature.capture.R

object KeepAliveNotificationHelper {
    private const val CHANNEL_ID = "keep_alive"

    fun create(context: Context, recordedCount: Int = 0): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "自动记账保活",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "确保自动记账服务在后台持续运行"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val contentText = if (recordedCount > 0) {
            "已帮您记账 $recordedCount 笔"
        } else {
            "自动记账服务正在运行"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("自动记账运行中")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }
}
