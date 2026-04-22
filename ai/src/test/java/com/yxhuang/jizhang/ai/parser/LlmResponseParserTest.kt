package com.yxhuang.jizhang.ai.parser

import com.yxhuang.jizhang.ai.llm.LlmException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LlmResponseParserTest {

    @Test
    fun `parse valid json returns result`() {
        val json = """{"category":"饮品","rule":"merchant contains 星巴克","confidence":0.95}"""
        val result = LlmResponseParser.parse(json)
        assertEquals("饮品", result.category)
        assertEquals("merchant contains 星巴克", result.ruleKeyword)
        assertEquals(0.95f, result.confidence)
    }

    @Test
    fun `parse json with missing confidence uses default`() {
        val json = """{"category":"餐饮","rule":"merchant == 麦当劳"}"""
        val result = LlmResponseParser.parse(json)
        assertEquals("餐饮", result.category)
        assertEquals(0.8f, result.confidence)
    }

    @Test
    fun `parse json with extra fields ignores them`() {
        val json = """{"category":"交通","rule":"merchant contains 滴滴","confidence":0.9,"reason":"出租车服务"}"""
        val result = LlmResponseParser.parse(json)
        assertEquals("交通", result.category)
    }

    @Test
    fun `parse invalid json throws exception`() {
        assertThrows<LlmException> {
            LlmResponseParser.parse("not json")
        }
    }

    @Test
    fun `parse json with missing category throws exception`() {
        val json = """{"rule":"merchant contains 滴滴","confidence":0.9}"""
        assertThrows<LlmException> {
            LlmResponseParser.parse(json)
        }
    }

    @Test
    fun `parse json with empty category throws exception`() {
        val json = """{"category":"","rule":"merchant contains 滴滴","confidence":0.9}"""
        assertThrows<LlmException> {
            LlmResponseParser.parse(json)
        }
    }

    @Test
    fun `parse json with missing rule throws exception`() {
        val json = """{"category":"饮品","confidence":0.9}"""
        assertThrows<LlmException> {
            LlmResponseParser.parse(json)
        }
    }
}
