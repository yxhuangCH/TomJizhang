package com.yxhuang.jizhang.feature.analytics.engine

import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import kotlin.math.abs
import kotlin.math.sqrt

class RecurringDetector(
    private val transactionRepository: TransactionRepository
) {
    companion object {
        private const val MIN_OCCURRENCES = 3
        private const val MAX_INTERVAL_CV = 0.15
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WEEK_MS = 7L * DAY_MS
        private const val MONTH_MS = 30L * DAY_MS
    }

    suspend fun detect(): List<RecurringPattern> {
        val allTransactions = transactionRepository.getAll()
        if (allTransactions.isEmpty()) return emptyList()

        return allTransactions
            .groupBy { it.merchant }
            .values
            .mapNotNull { detectPattern(it) }
    }

    private fun detectPattern(transactions: List<Transaction>): RecurringPattern? {
        if (transactions.size < MIN_OCCURRENCES) return null

        val sorted = transactions.sortedBy { it.timestamp }
        val intervals = sorted.zipWithNext { a, b -> b.timestamp - a.timestamp }
        if (intervals.isEmpty()) return null

        val avgInterval = intervals.average()
        val intervalCv = coefficientOfVariation(intervals.map { it.toDouble() })
        if (intervalCv > MAX_INTERVAL_CV) return null

        val amounts = sorted.map { it.amount }
        val avgAmount = amounts.average()
        val amountCv = coefficientOfVariation(amounts)

        val frequency = classifyFrequency(avgInterval)

        val intervalScore = 1.0 - (intervalCv / MAX_INTERVAL_CV).coerceAtMost(1.0)
        val amountScore = 1.0 - (amountCv / MAX_INTERVAL_CV).coerceAtMost(1.0)
        val countScore = (sorted.size.coerceAtMost(6) / 6.0).coerceAtMost(1.0)

        val confidence = (intervalScore * 0.4 + amountScore * 0.4 + countScore * 0.2).toFloat()

        val lastOccurrence = sorted.last().timestamp
        val nextEstimated = lastOccurrence + avgInterval.toLong()

        val latest = sorted.last()
        return RecurringPattern(
            merchant = latest.merchant,
            category = latest.category,
            averageAmount = avgAmount,
            type = latest.type,
            frequency = frequency,
            lastOccurrence = lastOccurrence,
            nextEstimatedOccurrence = nextEstimated,
            confidence = confidence.coerceIn(0f, 1f)
        )
    }

    private fun classifyFrequency(avgIntervalMs: Double): RecurringFrequency {
        val days = avgIntervalMs / DAY_MS
        return when {
            abs(days - 7) < 2 -> RecurringFrequency.WEEKLY
            abs(days - 30) < 5 -> RecurringFrequency.MONTHLY
            abs(days - 1) < 0.5 -> RecurringFrequency.DAILY
            days < 5 -> RecurringFrequency.DAILY
            days < 12 -> RecurringFrequency.WEEKLY
            else -> RecurringFrequency.MONTHLY
        }
    }

    private fun coefficientOfVariation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        if (mean == 0.0) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        return stdDev / abs(mean)
    }
}
