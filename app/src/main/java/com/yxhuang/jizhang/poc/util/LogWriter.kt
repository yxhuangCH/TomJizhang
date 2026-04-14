package com.yxhuang.jizhang.poc.util

import android.content.Context
import java.io.File

object LogWriter {
    fun write(context: Context, subDir: String, fileName: String, content: String) {
        val dir = File(context.getExternalFilesDir(null), subDir)
        dir.mkdirs()
        File(dir, fileName).writeText(content)
    }
}
