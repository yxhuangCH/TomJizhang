package com.yxhuang.jizhang.feature.ledger.ui.settings

import com.yxhuang.jizhang.core.database.repository.TransactionRepository
import com.yxhuang.jizhang.core.model.Transaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import android.content.Context
import android.os.Environment

class DataExportUseCaseTest {
    private val txRepo = mockk<TransactionRepository>()
    private val context = mockk<Context>()
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "jizhang_test_${System.currentTimeMillis()}")
        .apply { mkdirs() }
    private val useCase = DataExportUseCase(txRepo, context)

    @Test
    fun `exportToCsv creates file with header and data`() = runTest {
        every { context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) } returns tempDir
        coEvery { txRepo.getAll() } returns listOf(
            Transaction(1, 25.0, "星巴克", "饮品", 1000L, "wechat", "raw"),
            Transaction(2, 18.5, "滴滴", "交通", 2000L, "alipay", "raw")
        )

        val file = useCase.exportToCsv()

        assertTrue(file.exists())
        val content = file.readText()
        assertTrue(content.contains("ID,金额,商户,分类"))
        assertTrue(content.contains("星巴克"))
        assertTrue(content.contains("滴滴"))
        assertTrue(content.startsWith("\uFEFF")) // BOM
    }
}
