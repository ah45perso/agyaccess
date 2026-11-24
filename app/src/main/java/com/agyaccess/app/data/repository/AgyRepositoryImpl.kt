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
import java.time.Instant
import java.time.OffsetDateTime
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
        val sid = response.sessionId
        require(Session.isValidSessionId(sid)) {
            "Le serveur NAS a retourné un session_id non conforme au format UUID."
        }
        val now = parseIsoOrEpoch(response.createdAt, response.createdAtEpochMs)
        val session = Session(
            sessionId = sid,
            title = response.title?.takeIf { it.isNotBlank() } ?: "Nouvelle conversation",
            status = response.status.toSessionStatus(),
            isArchived = false,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            activeModel = response.mode
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

        val executionMode = when (modelOverride?.trim()?.lowercase()) {
            "accept-edits" -> "accept-edits"
            "plan" -> "plan"
            else -> "auto"
        }

        // 1. Mise à jour optimiste de la conversation
        coordinator.appendOptimisticPrompt(sessionId, prompt, executionMode)

        // 2. Ouvre d'abord le flux SSE pour ne manquer aucun token précoce
        coordinator.ensureStreamStarted(sessionId)

        // 3. Envoie le DTO JSON strictement typé (Zero Raw Command)
        return runCatching {
            apiService.sendPrompt(
                sessionId = sessionId,
                body = PromptRequestDto(
                    prompt = prompt,
                    mode = executionMode
                )
            )
            Unit
        }.onFailure { throwable ->
            coordinator.markPromptSubmissionFailed(
                sessionId = sessionId,
                errorMessage = throwable.localizedMessage
                    ?: "Échec de l'envoi du prompt vers le serveur agy."
            )
        }
    }

    override fun attachStreamIfRunning(sessionId: String) {
        if (Session.isValidSessionId(sessionId)) {
            coordinator.attachStreamIfNotRunning(sessionId)
        }
    }

    override suspend fun cancelActiveTask(sessionId: String): Result<Unit> = runCatching {
        if (Session.isValidSessionId(sessionId)) {
            apiService.cancelSession(sessionId)
            coordinator.notifyCancelled(sessionId)
        }
    }

    override fun toggleArchiveLocal(sessionId: String) {
        if (Session.isValidSessionId(sessionId)) {
            coordinator.toggleArchive(sessionId)
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        if (Session.isValidSessionId(sessionId)) {
            coordinator.removeSession(sessionId)
            runCatching { apiService.deleteSession(sessionId) }
        }
        Unit
    }

    private fun parseIsoOrEpoch(isoString: String?, epochMs: Long?): Long {
        if (epochMs != null && epochMs > 0) return epochMs
        if (!isoString.isNullOrBlank()) {
            try {
                return Instant.parse(isoString).toEpochMilli()
            } catch (_: Exception) {
                try {
                    return OffsetDateTime.parse(isoString).toInstant().toEpochMilli()
                } catch (_: Exception) {}
            }
        }
        return System.currentTimeMillis()
    }

    private fun SessionDto.toRuntimeState(): SessionRuntimeState {
        val created = parseIsoOrEpoch(createdAt, createdAtEpochMs)
        val updated = parseIsoOrEpoch(updatedAt, updatedAtEpochMs ?: createdAtEpochMs)
        val sid = sessionId
        val session = Session(
            sessionId = sid,
            title = title?.takeIf { it.isNotBlank() } ?: "Session ${sid.take(8)}",
            status = status.toSessionStatus(),
            isArchived = archived,
            createdAtEpochMs = created,
            updatedAtEpochMs = updated,
            activeModel = mode.takeIf { it.isNotBlank() } ?: activeModel
        )
        val initialMessages = messages.map { it.toDomain() }.toMutableList()
        if (initialMessages.isEmpty() && !lastPrompt.isNullOrBlank()) {
            initialMessages.add(
                ChatMessage.User(
                    id = "last-prompt-$sid",
                    timestampEpochMs = created,
                    content = lastPrompt,
                    modelOverride = mode
                )
            )
        }
        return SessionRuntimeState(
            session = session,
            messages = initialMessages
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
