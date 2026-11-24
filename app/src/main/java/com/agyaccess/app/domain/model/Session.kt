package com.agyaccess.app.domain.model

import java.util.UUID

/**
 * Statut métier d'une session agy côté serveur et client.
 */
enum class SessionStatus {
    IDLE,
    RUNNING,
    WAITING,
    COMPLETED,
    ERROR
}

/**
 * État de la liaison réseau SSE pour une session donnée.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    STREAMING,
    RECONNECTING
}

/**
 * Représente une conversation distante avec l'agent IA agy.
 * Chaque session est identifiée de façon stricte par un UUID.
 */
data class Session(
    val sessionId: String,
    val title: String,
    val status: SessionStatus,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val activeModel: String? = null
) {
    companion object {
        private val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )

        fun isValidSessionId(id: String): Boolean = UUID_REGEX.matches(id)

        fun generateLocalFallbackId(): String = UUID.randomUUID().toString()
    }
}

/**
 * État d'exécution complet d'une session (historique, buffer de streaming,
 * statut intermédiaire de réflexion / exécution d'outil, et reconnexion).
 */
data class SessionRuntimeState(
    val session: Session,
    val messages: List<ChatMessage> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val liveStatusBanner: String? = null,
    val activeToolName: String? = null,
    val hasUnreadUpdates: Boolean = false,
    val lastError: String? = null,
    val reconnectAttempt: Int = 0
) {
    val isRunning: Boolean
        get() = session.status == SessionStatus.RUNNING ||
            session.status == SessionStatus.WAITING ||
            connectionState == ConnectionState.STREAMING ||
            connectionState == ConnectionState.RECONNECTING
}
