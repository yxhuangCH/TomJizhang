package com.yxhuang.jizhang.feature.capture.keepalive

import android.content.Context

interface BudgetNotifier {
    fun showOverBudgetAlert(context: Context, category: String, limit: Double, spent: Double)
    fun showThresholdAlert(context: Context, category: String, limit: Double, spent: Double, percentage: Float)
}
