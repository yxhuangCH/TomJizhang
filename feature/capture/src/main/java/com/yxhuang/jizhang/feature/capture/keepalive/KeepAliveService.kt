package com.yxhuang.jizhang.feature.capture.keepalive

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

class KeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        val notification = KeepAliveNotificationHelper.create(this)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(ActivityManager::class.java)
            return manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == KeepAliveService::class.java.name }
        }
    }
}
