package com.yxhuang.jizhang.poc.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogWriterTest {

    @Test
    fun `write creates file in subdirectory`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        LogWriter.write(
            context = context,
            subDir = "test_logs",
            fileName = "test_file.json",
            content = "{\"hello\":\"world\"}"
        )

        val file = File(context.getExternalFilesDir(null), "test_logs/test_file.json")
        assertTrue(file.exists())
        assertEquals("{\"hello\":\"world\"}", file.readText())
    }

    @Test
    fun `write overwrites existing file`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val subDir = "test_logs"
        val fileName = "overwrite.json"

        LogWriter.write(context, subDir, fileName, "first")
        LogWriter.write(context, subDir, fileName, "second")

        val file = File(context.getExternalFilesDir(null), "$subDir/$fileName")
        assertEquals("second", file.readText())
    }

    @Test
    fun `write creates nested directories`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        LogWriter.write(
            context = context,
            subDir = "nested/deep/dir",
            fileName = "deep.json",
            content = "deep content"
        )

        val file = File(context.getExternalFilesDir(null), "nested/deep/dir/deep.json")
        assertTrue(file.exists())
        assertEquals("deep content", file.readText())
    }
}
