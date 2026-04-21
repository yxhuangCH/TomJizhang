package com.yxhuang.jizhang.ai.llm

interface LlmClient {
    suspend fun classify(merchant: String): LlmClassificationResult
}

data class LlmClassificationResult(
    val category: String,
    val ruleKeyword: String,
    val confidence: Float
)

class LlmException(message: String) : Exception(message)
