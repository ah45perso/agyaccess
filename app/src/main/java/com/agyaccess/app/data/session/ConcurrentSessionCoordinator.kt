package com.agyaccess.app.data.session

import com.agyaccess.app.data.remote.sse.AgySseClient
import com.agyaccess.app.di.ApplicationScope
import com.agyaccess.app.domain.model.ChatMessage
import com.agyaccess.app.domain.model.ConnectionState
import com.agyaccess.app.domain.model.Session
import com.agyaccess.app.domain.model.SessionRuntimeState
import com.agyaccess.app.domain.model.SessionStatus
import com.agyaccess.app.domain.model.StreamEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinateur multi-sessions concurrentes en arrière-plan.
 *
 * Chaque session dispose de son propre [Job] SSE indépendant exécuté dans un
 * [CoroutineScope] muni d'un `SupervisorJob` (une erreur sur la session A n'affecte
 * jamais la session B). L'utilisateur peut lancer un prompt sur A, basculer sur B et
 * lancer un second prompt simultanément : les deux flux continuent d'accumuler leurs
 * tokens et statuts d'outils dans [_sessionsRuntime] sans jamais bloquer l'UI.
 */
@Singleton
class ConcurrentSessionCoordinator @Inject constructor(
    private val sseClient: AgySseClient,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _sessionsRuntime = MutableStateFlow<Map<String, SessionRuntimeState>>(emptyMap())
    val sessionsRuntime: StateFlow<Map<String, SessionRuntimeState>> = _sessionsRuntime.asStateFlow()

    private val activeStreamJobs = ConcurrentHashMap<String, Job>()

    @Volatile
    private var currentlyViewedSessionId: String? = null

    fun setViewedSession(sessionId: String?) {
        currentlyViewedSessionId = sessionId
        if (sessionId != null) {
            markRead(sessionId)
        }
    }

    fun markRead(sessionId: String) {
        updateSession(sessionId) { state ->
            if (state.hasUnreadUpdates) state.copy(hasUnreadUpdates = false) else state
        }
    }

    /**
     * Fusionne les sessions synchronisées depuis le NAS tout en préservant les buffers
     * de streaming locaux pour les sessions déjà actives en arrière-plan.
     */
    fun mergeRemoteSessions(remoteStates: List<SessionRuntimeState>) {
        _sessionsRuntime.update { current ->
            val merged = current.toMutableMap()
            for (remote in remoteStates) {
                val id = remote.session.sessionId
                val existing = merged[id]
                if (existing == null) {
                    merged[id] = remote
                } else {
                    val isStreamingLocally = activeStreamJobs[id]?.isActive == true
                    merged[id] = existing.copy(
                        session = if (isStreamingLocally) {
                            remote.session.copy(status = existing.session.status)
                        } else {
                            remote.session
                        },
                        messages = if (isStreamingLocally || existing.messages.size >= remote.messages.size) {
                            existing.messages
                        } else {
                            remote.messages
                        }
                    )
                }
            }
            merged
        }
    }

    fun registerNewSession(session: Session) {
        _sessionsRuntime.update { current ->
            current + (session.sessionId to SessionRuntimeState(session = session))
        }
    }

    fun toggleArchive(sessionId: String) {
        updateSession(sessionId) { state ->
            state.copy(session = state.session.copy(isArchived = !state.session.isArchived))
        }
    }

    /**
     * Prépare la session de façon optimiste : ajoute le message utilisateur et une bulle
     * assistant en streaming, puis met à jour le titre si la session était vierge.
     */
    fun appendOptimisticPrompt(
        sessionId: String,
        prompt: String,
        modelOverride: String?
    ) {
        val now = System.currentTimeMillis()
        updateSession(sessionId) { state ->
            val updatedTitle = if (state.messages.isEmpty() && state.session.title.startsWith("Nouvelle")) {
                prompt.lineSequence().firstOrNull()?.take(42)?.trim().orEmpty()
                    .ifBlank { state.session.title }
            } else {
                state.session.title
            }
            val userMsg = ChatMessage.User(
                content = prompt,
                timestampEpochMs = now,
                modelOverride = modelOverride
            )
            val streamingPlaceholder = ChatMessage.Assistant(
                id = "stream_${UUID.randomUUID()}",
                timestampEpochMs = now + 1,
                content = "",
                isStreaming = true
            )
            state.copy(
                session = state.session.copy(
                    title = updatedTitle,
                    status = SessionStatus.WAITING,
                    updatedAtEpochMs = now,
                    activeModel = modelOverride ?: state.session.activeModel
                ),
                messages = state.messages + userMsg + streamingPlaceholder,
                liveStatusBanner = "Connexion au flux agy…",
                lastError = null
            )
        }
    }

    /**
     * Lance (ou garantit) une écoute SSE dédiée à [sessionId] en tâche de fond.
     * Attend brièvement l'ouverture du flux (`onOpen`) pour éviter la perte des premiers tokens.
     */
    suspend fun ensureStreamStarted(sessionId: String, waitForOpenMs: Long = 1_500L) {
        val existingJob = activeStreamJobs[sessionId]
        if (existingJob != null && existingJob.isActive) return

        val openedSignal: CompletableDeferred<Unit> = sseClient.createReadinessSignal()

        updateSession(sessionId) { state ->
            state.copy(
                connectionState = ConnectionState.CONNECTING,
                reconnectAttempt = 0
            )
        }

        val job = appScope.launch {
            try {
                sseClient.streamSession(
                    sessionId = sessionId,
                    onStreamOpened = { openedSignal.complete(Unit) }
                ).collect { signal ->
                    handleSseSignal(sessionId, signal)
                }
            } finally {
                if (!openedSignal.isCompleted) {
                    openedSignal.complete(Unit)
                }
                activeStreamJobs.remove(sessionId)
                finalizeDisconnectedState(sessionId)
            }
        }

        activeStreamJobs[sessionId] = job
        if (waitForOpenMs > 0L) {
            withTimeoutOrNull(waitForOpenMs) { openedSignal.await() }
        }
    }

    fun attachStreamIfNotRunning(sessionId: String) {
        if (activeStreamJobs[sessionId]?.isActive == true) return
        appScope.launch {
            ensureStreamStarted(sessionId, waitForOpenMs = 0L)
        }
    }

    fun markPromptSubmissionFailed(sessionId: String, errorMessage: String) {
        stopLocalStream(sessionId, markCancelled = false)
        updateSession(sessionId) { state ->
            val cleanedMessages = finalizeActiveAssistantMessage(
                messages = state.messages,
                cancelled = false,
                dropIfEmpty = true
            ) + ChatMessage.SystemError(
                code = "PROMPT_FAILED",
                message = errorMessage,
                recoverable = true
            )
            state.copy(
                session = state.session.copy(
                    status = SessionStatus.ERROR,
                    updatedAtEpochMs = System.currentTimeMillis()
                ),
                messages = cleanedMessages,
                connectionState = ConnectionState.DISCONNECTED,
                liveStatusBanner = null,
                activeToolName = null,
                lastError = errorMessage
            )
        }
    }

    fun stopLocalStream(sessionId: String, markCancelled: Boolean) {
        activeStreamJobs.remove(sessionId)?.cancel()
        updateSession(sessionId) { state ->
            val finalizedMessages = finalizeActiveAssistantMessage(
                messages = state.messages,
                cancelled = markCancelled,
                dropIfEmpty = !markCancelled
            )
            state.copy(
                session = state.session.copy(
                    status = if (markCancelled) SessionStatus.IDLE else state.session.status,
                    updatedAtEpochMs = System.currentTimeMillis()
                ),
                messages = finalizedMessages,
                connectionState = ConnectionState.DISCONNECTED,
                liveStatusBanner = null,
                activeToolName = null
            )
        }
    }

    private fun handleSseSignal(sessionId: String, signal: AgySseClient.SseSignal) {
        val isBackground = currentlyViewedSessionId != sessionId
        when (signal) {
            is AgySseClient.SseSignal.Connected -> {
                updateSession(sessionId) { state ->
                    state.copy(
                        connectionState = ConnectionState.STREAMING,
                        reconnectAttempt = 0,
                        liveStatusBanner = state.liveStatusBanner ?: "Connecté à agy…"
                    )
                }
            }

            is AgySseClient.SseSignal.Reconnecting -> {
                updateSession(sessionId) { state ->
                    state.copy(
                        connectionState = ConnectionState.RECONNECTING,
                        reconnectAttempt = signal.attempt,
                        liveStatusBanner = "Reconnexion (${signal.attempt}) : ${signal.reason}"
                    )
                }
            }

            is AgySseClient.SseSignal.Terminated -> {
                updateSession(sessionId) { state ->
                    val finishedStatus = if (state.session.status == SessionStatus.ERROR) {
                        SessionStatus.ERROR
                    } else {
                        SessionStatus.COMPLETED
                    }
                    state.copy(
                        session = state.session.copy(
                            status = finishedStatus,
                            updatedAtEpochMs = System.currentTimeMillis()
                        ),
                        messages = finalizeActiveAssistantMessage(
                            messages = state.messages,
                            cancelled = false,
                            dropIfEmpty = true
                        ),
                        connectionState = ConnectionState.DISCONNECTED,
                        liveStatusBanner = null,
                        activeToolName = null
                    )
                }
            }

            is AgySseClient.SseSignal.Event -> {
                applyStreamEvent(sessionId, signal.event, isBackground)
            }
        }
    }

    private fun applyStreamEvent(
        sessionId: String,
        event: StreamEvent,
        isBackground: Boolean
    ) {
        val now = System.currentTimeMillis()
        updateSession(sessionId) { state ->
            when (event) {
                is StreamEvent.Token -> {
                    val updatedMessages = appendTokenToAssistant(
                        messages = state.messages,
                        token = event.text,
                        messageId = event.messageId,
                        timestamp = now
                    )
                    state.copy(
                        session = state.session.copy(
                            status = SessionStatus.RUNNING,
                            updatedAtEpochMs = now
                        ),
                        messages = updatedMessages,
                        connectionState = ConnectionState.STREAMING,
                        liveStatusBanner = null,
                        hasUnreadUpdates = state.hasUnreadUpdates || isBackground
                    )
                }

                is StreamEvent.StatusUpdate -> {
                    state.copy(
                        session = state.session.copy(
                            status = event.status,
                            updatedAtEpochMs = now
                        ),
                        liveStatusBanner = event.detail.ifBlank { null },
                        hasUnreadUpdates = state.hasUnreadUpdates || isBackground
                    )
                }

                is StreamEvent.ToolCall -> {
                    val updatedMessages = upsertToolCallMessage(
                        messages = state.messages,
                        event = event,
                        timestamp = now
                    )
                    state.copy(
                        session = state.session.copy(
                            status = SessionStatus.RUNNING,
                            updatedAtEpochMs = now
                        ),
                        messages = updatedMessages,
                        activeToolName = event.toolName,
                        liveStatusBanner = "Outil : ${event.toolName} — ${event.summary}",
                        hasUnreadUpdates = state.hasUnreadUpdates || isBackground
                    )
                }

                is StreamEvent.Done -> {
                    state.copy(
                        session = state.session.copy(
                            status = SessionStatus.COMPLETED,
                            updatedAtEpochMs = now
                        ),
                        messages = finalizeActiveAssistantMessage(
                            messages = state.messages,
                            cancelled = false,
                            dropIfEmpty = true
                        ),
                        connectionState = ConnectionState.DISCONNECTED,
                        liveStatusBanner = null,
                        activeToolName = null,
                        hasUnreadUpdates = state.hasUnreadUpdates || isBackground
                    )
                }

                is StreamEvent.Error -> {
                    val updatedMessages = finalizeActiveAssistantMessage(
                        messages = state.messages,
                        cancelled = false,
                        dropIfEmpty = true
                    ) + ChatMessage.SystemError(
                        code = event.code,
                        message = event.message,
                        recoverable = event.recoverable,
                        timestampEpochMs = now
                    )
                    state.copy(
                        session = state.session.copy(
                            status = SessionStatus.ERROR,
                            updatedAtEpochMs = now
                        ),
                        messages = updatedMessages,
                        connectionState = if (event.recoverable) {
                            state.connectionState
                        } else {
                            ConnectionState.DISCONNECTED
                        },
                        liveStatusBanner = null,
                        activeToolName = null,
                        lastError = event.message,
                        hasUnreadUpdates = state.hasUnreadUpdates || isBackground
                    )
                }
            }
        }
    }

    private fun appendTokenToAssistant(
        messages: List<ChatMessage>,
        token: String,
        messageId: String?,
        timestamp: Long
    ): List<ChatMessage> {
        val last = messages.lastOrNull()
        return if (last is ChatMessage.Assistant && last.isStreaming) {
            val newContent = when {
                last.content.isEmpty() -> token
                token == last.content -> last.content
                token.startsWith(last.content) -> token
                else -> last.content + token
            }
            messages.dropLast(1) + last.copy(
                id = messageId ?: last.id,
                content = newContent,
                timestampEpochMs = timestamp
            )
        } else {
            messages + ChatMessage.Assistant(
                id = messageId ?: "stream_${UUID.randomUUID()}",
                content = token,
                isStreaming = true,
                timestampEpochMs = timestamp
            )
        }
    }

    private fun upsertToolCallMessage(
        messages: List<ChatMessage>,
        event: StreamEvent.ToolCall,
        timestamp: Long
    ): List<ChatMessage> {
        val existingIndex = messages.indexOfLast {
            it is ChatMessage.ToolCall && it.id == event.callId
        }
        val toolMessage = ChatMessage.ToolCall(
            id = event.callId,
            timestampEpochMs = timestamp,
            toolName = event.toolName,
            summary = event.summary,
            argumentsPreview = event.argumentsPreview,
            outputPreview = event.outputPreview,
            status = event.toolStatus
        )
        return if (existingIndex >= 0) {
            messages.toMutableList().apply { set(existingIndex, toolMessage) }
        } else {
            // Si une bulle assistant vide était en attente à la fin, on la conserve après l'outil
            val last = messages.lastOrNull()
            if (last is ChatMessage.Assistant && last.isStreaming && last.content.isEmpty()) {
                messages.dropLast(1) + toolMessage + last
            } else {
                messages + toolMessage
            }
        }
    }

    private fun finalizeActiveAssistantMessage(
        messages: List<ChatMessage>,
        cancelled: Boolean,
        dropIfEmpty: Boolean
    ): List<ChatMessage> {
        val last = messages.lastOrNull() as? ChatMessage.Assistant ?: return messages
        if (!last.isStreaming) return messages
        if (dropIfEmpty && last.content.isBlank()) {
            return messages.dropLast(1)
        }
        return messages.dropLast(1) + last.copy(
            isStreaming = false,
            wasCancelled = cancelled
        )
    }

    private fun finalizeDisconnectedState(sessionId: String) {
        updateSession(sessionId) { state ->
            if (state.connectionState != ConnectionState.DISCONNECTED) {
                state.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    messages = finalizeActiveAssistantMessage(
                        messages = state.messages,
                        cancelled = false,
                        dropIfEmpty = true
                    )
                )
            } else {
                state
            }
        }
    }

    private fun updateSession(
        sessionId: String,
        transform: (SessionRuntimeState) -> SessionRuntimeState
    ) {
        _sessionsRuntime.update { map ->
            val current = map[sessionId] ?: return@update map
            map + (sessionId to transform(current))
        }
    }
}
