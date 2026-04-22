package com.yxhuang.jizhang.feature.capture.keepalive

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings

class ServiceAliveChecker(private val context: Context) {

    fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName) == true
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isKeepAliveServiceRunning(): Boolean {
        return KeepAliveService.isRunning(context)
    }

    fun getMissingPermissions(): List<MissingPermission> {
        return buildList {
            if (!isNotificationServiceEnabled()) add(MissingPermission.NOTIFICATION_LISTENER)
            if (!isBatteryOptimizationIgnored()) add(MissingPermission.BATTERY_OPTIMIZATION)
            if (!isKeepAliveServiceRunning()) add(MissingPermission.FOREGROUND_SERVICE)
        }
    }

    enum class MissingPermission {
        NOTIFICATION_LISTENER,
        BATTERY_OPTIMIZATION,
        FOREGROUND_SERVICE
    }
}
