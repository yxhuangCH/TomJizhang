package com.yxhuang.jizhang.feature.capture.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CaptureNotificationService : NotificationListenerService(), KoinComponent {

    private val handler: CaptureNotificationHandler by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "CaptureNotificationService connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val data = NotificationExtractor.extract(sbn)
        serviceScope.launch {
            try {
                handler.handle(data)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification from ${data.packageName}", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 支付通知不需要处理移除事件
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext.cancel()
    }

    companion object {
        private const val TAG = "CaptureNotificationSvc"
    }
}
