package com.agyaccess.app.data.remote.api

import com.agyaccess.app.data.remote.dto.CancelResponseDto
import com.agyaccess.app.data.remote.dto.CreateSessionRequestDto
import com.agyaccess.app.data.remote.dto.CreateSessionResponseDto
import com.agyaccess.app.data.remote.dto.DeleteSessionResponseDto
import com.agyaccess.app.data.remote.dto.HealthResponseDto
import com.agyaccess.app.data.remote.dto.PromptAcceptedResponseDto
import com.agyaccess.app.data.remote.dto.PromptRequestDto
import com.agyaccess.app.data.remote.dto.SessionDto
import com.agyaccess.app.data.remote.dto.SessionsListResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Contrat REST strict pour le backend agy hébergé sur le serveur / Tailscale.
 *
 * Note : le flux temps réel (`GET /api/v1/sessions/{session_id}/stream`) est consommé
 * directement en Server-Sent Events (SSE) par [com.agyaccess.app.data.remote.sse.AgySseClient].
 */
interface AgyApiService {

    /** 0. Healthcheck du serveur. */
    @GET("health")
    suspend fun checkHealth(): HealthResponseDto

    /** 1. Liste les sessions actives et leurs métadonnées. */
    @GET("api/v1/sessions")
    suspend fun listSessions(): SessionsListResponseDto

    /** 2. Crée une nouvelle session de chat (retourne un `session_id` UUID). */
    @POST("api/v1/sessions")
    suspend fun createSession(
        @Body body: CreateSessionRequestDto = CreateSessionRequestDto()
    ): CreateSessionResponseDto

    /** 3. Récupère le détail d'une session. */
    @GET("api/v1/sessions/{session_id}")
    suspend fun getSession(
        @Path("session_id") sessionId: String
    ): SessionDto

    /** 4. Envoie un prompt UTF-8 strictement typé JSON sur une session donnée. */
    @POST("api/v1/sessions/{session_id}/prompt")
    suspend fun sendPrompt(
        @Path("session_id") sessionId: String,
        @Body body: PromptRequestDto
    ): PromptAcceptedResponseDto

    /** 5. Interrompt la tâche en cours sur l'agent agy pour cette session. */
    @POST("api/v1/sessions/{session_id}/cancel")
    suspend fun cancelSession(
        @Path("session_id") sessionId: String
    ): CancelResponseDto

    /** 6. Supprime définitivement une session du NAS. */
    @DELETE("api/v1/sessions/{session_id}")
    suspend fun deleteSession(
        @Path("session_id") sessionId: String
    ): DeleteSessionResponseDto
}
