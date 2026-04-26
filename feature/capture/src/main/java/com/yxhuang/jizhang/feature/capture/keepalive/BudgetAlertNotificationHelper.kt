package com.yxhuang.jizhang.feature.capture.keepalive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object BudgetAlertNotificationHelper : BudgetNotifier {
    private const val CHANNEL_ID = "budget_alerts"

    override fun showOverBudgetAlert(context: Context, category: String, limit: Double, spent: Double) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("预算超支提醒")
            .setContentText("$category 预算已超支！限额 %.0f元，已用 %.0f元".format(limit, spent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify("$category-over".hashCode(), notification)
    }

    override fun showThresholdAlert(context: Context, category: String, limit: Double, spent: Double, percentage: Float) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("预算预警")
            .setContentText("$category 已用 %.0f%%（%.0f/%.0f元）".format(percentage * 100, spent, limit))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify("$category-alert".hashCode(), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "预算提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        }
    }
}
