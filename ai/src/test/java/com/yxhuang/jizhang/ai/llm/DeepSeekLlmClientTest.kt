package com.yxhuang.jizhang.ai.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeepSeekLlmClientTest {

    private fun createClient(mockEngine: MockEngine): HttpClient {
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            expectSuccess = false
        }
    }

    @Test
    fun `classify returns parsed result from mock server`() = runTest {
        val innerJson = "{\\\"category\\\":\\\"饮品\\\",\\\"rule\\\":\\\"merchant contains 星巴克\\\",\\\"confidence\\\":0.95}"
        val responseJson = "{\"choices\":[{\"message\":{\"content\":\"$innerJson\"}}]}"
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = DeepSeekLlmClient(createClient(mockEngine), "fake-api-key")

        val result = client.classify("星巴克")
        assertEquals("饮品", result.category)
        assertEquals("merchant contains 星巴克", result.ruleKeyword)
        assertEquals(0.95f, result.confidence)
    }

    @Test
    fun `classify throws on empty response`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "{\"choices\":[]}",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = DeepSeekLlmClient(createClient(mockEngine), "fake-key")

        assertThrows<LlmException> {
            client.classify("星巴克")
        }
    }

    @Test
    fun `classify throws on http error`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "{\"error\":\"invalid key\"}",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = DeepSeekLlmClient(createClient(mockEngine), "fake-key")

        assertThrows<LlmException> {
            client.classify("星巴克")
        }
    }
}
