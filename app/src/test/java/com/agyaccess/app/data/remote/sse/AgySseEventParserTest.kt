package com.agyaccess.app.data.remote.sse

import com.agyaccess.app.domain.model.SessionStatus
import com.agyaccess.app.domain.model.StreamEvent
import com.agyaccess.app.domain.model.ToolCallStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgySseEventParserTest {

    private val parser = AgySseEventParser(
        Json {
            ignoreUnknownKeys = true
            isLenient = false
        }
    )

    @Test
    fun `parses token event`() {
        val event = parser.parse("42", "token", """{"text":"Bonjour ","message_id":"m1"}""")
        assertTrue(event is StreamEvent.Token)
        val token = event as StreamEvent.Token
        assertEquals("42", token.eventId)
        assertEquals("Bonjour ", token.text)
        assertEquals("m1", token.messageId)
    }

    @Test
    fun `parses status_update event`() {
        val event = parser.parse("43", "status_update", """{"status":"running","detail":"Réflexion en cours"}""")
        assertTrue(event is StreamEvent.StatusUpdate)
        val status = event as StreamEvent.StatusUpdate
        assertEquals(SessionStatus.RUNNING, status.status)
        assertEquals("Réflexion en cours", status.detail)
    }

    @Test
    fun `parses tool_call event`() {
        val event = parser.parse(
            "44",
            "tool_call",
            """{"call_id":"tc-1","tool_name":"nas_disk_usage","summary":"Lecture espace disque","status":"success"}"""
        )
        assertTrue(event is StreamEvent.ToolCall)
        val tool = event as StreamEvent.ToolCall
        assertEquals("tc-1", tool.callId)
        assertEquals("nas_disk_usage", tool.toolName)
        assertEquals(ToolCallStatus.SUCCESS, tool.toolStatus)
    }

    @Test
    fun `parses done and error events`() {
        val done = parser.parse("45", "done", """{"finish_reason":"stop"}""")
        assertTrue(done is StreamEvent.Done)
        assertEquals("stop", (done as StreamEvent.Done).finishReason)

        val err = parser.parse("46", "error", """{"code":"RATE_LIMIT","message":"Trop de requêtes","recoverable":true}""")
        assertTrue(err is StreamEvent.Error)
        assertEquals("RATE_LIMIT", (err as StreamEvent.Error).code)
    }
}
