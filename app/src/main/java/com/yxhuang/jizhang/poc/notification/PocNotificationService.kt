package com.yxhuang.jizhang.poc.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.yxhuang.jizhang.poc.util.LogWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PocNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "PocNotification"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!NotificationExtractor.shouldCapture(pkg)) return

        val data = NotificationExtractor.extract(sbn)
        val json = Json.encodeToString(data)
        LogWriter.write(
            context = this,
            subDir = "notifications",
            fileName = "${pkg}_${data.timestamp}.json",
            content = json
        )
        Log.i(TAG, "Captured notification from $pkg: title=${data.title}, text=${data.text}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // PoC 阶段不处理移除事件
    }
}
