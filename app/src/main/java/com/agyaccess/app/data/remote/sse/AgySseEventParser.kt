package com.agyaccess.app.data.remote.sse

import com.agyaccess.app.data.remote.dto.SseDonePayloadDto
import com.agyaccess.app.data.remote.dto.SseErrorPayloadDto
import com.agyaccess.app.data.remote.dto.SseStatusUpdatePayloadDto
import com.agyaccess.app.data.remote.dto.SseTokenPayloadDto
import com.agyaccess.app.data.remote.dto.SseToolCallPayloadDto
import com.agyaccess.app.domain.model.SessionStatus
import com.agyaccess.app.domain.model.StreamEvent
import com.agyaccess.app.domain.model.ToolCallStatus
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parseur strict des événements SSE émis par `GET /api/v1/sessions/{session_id}/stream`.
 * Supporte les 5 événements du contrat : `token`, `status_update`, `tool_call`, `done`, `error`.
 */
@Singleton
class AgySseEventParser @Inject constructor(
    private val json: Json
) {
    fun parse(eventId: String?, eventType: String?, rawData: String): StreamEvent? {
        val normalizedType = eventType?.trim()?.lowercase() ?: "token"
        if (rawData.isBlank() && normalizedType != "done") return null

        return runCatching {
            when (normalizedType) {
                "token" -> {
                    val dto = json.decodeFromString<SseTokenPayloadDto>(rawData)
                    val chunk = dto.delta ?: dto.text
                    StreamEvent.Token(
                        eventId = eventId,
                        text = chunk,
                        messageId = dto.messageId
                    )
                }

                "status_update" -> {
                    val dto = json.decodeFromString<SseStatusUpdatePayloadDto>(rawData)
                    StreamEvent.StatusUpdate(
                        eventId = eventId,
                        status = dto.status.toSessionStatus(),
                        detail = dto.detail.ifBlank { dto.message.orEmpty() }
                    )
                }

                "tool_call" -> {
                    val dto = json.decodeFromString<SseToolCallPayloadDto>(rawData)
                    StreamEvent.ToolCall(
                        eventId = eventId,
                        callId = dto.callId ?: UUID.randomUUID().toString(),
                        toolName = dto.toolName,
                        summary = dto.summary.ifBlank { "Exécution de ${dto.toolName}" },
                        argumentsPreview = dto.argumentsPreview,
                        outputPreview = dto.outputPreview,
                        toolStatus = dto.status.toToolCallStatus()
                    )
                }

                "done" -> {
                    val dto = if (rawData.isBlank()) {
                        SseDonePayloadDto()
                    } else {
                        json.decodeFromString<SseDonePayloadDto>(rawData)
                    }
                    StreamEvent.Done(
                        eventId = eventId,
                        finishReason = dto.finishReason
                    )
                }

                "error" -> {
                    val dto = json.decodeFromString<SseErrorPayloadDto>(rawData)
                    StreamEvent.Error(
                        eventId = eventId,
                        code = dto.code,
                        message = dto.message,
                        recoverable = dto.recoverable
                    )
                }

                else -> null
            }
        }.getOrElse { error ->
            StreamEvent.Error(
                eventId = eventId,
                code = "PARSE_ERROR",
                message = "Événement SSE '$normalizedType' illisible : ${error.localizedMessage}",
                recoverable = true
            )
        }
    }

    companion object {
        fun String.toSessionStatus(): SessionStatus = when (trim().lowercase()) {
            "running", "streaming", "thinking", "executing" -> SessionStatus.RUNNING
            "waiting", "queued", "pending" -> SessionStatus.WAITING
            "completed", "done", "finished" -> SessionStatus.COMPLETED
            "error", "failed" -> SessionStatus.ERROR
            else -> SessionStatus.IDLE
        }

        fun String.toToolCallStatus(): ToolCallStatus = when (trim().lowercase()) {
            "success", "completed", "done", "ok" -> ToolCallStatus.SUCCESS
            "failed", "error" -> ToolCallStatus.FAILED
            else -> ToolCallStatus.RUNNING
        }
    }
}
