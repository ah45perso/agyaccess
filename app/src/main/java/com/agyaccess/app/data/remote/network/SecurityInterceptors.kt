package com.agyaccess.app.data.remote.network

import com.agyaccess.app.data.security.SecureConfigStorage
import com.agyaccess.app.domain.model.NasConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Réécrit dynamiquement l'hôte cible de Retrofit avec l'URL HTTPS stockée
 * dans le Keystore matériel. Refuse systématiquement tout trafic non-HTTPS.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val configStorage: SecureConfigStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val config = configStorage.currentConfig.value

        if (!NasConfig.isValidHttpsUrl(config.baseUrl)) {
            throw IOException(
                "URL du NAS non configurée ou non sécurisée (HTTPS obligatoire)."
            )
        }

        val targetBaseUrl = config.normalizedBaseUrl().toHttpUrlOrNull()
            ?: throw IOException("URL du NAS invalide.")

        if (!targetBaseUrl.isHttps) {
            throw IOException("Blocage sécurité : le trafic HTTP en clair est interdit.")
        }

        val mergedPathSegments = buildList {
            addAll(targetBaseUrl.pathSegments.filter { it.isNotEmpty() })
            addAll(originalRequest.url.pathSegments.filter { it.isNotEmpty() })
        }

        val rebuiltUrlBuilder = originalRequest.url.newBuilder()
            .scheme("https")
            .host(targetBaseUrl.host)
            .port(targetBaseUrl.port)
            .encodedPath("/")

        mergedPathSegments.forEach { segment ->
            rebuiltUrlBuilder.addPathSegment(segment)
        }

        val updatedRequest = originalRequest.newBuilder()
            .url(rebuiltUrlBuilder.build())
            .build()

        return chain.proceed(updatedRequest)
    }
}

/**
 * Injecte de façon transparente les en-têtes d'authentification (JWT / API Key +
 * éventuel en-tête d'authentification mutuelle) sans jamais exposer les secrets dans les logs.
 */
@Singleton
class AuthHeaderInterceptor @Inject constructor(
    private val configStorage: SecureConfigStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val config = configStorage.currentConfig.value
        val builder = chain.request().newBuilder()
            .header("X-Client-App", "AgyAccess-Android")

        if (config.apiToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.apiToken}")
        }

        if (config.mutualAuthHeaderName.isNotBlank() && config.mutualAuthHeaderValue.isNotBlank()) {
            val safeHeaderName = config.mutualAuthHeaderName.trim()
            if (HEADER_NAME_REGEX.matches(safeHeaderName)) {
                builder.header(safeHeaderName, config.mutualAuthHeaderValue.trim())
            }
        }

        return chain.proceed(builder.build())
    }

    companion object {
        private val HEADER_NAME_REGEX = Regex("^[A-Za-z0-9-]+$")
    }
}
