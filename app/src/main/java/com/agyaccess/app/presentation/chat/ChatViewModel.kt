package com.agyaccess.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agyaccess.app.domain.model.NasConfig
import com.agyaccess.app.domain.repository.AgyRepository
import com.agyaccess.app.domain.repository.SecurityConfigRepository
import com.agyaccess.app.domain.usecase.CancelSessionUseCase
import com.agyaccess.app.domain.usecase.CreateSessionUseCase
import com.agyaccess.app.domain.usecase.ObserveSessionsRuntimeUseCase
import com.agyaccess.app.domain.usecase.RefreshSessionsUseCase
import com.agyaccess.app.domain.usecase.SendPromptUseCase
import com.agyaccess.app.domain.usecase.ValidatePromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agyRepository: AgyRepository,
    private val securityConfigRepository: SecurityConfigRepository,
    observeSessionsRuntime: ObserveSessionsRuntimeUseCase,
    private val refreshSessionsUseCase: RefreshSessionsUseCase,
    private val createSessionUseCase: CreateSessionUseCase,
    private val sendPromptUseCase: SendPromptUseCase,
    private val cancelSessionUseCase: CancelSessionUseCase,
    private val validatePromptUseCase: ValidatePromptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects: Flow<ChatEffect> = _effects.receiveAsFlow()

    private var initialLockEvaluated = false

    init {
        viewModelScope.launch {
            combine(
                observeSessionsRuntime(),
                securityConfigRepository.nasConfigFlow
            ) { runtimeMap, nasConfig ->
                runtimeMap to nasConfig
            }.collect { (runtimeMap, nasConfig) ->
                val sessionsList = runtimeMap.values.toList()
                val shouldLockOnFirstLoad = !initialLockEvaluated &&
                    nasConfig.isConfigured &&
                    nasConfig.requireBiometricOnLaunch

                if (!initialLockEvaluated) {
                    initialLockEvaluated = true
                    if (shouldLockOnFirstLoad) {
                        _effects.trySend(ChatEffect.TriggerAppUnlockBiometric)
                    } else if (nasConfig.isConfigured) {
                        refreshSessions()
                    }
                }

                _uiState.update { current ->
                    val resolvedSelectedId = current.selectedSessionId
                        ?.takeIf { runtimeMap.containsKey(it) }
                        ?: sessionsList
                            .filter { !it.session.isArchived }
                            .maxByOrNull { it.session.updatedAtEpochMs }
                            ?.session?.sessionId

                    current.copy(
                        nasConfig = nasConfig,
                        sessions = sessionsList,
                        selectedSessionId = resolvedSelectedId,
                        isAppLocked = if (shouldLockOnFirstLoad) true else current.isAppLocked,
                        isSettingsOpen = current.isSettingsOpen || (!nasConfig.isConfigured && !shouldLockOnFirstLoad)
                    )
                }
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.RefreshSessions -> refreshSessions()
            is ChatIntent.CreateNewSession -> createNewSession()
            is ChatIntent.SelectSession -> selectSession(intent.sessionId)
            is ChatIntent.ToggleArchiveSession -> agyRepository.toggleArchiveLocal(intent.sessionId)
            is ChatIntent.UpdateDraft -> updateDraft(intent.text)
            is ChatIntent.SelectModelOverride -> {
                _uiState.update { it.copy(selectedModelOverride = intent.model) }
            }
            is ChatIntent.SubmitPrompt -> submitCurrentPrompt()
            is ChatIntent.ConfirmSensitivePrompt -> dispatchValidatedPrompt(
                sessionId = intent.pending.sessionId,
                prompt = intent.pending.prompt,
                modelOverride = intent.pending.modelOverride
            )
            is ChatIntent.CancelActiveSession -> cancelCurrentSession()
            is ChatIntent.OpenSettings -> _uiState.update { it.copy(isSettingsOpen = true) }
            is ChatIntent.CloseSettings -> _uiState.update { it.copy(isSettingsOpen = false) }
            is ChatIntent.SaveSettings -> saveSettings(intent.config)
            is ChatIntent.ClearSecrets -> clearSecrets()
            is ChatIntent.RequestAppUnlock -> _effects.trySend(ChatEffect.TriggerAppUnlockBiometric)
            is ChatIntent.OnAppUnlocked -> {
                _uiState.update { it.copy(isAppLocked = false) }
                if (_uiState.value.nasConfig.isConfigured) {
                    refreshSessions()
                }
            }
            is ChatIntent.LockAppNow -> _uiState.update { it.copy(isAppLocked = true) }
            is ChatIntent.DismissError -> _uiState.update { it.copy(errorBanner = null) }
        }
    }

    private fun refreshSessions() {
        if (!_uiState.value.nasConfig.isConfigured) {
            _uiState.update { it.copy(isSettingsOpen = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingSessions = true, errorBanner = null) }
            refreshSessionsUseCase()
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorBanner = error.localizedMessage
                                ?: "Impossible de joindre le NAS."
                        )
                    }
                }
            _uiState.update { it.copy(isRefreshingSessions = false) }
        }
    }

    private fun createNewSession() {
        if (!_uiState.value.nasConfig.isConfigured) {
            _uiState.update { it.copy(isSettingsOpen = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true, errorBanner = null) }
            createSessionUseCase()
                .onSuccess { session ->
                    agyRepository.markSessionRead(session.sessionId)
                    _uiState.update {
                        it.copy(
                            selectedSessionId = session.sessionId,
                            isCreatingSession = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingSession = false,
                            errorBanner = error.localizedMessage
                                ?: "Échec de création de session."
                        )
                    }
                }
        }
    }

    private fun selectSession(sessionId: String) {
        agyRepository.markSessionRead(sessionId)
        _uiState.update { it.copy(selectedSessionId = sessionId) }
    }

    private fun updateDraft(text: String) {
        val key = _uiState.value.selectedSessionId.orEmpty()
        _uiState.update { state ->
            state.copy(promptDrafts = state.promptDrafts + (key to text))
        }
    }

    private fun submitCurrentPrompt() {
        val state = _uiState.value
        if (!state.nasConfig.isConfigured) {
            _uiState.update { it.copy(isSettingsOpen = true) }
            return
        }

        val validation = validatePromptUseCase(
            rawPrompt = state.currentDraft,
            rawModelOverride = state.selectedModelOverride
        )

        when (validation) {
            is ValidatePromptUseCase.ValidationResult.Invalid -> {
                _effects.trySend(ChatEffect.ShowSnackbar(validation.reason))
            }

            is ValidatePromptUseCase.ValidationResult.Valid -> {
                viewModelScope.launch {
                    val targetSessionId = state.selectedSessionId ?: run {
                        val created = createSessionUseCase().getOrElse { err ->
                            _uiState.update {
                                it.copy(errorBanner = err.localizedMessage ?: "Création impossible.")
                            }
                            return@launch
                        }
                        agyRepository.markSessionRead(created.sessionId)
                        _uiState.update { it.copy(selectedSessionId = created.sessionId) }
                        created.sessionId
                    }

                    val validated = validation.prompt
                    if (validated.requiresSensitiveConfirmation &&
                        state.nasConfig.requireBiometricForSensitivePrompts
                    ) {
                        _effects.trySend(
                            ChatEffect.TriggerSensitivePromptBiometric(
                                PendingSensitivePrompt(
                                    sessionId = targetSessionId,
                                    prompt = validated.text,
                                    modelOverride = validated.modelOverride
                                )
                            )
                        )
                    } else {
                        dispatchValidatedPrompt(
                            sessionId = targetSessionId,
                            prompt = validated.text,
                            modelOverride = validated.modelOverride
                        )
                    }
                }
            }
        }
    }

    private fun dispatchValidatedPrompt(
        sessionId: String,
        prompt: String,
        modelOverride: String?
    ) {
        // Efface le brouillon de cette session immédiatement afin de permettre un basculement fluide
        _uiState.update { state ->
            state.copy(promptDrafts = state.promptDrafts - sessionId - "")
        }

        viewModelScope.launch {
            sendPromptUseCase(
                sessionId = sessionId,
                prompt = prompt,
                modelOverride = modelOverride
            ).onFailure { error ->
                _effects.trySend(
                    ChatEffect.ShowSnackbar(
                        error.localizedMessage ?: "Erreur lors de l'envoi du prompt."
                    )
                )
            }
        }
    }

    private fun cancelCurrentSession() {
        val sessionId = _uiState.value.selectedSessionId ?: return
        viewModelScope.launch {
            cancelSessionUseCase(sessionId)
                .onSuccess {
                    _effects.trySend(ChatEffect.ShowSnackbar("Exécution interrompue."))
                }
                .onFailure { error ->
                    _effects.trySend(
                        ChatEffect.ShowSnackbar(
                            "Flux local arrêté (NAS : ${error.localizedMessage ?: "injoignable"})."
                        )
                    )
                }
        }
    }

    private fun saveSettings(config: NasConfig) {
        viewModelScope.launch {
            securityConfigRepository.saveNasConfig(config)
                .onSuccess {
                    _uiState.update { it.copy(isSettingsOpen = false, errorBanner = null) }
                    _effects.trySend(ChatEffect.ShowSnackbar("Configuration chiffrée dans Android Keystore."))
                    if (config.isConfigured) {
                        refreshSessions()
                    }
                }
                .onFailure { error ->
                    _effects.trySend(
                        ChatEffect.ShowSnackbar(
                            error.localizedMessage ?: "Configuration invalide."
                        )
                    )
                }
        }
    }

    private fun clearSecrets() {
        viewModelScope.launch {
            securityConfigRepository.clearSecrets()
            _uiState.update { it.copy(isSettingsOpen = true) }
            _effects.trySend(ChatEffect.ShowSnackbar("Secrets effacés du stockage sécurisé."))
        }
    }
}
