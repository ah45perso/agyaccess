package com.agyaccess.app.data.remote.sse

import com.agyaccess.app.data.remote.network.SecureOkHttpProvider
import com.agyaccess.app.domain.model.Session
import com.agyaccess.app.domain.model.StreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Client Server-Sent Events (SSE) résilient pour l'endpoint
 * `GET /api/v1/sessions/{session_id}/stream`.
 *
 * - S'appuie sur `okhttp-sse` avec le client TLS/Pinning fourni par [SecureOkHttpProvider].
 * - Supporte la reconnexion automatique avec backoff exponentiel et reprise par `Last-Event-ID`.
 * - Ferme immédiatement la connexion réseau dès annulation de la coroutine ou réception de `done`.
 */
@Singleton
class AgySseClient @Inject constructor(
    private val okHttpProvider: SecureOkHttpProvider,
    private val parser: AgySseEventParser
) {
    sealed interface SseSignal {
        data object Connected : SseSignal
        data class Event(val event: StreamEvent) : SseSignal
        data class Reconnecting(val attempt: Int, val reason: String) : SseSignal
        data object Terminated : SseSignal
    }

    /**
     * Ouvre un flux SSE persistant avec reconnexion gracieuse jusqu'à réception
     * d'un événement [StreamEvent.Done], d'une erreur fatale, ou d'une annulation.
     */
    fun streamSession(
        sessionId: String,
        onStreamOpened: (() -> Unit)? = null
    ): Flow<SseSignal> = flow {
        require(Session.isValidSessionId(sessionId)) {
            "Identifiant UUID de session invalide."
        }

        var lastEventId: String? = null
        var attempt = 0
        var shouldContinue = true
        var notifiedOpen = false

        while (shouldContinue) {
            var completedNormally = false
            var fatalError = false
            var failureReason: String? = null

            openSingleConnection(sessionId, lastEventId).collect { signal ->
                when (signal) {
                    is SseSignal.Connected -> {
                        attempt = 0
                        if (!notifiedOpen) {
                            notifiedOpen = true
                            onStreamOpened?.invoke()
                        }
                        emit(signal)
                    }

                    is SseSignal.Event -> {
                        signal.event.eventId?.let { lastEventId = it }
                        emit(signal)
                        when (signal.event) {
                            is StreamEvent.Done -> completedNormally = true
                            is StreamEvent.Error -> {
                                if (!signal.event.recoverable) fatalError = true
                            }
                            else -> Unit
                        }
                    }

                    is SseSignal.Reconnecting -> {
                        failureReason = signal.reason
                    }

                    is SseSignal.Terminated -> {
                        completedNormally = true
                    }
                }
            }

            if (completedNormally || fatalError) {
                shouldContinue = false
                emit(SseSignal.Terminated)
            } else {
                attempt += 1
                if (attempt > MAX_RECONNECT_ATTEMPTS) {
                    emit(
                        SseSignal.Event(
                            StreamEvent.Error(
                                eventId = lastEventId,
                                code = "SSE_RECONNECT_EXHAUSTED",
                                message = failureReason
                                    ?: "Connexion interrompue après $MAX_RECONNECT_ATTEMPTS tentatives.",
                                recoverable = false
                            )
                        )
                    )
                    shouldContinue = false
                } else {
                    val backoffMs = min(1000L * (1L shl (attempt - 1)), MAX_BACKOFF_MS)
                    emit(
                        SseSignal.Reconnecting(
                            attempt = attempt,
                            reason = failureReason ?: "Perte de connexion au NAS"
                        )
                    )
                    delay(backoffMs)
                }
            }
        }
    }

    private fun openSingleConnection(
        sessionId: String,
        lastEventId: String?
    ): Flow<SseSignal> = callbackFlow {
        val client = okHttpProvider.getSseClient()
        val requestBuilder = Request.Builder()
            // L'hôte placeholder est réécrit en HTTPS par DynamicBaseUrlInterceptor
            .url("https://placeholder.invalid/api/v1/sessions/$sessionId/stream")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .get()

        if (!lastEventId.isNullOrBlank()) {
            requestBuilder.header("Last-Event-ID", lastEventId)
        }

        val factory = EventSources.createFactory(client)
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySend(SseSignal.Connected)
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                val parsed = parser.parse(id, type, data) ?: return
                trySend(SseSignal.Event(parsed))
                if (parsed is StreamEvent.Done || (parsed is StreamEvent.Error && !parsed.recoverable)) {
                    eventSource.cancel()
                    close()
                }
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(SseSignal.Terminated)
                close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                if (t is CancellationException) {
                    close()
                    return
                }
                val httpCode = response?.code
                val isAuthOrClientError = httpCode != null && httpCode in 400..499 && httpCode != 408 && httpCode != 429
                if (isAuthOrClientError) {
                    trySend(
                        SseSignal.Event(
                            StreamEvent.Error(
                                eventId = lastEventId,
                                code = "HTTP_$httpCode",
                                message = "Rejet du serveur NAS (HTTP $httpCode). Vérifiez votre clé API ou session.",
                                recoverable = false
                            )
                        )
                    )
                    close()
                    return
                }
                val reason = when {
                    httpCode != null -> "Erreur HTTP $httpCode sur le flux SSE"
                    t is IOException -> t.localizedMessage ?: "Déconnexion réseau"
                    else -> t?.localizedMessage ?: "Erreur de flux inattendue"
                }
                trySend(SseSignal.Reconnecting(attempt = 1, reason = reason))
                close()
            }
        }

        val eventSource = factory.newEventSource(requestBuilder.build(), listener)

        awaitClose {
            eventSource.cancel()
        }
    }

    /**
     * Helper utile pour attendre que la connexion SSE soit établie (avec timeout externe)
     * avant de déclencher l'envoi du prompt.
     */
    fun createReadinessSignal(): CompletableDeferred<Unit> = CompletableDeferred()

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val MAX_BACKOFF_MS = 16_000L
    }
}
