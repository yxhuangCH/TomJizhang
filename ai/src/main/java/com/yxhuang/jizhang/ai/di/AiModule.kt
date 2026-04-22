package com.yxhuang.jizhang.ai.di

import com.yxhuang.jizhang.ai.llm.DeepSeekLlmClient
import com.yxhuang.jizhang.ai.llm.LlmClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val aiModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single<LlmClient> {
        DeepSeekLlmClient(
            httpClient = get(),
            apiKey = getProperty("deepseek_api_key", "")
        )
    }
}
