package com.yxhuang.jizhang.poc.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.yxhuang.jizhang.poc.util.LogWriter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class PocAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PocAccessibility"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return

        val allTexts = mutableListOf<String>()
        traverseNode(root, allTexts)

        if (!AccessibilityExtractor.shouldCapture(pkg, allTexts)) return

        val amount = AccessibilityExtractor.extractAmount(allTexts)
        val merchant = AccessibilityExtractor.extractMerchant(allTexts)

        val data = JsonObject(
            mapOf(
                "timestamp" to JsonPrimitive(System.currentTimeMillis()),
                "packageName" to JsonPrimitive(pkg),
                "allTexts" to JsonPrimitive(allTexts.take(20).joinToString(" | ")),
                "amount" to JsonPrimitive(amount),
                "merchant" to JsonPrimitive(merchant)
            )
        )

        LogWriter.write(
            context = this,
            subDir = "accessibility",
            fileName = "${pkg}_${System.currentTimeMillis()}.json",
            content = data.toString()
        )
        Log.i(TAG, "Accessibility captured from $pkg: amount=$amount, merchant=$merchant")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    private fun traverseNode(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && text !in texts) {
            texts.add(text)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, texts)
        }
    }
}
