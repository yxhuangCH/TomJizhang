package com.yxhuang.jizhang.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val timestamp: Long,
    val packageName: String,
    val tickerText: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val summaryText: String?
)
