package com.yxhuang.jizhang.feature.capture.usecase

import com.yxhuang.jizhang.core.model.NotificationData
import com.yxhuang.jizhang.core.model.ParseFailureLog
import com.yxhuang.jizhang.core.model.Transaction
import com.yxhuang.jizhang.core.database.repository.ParseFailureRepository
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.feature.classification.ClassificationEngine
import com.yxhuang.jizhang.feature.classification.ClassificationResult
import com.yxhuang.jizhang.feature.classification.learn.LlmLearningUseCase
import com.yxhuang.jizhang.feature.parser.TransactionParser

class PersistCapturedTransactionUseCase(
    private val parser: TransactionParser = TransactionParser,
    private val transactionRepository: TransactionRepository,
    private val parseFailureRepository: ParseFailureRepository,
    private val classificationEngine: ClassificationEngine,
    private val llmLearningUseCase: LlmLearningUseCase
) {
    suspend operator fun invoke(notification: NotificationData) {
        val parsed = parser.parse(notification)
        val amount = parsed.amount
        val merchant = parsed.merchant
        if (amount != null && merchant != null && parsed.isPayment) {
            val classification = classificationEngine.classify(merchant)
            val category = (classification as? ClassificationResult.Classified)?.category

            transactionRepository.insert(
                Transaction(
                    amount = amount.toDouble(),
                    merchant = merchant,
                    category = category,
                    timestamp = notification.timestamp,
                    sourceApp = notification.packageName,
                    rawText = notification.text ?: notification.bigText ?: ""
                )
            )

            if (classification == ClassificationResult.Unclassified) {
                llmLearningUseCase.learnForMerchant(merchant)
            }
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
