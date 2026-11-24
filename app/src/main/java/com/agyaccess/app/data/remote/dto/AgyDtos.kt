package com.agyaccess.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs réseau strictement typés (Zero Raw Command).
 * Aucune commande shell ni argument système brut n'est jamais construit côté client.
 */

@Serializable
data class HealthResponseDto(
    @SerialName("status") val status: String = "ok",
    @SerialName("host") val host: String = ""
)

@Serializable
data class SessionsListResponseDto(
    @SerialName("sessions") val sessions: List<SessionDto> = emptyList(),
    @SerialName("count") val count: Int = 0
)

@Serializable
data class SessionDto(
    @SerialName("session_id") val rawSessionId: String? = null,
    @SerialName("id") val rawId: String? = null,
    @SerialName("agy_conversation_id") val agyConversationId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: String = "idle",
    @SerialName("mode") val mode: String = "auto",
    @SerialName("archived") val archived: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at_epoch_ms") val createdAtEpochMs: Long? = null,
    @SerialName("updated_at_epoch_ms") val updatedAtEpochMs: Long? = null,
    @SerialName("active_model") val activeModel: String? = null,
    @SerialName("last_prompt") val lastPrompt: String? = null,
    @SerialName("events_count") val eventsCount: Int? = null,
    @SerialName("messages") val messages: List<MessageDto> = emptyList()
) {
    val sessionId: String
        get() = rawSessionId ?: rawId ?: ""
}

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("role") val role: String, // "user" | "assistant" | "tool_call" | "error"
    @SerialName("content") val content: String = "",
    @SerialName("timestamp") val timestampEpochMs: Long? = null,
    @SerialName("tool_name") val toolName: String? = null,
    @SerialName("tool_status") val toolStatus: String? = null,
    @SerialName("arguments_preview") val argumentsPreview: String? = null,
    @SerialName("output_preview") val outputPreview: String? = null,
    @SerialName("model_override") val modelOverride: String? = null
)

@Serializable
data class CreateSessionRequestDto(
    @SerialName("title") val title: String? = null
)

@Serializable
data class CreateSessionResponseDto(
    @SerialName("session_id") val rawSessionId: String? = null,
    @SerialName("id") val rawId: String? = null,
    @SerialName("agy_conversation_id") val agyConversationId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: String = "idle",
    @SerialName("mode") val mode: String = "auto",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("created_at_epoch_ms") val createdAtEpochMs: Long? = null
) {
    val sessionId: String
        get() = rawSessionId ?: rawId ?: ""
}

/**
 * Payload exact pour `POST /api/v1/sessions/{session_id}/prompt` :
 * `{"prompt": "texte utilisateur", "mode": "auto"|"accept-edits"|"plan"}`
 */
@Serializable
data class PromptRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("mode") val mode: String = "auto"
)

@Serializable
data class PromptAcceptedResponseDto(
    @SerialName("accepted") val accepted: Boolean = true,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("status") val status: String = "running",
    @SerialName("mode") val mode: String? = null,
    @SerialName("prompt") val prompt: String? = null
)

@Serializable
data class CancelResponseDto(
    @SerialName("cancelled") val cancelled: Boolean = true,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("status") val status: String = "idle",
    @SerialName("active") val active: Boolean = false,
    @SerialName("signal") val signal: String? = null
)

@Serializable
data class DeleteSessionResponseDto(
    @SerialName("deleted") val deleted: Boolean = true,
    @SerialName("session_id") val sessionId: String? = null
)

// --- DTOs des événements SSE (`GET /api/v1/sessions/{session_id}/stream`) ---

@Serializable
data class SseTokenPayloadDto(
    @SerialName("text") val text: String = "",
    @SerialName("delta") val delta: String? = null,
    @SerialName("message_id") val messageId: String? = null
)

@Serializable
data class SseStatusUpdatePayloadDto(
    @SerialName("status") val status: String = "running",
    @SerialName("detail") val detail: String = "",
    @SerialName("message") val message: String? = null
)

@Serializable
data class SseToolCallPayloadDto(
    @SerialName("call_id") val callId: String? = null,
    @SerialName("tool_name") val toolName: String = "tool",
    @SerialName("summary") val summary: String = "",
    @SerialName("arguments_preview") val argumentsPreview: String? = null,
    @SerialName("output_preview") val outputPreview: String? = null,
    @SerialName("status") val status: String = "running"
)

@Serializable
data class SseDonePayloadDto(
    @SerialName("finish_reason") val finishReason: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("returncode") val returncode: Int? = null
)

@Serializable
data class SseErrorPayloadDto(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String = "Erreur inconnue de l'agent.",
    @SerialName("recoverable") val recoverable: Boolean = true
)
