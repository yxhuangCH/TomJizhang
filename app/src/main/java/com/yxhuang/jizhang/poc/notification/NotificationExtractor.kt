package com.yxhuang.jizhang.poc.notification

import android.app.Notification
import android.service.notification.StatusBarNotification

object NotificationExtractor {

    val TARGET_PACKAGES = setOf(
        "com.tencent.mm",
        "com.eg.android.AlipayGphone"
    )

    fun shouldCapture(packageName: String): Boolean =
        packageName in TARGET_PACKAGES

    fun extract(
        sbn: StatusBarNotification,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): NotificationData {
        val extras = sbn.notification.extras
        return extract(
            packageName = sbn.packageName,
            tickerText = sbn.notification.tickerText?.toString(),
            title = extras.getString(Notification.EXTRA_TITLE),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString(),
            currentTimeMillis = currentTimeMillis
        )
    }

    fun extract(
        packageName: String,
        tickerText: String? = null,
        title: String? = null,
        text: String? = null,
        subText: String? = null,
        bigText: String? = null,
        summaryText: String? = null,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): NotificationData {
        return NotificationData(
            timestamp = currentTimeMillis,
            packageName = packageName,
            tickerText = tickerText,
            title = title,
            text = text,
            subText = subText,
            bigText = bigText,
            summaryText = summaryText
        )
    }
}
