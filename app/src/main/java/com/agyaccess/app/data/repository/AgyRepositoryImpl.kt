package com.agyaccess.app.data.repository

import com.agyaccess.app.data.remote.api.AgyApiService
import com.agyaccess.app.data.remote.dto.CreateSessionRequestDto
import com.agyaccess.app.data.remote.dto.MessageDto
import com.agyaccess.app.data.remote.dto.PromptRequestDto
import com.agyaccess.app.data.remote.dto.SessionDto
import com.agyaccess.app.data.remote.sse.AgySseEventParser.Companion.toSessionStatus
import com.agyaccess.app.data.remote.sse.AgySseEventParser.Companion.toToolCallStatus
import com.agyaccess.app.data.session.ConcurrentSessionCoordinator
import com.agyaccess.app.domain.model.ChatMessage
import com.agyaccess.app.domain.model.Session
import com.agyaccess.app.domain.model.SessionRuntimeState
import com.agyaccess.app.domain.model.SessionStatus
import com.agyaccess.app.domain.repository.AgyRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgyRepositoryImpl @Inject constructor(
    private val apiService: AgyApiService,
    private val coordinator: ConcurrentSessionCoordinator
) : AgyRepository {

    override val sessionsRuntime: StateFlow<Map<String, SessionRuntimeState>>
        get() = coordinator.sessionsRuntime

    override suspend fun refreshSessions(): Result<List<Session>> = runCatching {
        val response = apiService.listSessions()
        val runtimeStates = response.sessions
            .filter { Session.isValidSessionId(it.sessionId) }
            .map { it.toRuntimeState() }

        coordinator.mergeRemoteSessions(runtimeStates)

        // Raccroche automatiquement les flux SSE pour les sessions distantes actives
        runtimeStates
            .filter { it.session.status == SessionStatus.RUNNING || it.session.status == SessionStatus.WAITING }
            .forEach { coordinator.attachStreamIfNotRunning(it.session.sessionId) }

        runtimeStates.map { it.session }
    }

    override suspend fun createSession(title: String?): Result<Session> = runCatching {
        val response = apiService.createSession(CreateSessionRequestDto(title = title))
        require(Session.isValidSessionId(response.sessionId)) {
            "Le serveur NAS a retourné un session_id non conforme au format UUID."
        }
        val now = response.createdAtEpochMs ?: System.currentTimeMillis()
        val session = Session(
            sessionId = response.sessionId,
            title = response.title?.takeIf { it.isNotBlank() } ?: "Nouvelle conversation",
            status = response.status.toSessionStatus(),
            isArchived = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        coordinator.registerNewSession(session)
        session
    }

    override suspend fun sendPrompt(
        sessionId: String,
        prompt: String,
        modelOverride: String?
    ): Result<Unit> {
        if (!Session.isValidSessionId(sessionId)) {
            return Result.failure(IllegalArgumentException("session_id UUID invalide."))
        }

        // 1. Mise à jour optimiste de la conversation
        coordinator.appendOptimisticPrompt(sessionId, prompt, modelOverride)

        // 2. Ouvre d'abord le flux SSE pour ne manquer aucun token précoce
        coordinator.ensureStreamStarted(sessionId)

        // 3. Envoie le DTO JSON strictement typé (Zero Raw Command)
        return runCatching {
            apiService.sendPrompt(
                sessionId = sessionId,
                body = PromptRequestDto(
                    prompt = prompt,
                    modelOverride = modelOverride
                )
            )
            Unit
        }.onFailure { throwable ->
            coordinator.markPromptSubmissionFailed(
                sessionId = sessionId,
                errorMessage = throwable.localizedMessage
                    ?: "Échec de l'envoi du prompt vers le NAS."
            )
        }
    }

    override fun attachStreamIfRunning(sessionId: String) {
        if (Session.isValidSessionId(sessionId)) {
            coordinator.attachStreamIfNotRunning(sessionId)
        }
    }

    override suspend fun cancelSession(sessionId: String): Result<Unit> {
        if (!Session.isValidSessionId(sessionId)) {
            return Result.failure(IllegalArgumentException("session_id UUID invalide."))
        }
        coordinator.stopLocalStream(sessionId, markCancelled = true)
        return runCatching {
            apiService.cancelSession(sessionId)
            Unit
        }
    }

    override fun markSessionRead(sessionId: String) {
        coordinator.setViewedSession(sessionId)
    }

    override fun toggleArchiveLocal(sessionId: String) {
        coordinator.toggleArchive(sessionId)
    }

    private fun SessionDto.toRuntimeState(): SessionRuntimeState {
        val now = System.currentTimeMillis()
        val session = Session(
            sessionId = sessionId,
            title = title?.takeIf { it.isNotBlank() } ?: "Session ${sessionId.take(8)}",
            status = status.toSessionStatus(),
            isArchived = archived,
            createdAtEpochMs = createdAtEpochMs ?: now,
            updatedAtEpochMs = updatedAtEpochMs ?: createdAtEpochMs ?: now,
            activeModel = activeModel
        )
        return SessionRuntimeState(
            session = session,
            messages = messages.map { it.toDomain() }
        )
    }

    private fun MessageDto.toDomain(): ChatMessage {
        val ts = timestampEpochMs ?: System.currentTimeMillis()
        return when (role.trim().lowercase()) {
            "user" -> ChatMessage.User(
                id = id,
                timestampEpochMs = ts,
                content = content,
                modelOverride = modelOverride
            )

            "tool_call", "tool" -> ChatMessage.ToolCall(
                id = id,
                timestampEpochMs = ts,
                toolName = toolName ?: "tool",
                summary = content.ifBlank { "Appel d'outil" },
                argumentsPreview = argumentsPreview,
                outputPreview = outputPreview,
                status = (toolStatus ?: "success").toToolCallStatus()
            )

            "error" -> ChatMessage.SystemError(
                id = id,
                timestampEpochMs = ts,
                code = null,
                message = content
            )

            else -> ChatMessage.Assistant(
                id = id,
                timestampEpochMs = ts,
                content = content,
                isStreaming = false
            )
        }
    }
}
