package com.agyaccess.app.domain.repository

import com.agyaccess.app.domain.model.Session
import com.agyaccess.app.domain.model.SessionRuntimeState
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrat du dépôt gérant les sessions concurrentes avec l'agent agy.
 */
interface AgyRepository {

    /**
     * Flux réactif de toutes les sessions connues et de leur état d'exécution
     * temps réel (messages, streaming en cours, statuts d'outils, reconnexion).
     */
    val sessionsRuntime: StateFlow<Map<String, SessionRuntimeState>>

    /** Récupère et synchronise la liste des sessions actives/archivées depuis le NAS. */
    suspend fun refreshSessions(): Result<List<Session>>

    /** Crée une nouvelle session distante et initialise son état local. */
    suspend fun createSession(title: String? = null): Result<Session>

    /**
     * Envoie un prompt validé sur [sessionId] et lance (ou réutilise) son flux SSE
     * en tâche de fond sans bloquer les autres sessions.
     */
    suspend fun sendPrompt(
        sessionId: String,
        prompt: String,
        modelOverride: String? = null
    ): Result<Unit>

    /** Attache l'écoute SSE sur une session déjà en cours d'exécution. */
    fun attachStreamIfRunning(sessionId: String)

    /** Interrompt la tâche en cours sur le NAS et stoppe le flux SSE local. */
    suspend fun cancelSession(sessionId: String): Result<Unit>

    /** Marque une session comme consultée (efface le badge de mise à jour en arrière-plan). */
    fun markSessionRead(sessionId: String)

    /** Archive ou désarchive localement une session dans le tiroir. */
    fun toggleArchiveLocal(sessionId: String)

    /** Supprime définitivement une session du NAS et de l'état local. */
    suspend fun deleteSession(sessionId: String): Result<Unit>
}
