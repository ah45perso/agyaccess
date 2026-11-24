package com.agyaccess.app.domain.model

import java.net.URI

/**
 * Configuration sécurisée de connexion au NAS hébergeant l'agent agy.
 * Stockée chiffrée via Android Keystore (AES-256-GCM).
 */
data class NasConfig(
    val baseUrl: String = "",
    val apiToken: String = "",
    /** Empreintes publiques TLS au format `sha256/BASE64...` (Certificate Pinning). */
    val certificatePins: List<String> = emptyList(),
    /** En-tête optionnel d'authentification mutuelle / reverse-proxy (ex: X-Client-Cert-Token). */
    val mutualAuthHeaderName: String = "",
    val mutualAuthHeaderValue: String = "",
    val requireBiometricOnLaunch: Boolean = true,
    val requireBiometricForSensitivePrompts: Boolean = true
) {
    val isConfigured: Boolean
        get() = isValidHttpsUrl(baseUrl) && apiToken.isNotBlank()

    fun normalizedBaseUrl(): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return "$trimmed/"
    }

    fun extractHost(): String? = runCatching {
        URI(baseUrl.trim()).host
    }.getOrNull()

    companion object {
        private val SHA256_PIN_REGEX = Regex("^sha256/[A-Za-z0-9+/=]{43,44}$")

        /**
         * Impose strictement HTTPS et interdit toute URL malformée, identifiants
         * embarqués (`user:pass@host`) ou trafic en clair (`http://`).
         */
        fun isValidHttpsUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return false
            return runCatching {
                val uri = URI(trimmed)
                uri.scheme.equals("https", ignoreCase = true) &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null &&
                    uri.query == null &&
                    uri.fragment == null
            }.getOrDefault(false)
        }

        fun isValidPin(pin: String): Boolean = SHA256_PIN_REGEX.matches(pin.trim())
    }
}
