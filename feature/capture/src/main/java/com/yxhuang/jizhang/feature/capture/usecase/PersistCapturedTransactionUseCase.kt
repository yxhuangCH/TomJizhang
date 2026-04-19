package com.yxhuang.jizhang.feature.capture.usecase

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.core.model.ParseFailureLog
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.feature.parser.TransactionParser

class PersistCapturedTransactionUseCase(
    private val parser: TransactionParser = TransactionParser,
    private val transactionRepository: TransactionRepository,
    private val parseFailureRepository: ParseFailureRepository
) {
    suspend operator fun invoke(notification: NotificationData) {
        val parsed = parser.parse(notification)
        val amount = parsed.amount
        val merchant = parsed.merchant
        if (amount != null && merchant != null && parsed.isPayment) {
            transactionRepository.insert(
                Transaction(
                    amount = amount.toDouble(),
                    merchant = merchant,
                    category = null,
                    timestamp = notification.timestamp,
                    sourceApp = notification.packageName,
                    rawText = notification.text ?: notification.bigText ?: ""
                )
            )
        } else {
            parseFailureRepository.insert(
                ParseFailureLog(
                    rawText = notification.text ?: "",
                    sourceApp = notification.packageName,
                    timestamp = notification.timestamp,
                    reason = if (!parsed.isPayment) "Not a payment" else "Missing amount or merchant"
                )
            )
        }
    }
}
