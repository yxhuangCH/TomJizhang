package com.yxhuang.jizhang.feature.classification

sealed class ClassificationResult {
    data class Classified(
        val category: String,
        val confidence: Float
    ) : ClassificationResult()

    data object Unclassified : ClassificationResult()
}
