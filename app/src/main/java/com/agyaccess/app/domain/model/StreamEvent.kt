package com.agyaccess.app.domain.model

/**
 * Événements temps réel reçus depuis le flux SSE
 * `GET /api/v1/sessions/{session_id}/stream`.
 */
sealed interface StreamEvent {
    val eventId: String?

    /** Fragment de texte (token) généré par l'agent. */
    data class Token(
        override val eventId: String?,
        val text: String,
        val messageId: String? = null
    ) : StreamEvent

    /** Statut intermédiaire (ex: "Réflexion en cours...", "Analyse du contexte"). */
    data class StatusUpdate(
        override val eventId: String?,
        val status: SessionStatus,
        val detail: String
    ) : StreamEvent

    /** Exécution ou mise à jour d'un appel d'outil côté agent agy. */
    data class ToolCall(
        override val eventId: String?,
        val callId: String,
        val toolName: String,
        val summary: String,
        val argumentsPreview: String? = null,
        val outputPreview: String? = null,
        val toolStatus: ToolCallStatus
    ) : StreamEvent

    /** Fin propre de la réponse de l'agent. */
    data class Done(
        override val eventId: String?,
        val finishReason: String? = null
    ) : StreamEvent

    /** Erreur renvoyée par le backend ou la couche transport. */
    data class Error(
        override val eventId: String?,
        val code: String?,
        val message: String,
        val recoverable: Boolean = true
    ) : StreamEvent
}
