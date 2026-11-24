package com.agyaccess.app.domain.model

import java.util.UUID

enum class ToolCallStatus {
    RUNNING,
    SUCCESS,
    FAILED
}

/**
 * Modèle scellé représentant un élément de l'historique d'une session agy.
 */
sealed interface ChatMessage {
    val id: String
    val timestampEpochMs: Long

    data class User(
        override val id: String = UUID.randomUUID().toString(),
        override val timestampEpochMs: Long = System.currentTimeMillis(),
        val content: String,
        val modelOverride: String? = null
    ) : ChatMessage

    data class Assistant(
        override val id: String = UUID.randomUUID().toString(),
        override val timestampEpochMs: Long = System.currentTimeMillis(),
        val content: String,
        val isStreaming: Boolean = false,
        val wasCancelled: Boolean = false
    ) : ChatMessage

    data class ToolCall(
        override val id: String = UUID.randomUUID().toString(),
        override val timestampEpochMs: Long = System.currentTimeMillis(),
        val toolName: String,
        val summary: String,
        val argumentsPreview: String? = null,
        val outputPreview: String? = null,
        val status: ToolCallStatus = ToolCallStatus.RUNNING
    ) : ChatMessage

    data class SystemError(
        override val id: String = UUID.randomUUID().toString(),
        override val timestampEpochMs: Long = System.currentTimeMillis(),
        val code: String?,
        val message: String,
        val recoverable: Boolean = true
    ) : ChatMessage
}
