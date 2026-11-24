package com.agyaccess.app.presentation.chat

import com.agyaccess.app.domain.model.NasConfig
import com.agyaccess.app.domain.model.SessionRuntimeState

/**
 * Contrat MVI (State / Intent / One-shot Effect) de l'écran principal AgyAccess.
 */
data class PendingSensitivePrompt(
    val sessionId: String,
    val prompt: String,
    val modelOverride: String?
)

data class ChatUiState(
    val nasConfig: NasConfig = NasConfig(),
    val sessions: List<SessionRuntimeState> = emptyList(),
    val selectedSessionId: String? = null,
    val promptDrafts: Map<String, String> = emptyMap(),
    val selectedModelOverride: String? = null,
    val isRefreshingSessions: Boolean = false,
    val isCreatingSession: Boolean = false,
    val isAppLocked: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val errorBanner: String? = null
) {
    val activeSession: SessionRuntimeState?
        get() = sessions.firstOrNull { it.session.sessionId == selectedSessionId }

    val activeSessions: List<SessionRuntimeState>
        get() = sessions
            .filter { !it.session.isArchived }
            .sortedByDescending { it.session.updatedAtEpochMs }

    val archivedSessions: List<SessionRuntimeState>
        get() = sessions
            .filter { it.session.isArchived }
            .sortedByDescending { it.session.updatedAtEpochMs }

    val currentDraft: String
        get() = promptDrafts[selectedSessionId.orEmpty()].orEmpty()

    val runningBackgroundCount: Int
        get() = sessions.count {
            it.session.sessionId != selectedSessionId && it.isRunning
        }
}

sealed interface ChatIntent {
    data object RefreshSessions : ChatIntent
    data object CreateNewSession : ChatIntent
    data class SelectSession(val sessionId: String) : ChatIntent
    data class ToggleArchiveSession(val sessionId: String) : ChatIntent
    data class DeleteSession(val sessionId: String) : ChatIntent
    data class UpdateDraft(val text: String) : ChatIntent
    data class SelectModelOverride(val model: String?) : ChatIntent
    data object SubmitPrompt : ChatIntent
    data class ConfirmSensitivePrompt(val pending: PendingSensitivePrompt) : ChatIntent
    data object CancelActiveSession : ChatIntent
    data object OpenSettings : ChatIntent
    data object CloseSettings : ChatIntent
    data class SaveSettings(val config: NasConfig) : ChatIntent
    data object ClearSecrets : ChatIntent
    data object RequestAppUnlock : ChatIntent
    data object OnAppUnlocked : ChatIntent
    data object LockAppNow : ChatIntent
    data object DismissError : ChatIntent
}

sealed interface ChatEffect {
    data class ShowSnackbar(val message: String) : ChatEffect
    data object TriggerAppUnlockBiometric : ChatEffect
    data class TriggerSensitivePromptBiometric(val pending: PendingSensitivePrompt) : ChatEffect
}
