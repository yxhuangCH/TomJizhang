package com.yxhuang.jizhang.ai.llm

import com.yxhuang.jizhang.ai.parser.LlmResponseParser
import com.yxhuang.jizhang.ai.prompt.LlmPrompts
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DeepSeekLlmClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com"
) : LlmClient {

    override suspend fun classify(merchant: String): LlmClassificationResult {
        val response = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(
                ChatCompletionRequest(
                    model = "deepseek-chat",
                    messages = listOf(
                        Message("system", LlmPrompts.SYSTEM),
                        Message("user", LlmPrompts.user(merchant))
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )
            )
        }

        if (!response.status.isSuccess()) {
            throw LlmException("LLM API error: ${response.status}")
        }

        val jsonText = response.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val body = json.decodeFromString<ChatCompletionResponse>(jsonText)
        val content = body.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Empty response from LLM")

        return LlmResponseParser.parse(content)
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat
)

@Serializable
private data class Message(val role: String = "", val content: String)

@Serializable
private data class ResponseFormat(val type: String)

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
private data class Choice(val message: Message)
