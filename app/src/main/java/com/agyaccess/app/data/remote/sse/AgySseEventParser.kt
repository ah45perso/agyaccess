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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parseur universel des événements SSE émis par `GET /api/v1/sessions/{session_id}/stream`.
 *
 * Supporte :
 * 1. Les événements SSE standards (`token`, `status_update`, `tool_call`, `done`, `error`).
 * 2. Les événements stream-json émis par le backend agy-bridge (`step_update`, `result`, `done`, `stderr`, `raw`, `init`).
 */
@Singleton
class AgySseEventParser @Inject constructor(
    private val json: Json
) {
    fun parse(eventId: String?, eventType: String?, rawData: String): StreamEvent? {
        val trimmed = rawData.trim()
        val normalizedType = eventType?.trim()?.lowercase()

        if (trimmed.isEmpty()) {
            return if (normalizedType == "done") StreamEvent.Done(eventId = eventId) else null
        }

        return runCatching {
            // 1. Tente d'analyser l'arbre JSON pour inspecter un éventuel champ "event" dans le payload
            val jsonElement = runCatching { json.parseToJsonElement(trimmed) }.getOrNull()
            val jsonObject = jsonElement as? JsonObject
            val payloadEventType = jsonObject?.get("event")?.jsonPrimitive?.contentOrNull?.lowercase()

            val effectiveType = payloadEventType ?: normalizedType ?: inferTypeFromJson(jsonObject)

            when (effectiveType) {
                // Événements agy stream-json natifs émis par le bridge
                "step_update" -> parseStepUpdate(eventId, jsonObject)
                "result" -> parseResult(eventId, jsonObject)
                "stderr" -> {
                    val msg = jsonObject?.get("message")?.jsonPrimitive?.contentOrNull.orEmpty()
                    StreamEvent.StatusUpdate(
                        eventId = eventId,
                        status = SessionStatus.RUNNING,
                        detail = msg
                    )
                }
                "raw" -> {
                    val text = jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: jsonObject?.get("data")?.jsonPrimitive?.contentOrNull
                        ?: trimmed
                    StreamEvent.Token(
                        eventId = eventId,
                        text = text
                    )
                }
                "init" -> {
                    StreamEvent.StatusUpdate(
                        eventId = eventId,
                        status = SessionStatus.RUNNING,
                        detail = "Agent initialisé"
                    )
                }

                // Événements SSE du contrat standard
                "token" -> {
                    val dto = json.decodeFromString<SseTokenPayloadDto>(trimmed)
                    val chunk = dto.delta ?: dto.text
                    StreamEvent.Token(
                        eventId = eventId,
                        text = chunk,
                        messageId = dto.messageId
                    )
                }

                "status_update" -> {
                    val dto = json.decodeFromString<SseStatusUpdatePayloadDto>(trimmed)
                    StreamEvent.StatusUpdate(
                        eventId = eventId,
                        status = dto.status.toSessionStatus(),
                        detail = dto.detail.ifBlank { dto.message.orEmpty() }
                    )
                }

                "tool_call" -> {
                    val dto = json.decodeFromString<SseToolCallPayloadDto>(trimmed)
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
                    val finishReason = jsonObject?.get("status")?.jsonPrimitive?.contentOrNull
                        ?: jsonObject?.get("finish_reason")?.jsonPrimitive?.contentOrNull
                        ?: "completed"
                    StreamEvent.Done(
                        eventId = eventId,
                        finishReason = finishReason
                    )
                }

                "error" -> {
                    val dto = json.decodeFromString<SseErrorPayloadDto>(trimmed)
                    StreamEvent.Error(
                        eventId = eventId,
                        code = dto.code,
                        message = dto.message,
                        recoverable = dto.recoverable
                    )
                }

                else -> {
                    // Fallback intelligent si le JSON contient du texte directement
                    if (jsonObject?.containsKey("text") == true || jsonObject?.containsKey("delta") == true) {
                        val dto = json.decodeFromString<SseTokenPayloadDto>(trimmed)
                        StreamEvent.Token(eventId = eventId, text = dto.delta ?: dto.text, messageId = dto.messageId)
                    } else {
                        null
                    }
                }
            }
        }.getOrElse { error ->
            StreamEvent.Error(
                eventId = eventId,
                code = "PARSE_ERROR",
                message = "Événement SSE illisible : ${error.localizedMessage}",
                recoverable = true
            )
        }
    }

    private fun inferTypeFromJson(obj: JsonObject?): String? {
        if (obj == null) return null
        return when {
            obj.containsKey("step_update") -> "step_update"
            obj.containsKey("result") -> "result"
            obj.containsKey("text") || obj.containsKey("delta") -> "token"
            obj.containsKey("finish_reason") -> "done"
            obj.containsKey("code") && obj.containsKey("message") -> "error"
            else -> null
        }
    }

    private fun parseStepUpdate(eventId: String?, root: JsonObject?): StreamEvent? {
        val stepUpdate = root?.get("step_update")?.let { runCatching { it.jsonObject }.getOrNull() }
            ?: return null

        val stepType = stepUpdate["step_type"]?.jsonPrimitive?.contentOrNull
        val textDelta = stepUpdate["text_delta"]?.jsonPrimitive?.contentOrNull
        val state = stepUpdate["state"]?.jsonPrimitive?.contentOrNull

        if (stepType == "agent_response" && !textDelta.isNullOrEmpty()) {
            return StreamEvent.Token(
                eventId = eventId,
                text = textDelta
            )
        }

        if (stepType == "tool_call") {
            val toolName = stepUpdate["tool_name"]?.jsonPrimitive?.contentOrNull ?: "tool"
            val summary = stepUpdate["summary"]?.jsonPrimitive?.contentOrNull
                ?: "Exécution de $toolName"
            val args = stepUpdate["arguments_preview"]?.jsonPrimitive?.contentOrNull
            val output = stepUpdate["output_preview"]?.jsonPrimitive?.contentOrNull
            val toolStatus = if (state.equals("DONE", ignoreCase = true)) {
                ToolCallStatus.SUCCESS
            } else {
                ToolCallStatus.RUNNING
            }
            return StreamEvent.ToolCall(
                eventId = eventId,
                callId = UUID.randomUUID().toString(),
                toolName = toolName,
                summary = summary,
                argumentsPreview = args,
                outputPreview = output,
                toolStatus = toolStatus
            )
        }

        return if (!state.isNullOrBlank()) {
            StreamEvent.StatusUpdate(
                eventId = eventId,
                status = state.toSessionStatus(),
                detail = stepType.orEmpty()
            )
        } else {
            null
        }
    }

    private fun parseResult(eventId: String?, root: JsonObject?): StreamEvent {
        val result = root?.get("result")?.let { runCatching { it.jsonObject }.getOrNull() }
        val status = result?.get("status")?.jsonPrimitive?.contentOrNull ?: "SUCCESS"
        val responseText = result?.get("response")?.jsonPrimitive?.contentOrNull

        return if (!responseText.isNullOrEmpty()) {
            StreamEvent.Token(
                eventId = eventId,
                text = responseText
            )
        } else {
            StreamEvent.StatusUpdate(
                eventId = eventId,
                status = SessionStatus.COMPLETED,
                detail = "Tâche terminée ($status)"
            )
        }
    }

    companion object {
        fun String.toSessionStatus(): SessionStatus = when (trim().lowercase()) {
            "running", "streaming", "thinking", "executing", "active" -> SessionStatus.RUNNING
            "waiting", "queued", "pending" -> SessionStatus.WAITING
            "completed", "done", "finished", "success" -> SessionStatus.COMPLETED
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
