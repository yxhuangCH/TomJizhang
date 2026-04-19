package com.yxhuang.jizhang.feature.capture.dedup

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.feature.parser.TransactionParser

class NotificationDeduplicator(
    private val windowMillis: Long = 3000L,
    private val maxSize: Int = 20
) {
    private val recent = ArrayDeque<NotificationData>(maxSize)

    fun isDuplicate(newData: NotificationData): Boolean {
        val cutoff = newData.timestamp - windowMillis

        // Remove expired entries
        while (recent.isNotEmpty() && recent.first().timestamp < cutoff) {
            recent.removeFirst()
        }

        val isDup = recent.any { it.isSameTransaction(newData) }
        if (!isDup) {
            if (recent.size >= maxSize) {
                recent.removeFirst()
            }
            recent.addLast(newData)
        }
        return isDup
    }

    private fun NotificationData.isSameTransaction(other: NotificationData): Boolean {
        if (packageName != other.packageName) return false
        val textMatch = text != null && other.text != null && text == other.text
        val bigTextMatch = bigText != null && other.bigText != null && bigText == other.bigText
        return textMatch || bigTextMatch
    }
}
