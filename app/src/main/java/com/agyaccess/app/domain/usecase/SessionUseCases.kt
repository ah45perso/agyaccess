package com.agyaccess.app.domain.usecase

import com.agyaccess.app.domain.model.Session
import com.agyaccess.app.domain.model.SessionRuntimeState
import com.agyaccess.app.domain.repository.AgyRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSessionsRuntimeUseCase @Inject constructor(
    private val repository: AgyRepository
) {
    operator fun invoke(): StateFlow<Map<String, SessionRuntimeState>> =
        repository.sessionsRuntime
}

class RefreshSessionsUseCase @Inject constructor(
    private val repository: AgyRepository
) {
    suspend operator fun invoke(): Result<List<Session>> =
        repository.refreshSessions()
}

class CreateSessionUseCase @Inject constructor(
    private val repository: AgyRepository
) {
    suspend operator fun invoke(title: String? = null): Result<Session> =
        repository.createSession(title)
}

class SendPromptUseCase @Inject constructor(
    private val repository: AgyRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        prompt: String,
        modelOverride: String? = null
    ): Result<Unit> {
        if (!Session.isValidSessionId(sessionId)) {
            return Result.failure(IllegalArgumentException("Identifiant de session UUID invalide."))
        }
        return repository.sendPrompt(sessionId, prompt, modelOverride)
    }
}

class CancelSessionUseCase @Inject constructor(
    private val repository: AgyRepository
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        if (!Session.isValidSessionId(sessionId)) {
            return Result.failure(IllegalArgumentException("Identifiant de session UUID invalide."))
        }
        return repository.cancelSession(sessionId)
    }
}
