package com.yxhuang.jizhang.feature.analytics.engine

import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.model.TransactionType
import java.time.YearMonth
import java.time.ZoneId

class AnalyticsEngine(
    private val transactionRepository: TransactionRepository
) {

    suspend fun getMonthlySummaries(
        startYearMonth: String,
        endYearMonth: String
    ): List<MonthlySummary> {
        val start = YearMonth.parse(startYearMonth)
        val end = YearMonth.parse(endYearMonth)
        val months = generateSequence(start) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(end) }
            .toList()

        return months.map { ym ->
            val rangeStart = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val rangeEnd = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val totalExpense = transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.EXPENSE, rangeStart, rangeEnd
            )
            val totalIncome = transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.INCOME, rangeStart, rangeEnd
            )

            MonthlySummary(
                yearMonth = ym.toString(),
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = totalIncome - totalExpense,
                transactionCount = 0 // Not queried separately; add if needed
            )
        }
    }

    suspend fun getCategoryBreakdown(yearMonth: String): List<CategoryBreakdown> {
        val ym = YearMonth.parse(yearMonth)
        val startTime = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val summary = transactionRepository.getCategorySummary(startTime, endTime)
        val total = summary.sumOf { it.total }

        return summary.map {
            CategoryBreakdown(
                category = it.category ?: "未分类",
                amount = it.total,
                percentage = if (total > 0) (it.total / total).toFloat() else 0f,
                transactionCount = it.count
            )
        }
    }

    suspend fun getDailyTrend(yearMonth: String): List<TrendPoint> {
        val ym = YearMonth.parse(yearMonth)
        val startTime = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val transactions = transactionRepository.getAll().filter {
            it.timestamp in startTime..endTime && it.type == TransactionType.EXPENSE
        }

        return transactions
            .groupBy { dayLabel(it.timestamp) }
            .map { (label, txs) ->
                TrendPoint(label = label, amount = txs.sumOf { it.amount })
            }
            .sortedBy { it.label }
    }

    suspend fun getTopMerchants(yearMonth: String, limit: Int = 10): List<MerchantRanking> {
        val ym = YearMonth.parse(yearMonth)
        val startTime = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val transactions = transactionRepository.getAll().filter {
            it.timestamp in startTime..endTime && it.type == TransactionType.EXPENSE
        }

        return transactions
            .groupBy { it.merchant }
            .map { (merchant, txs) ->
                MerchantRanking(
                    merchant = merchant,
                    totalAmount = txs.sumOf { it.amount },
                    transactionCount = txs.size,
                    category = txs.firstOrNull()?.category
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(limit)
    }

    private fun dayLabel(timestamp: Long): String {
        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val date = java.time.LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return String.format("%02d-%02d", date.monthValue, date.dayOfMonth)
    }
}
