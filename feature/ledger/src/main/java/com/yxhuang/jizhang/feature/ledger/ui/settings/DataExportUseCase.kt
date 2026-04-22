package com.yxhuang.jizhang.feature.ledger.ui.settings

import android.content.Context
import android.os.Environment
import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import java.io.File

class DataExportUseCase(
    private val transactionRepository: TransactionRepository,
    private val context: Context
) {
    suspend fun exportToCsv(): File {
        val transactions = transactionRepository.getAll()
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "jizhang_export_${System.currentTimeMillis()}.csv"
        )

        file.bufferedWriter().use { writer ->
            writer.write("\uFEFF") // BOM for Excel UTF-8
            writer.write("ID,金额,商户,分类,时间,来源应用,原始文本\n")
            transactions.forEach { tx ->
                writer.write(
                    "${tx.id},${tx.amount},${tx.merchant},${tx.category ?: ""},${tx.timestamp},${tx.sourceApp},\"${tx.rawText}\"\n"
                )
            }
        }
        return file
    }
}
