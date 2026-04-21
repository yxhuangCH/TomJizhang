package com.yxhuang.jizhang.ai.parser

import com.yxhuang.jizhang.ai.llm.LlmClassificationResult
import com.yxhuang.jizhang.ai.llm.LlmException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object LlmResponseParser {
    fun parse(jsonString: String): LlmClassificationResult {
        val json = Json { ignoreUnknownKeys = true }
        val raw = try {
            json.decodeFromString<LlmRawResponse>(jsonString)
        } catch (_: Exception) {
            throw LlmException("Invalid JSON response")
        }

        val category = raw.category?.takeIf { it.isNotBlank() }
            ?: throw LlmException("Missing or empty category")
        val rule = raw.rule?.takeIf { it.isNotBlank() }
            ?: throw LlmException("Missing or empty rule")
        val confidence = raw.confidence?.coerceIn(0.0f, 1.0f) ?: 0.8f

        return LlmClassificationResult(category, rule, confidence)
    }
}

@Serializable
private data class LlmRawResponse(
    val category: String? = null,
    val rule: String? = null,
    val confidence: Float? = null
)
