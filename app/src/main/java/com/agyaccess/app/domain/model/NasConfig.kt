package com.agyaccess.app.domain.model

import java.net.URI

/**
 * Configuration sécurisée de connexion au NAS hébergeant l'agent agy.
 * Stockée chiffrée via Android Keystore (AES-256-GCM).
 */
data class NasConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val apiToken: String = "",
    /** Empreintes publiques TLS au format `sha256/BASE64...` ou hexadécimal (Certificate Pinning). */
    val certificatePins: List<String> = listOf(DEFAULT_PIN),
    /** En-tête optionnel d'authentification mutuelle / reverse-proxy (ex: X-Client-Cert-Token). */
    val mutualAuthHeaderName: String = "",
    val mutualAuthHeaderValue: String = "",
    val requireBiometricOnLaunch: Boolean = false,
    val requireBiometricForSensitivePrompts: Boolean = false
) {
    val isConfigured: Boolean
        get() = isValidHttpsUrl(effectiveBaseUrl())

    fun effectiveBaseUrl(): String =
        if (isValidHttpsUrl(baseUrl)) baseUrl.trim() else DEFAULT_BASE_URL

    fun normalizedBaseUrl(): String {
        val trimmed = effectiveBaseUrl().trimEnd('/')
        return "$trimmed/"
    }

    fun extractHost(): String? = runCatching {
        URI(effectiveBaseUrl()).host
    }.getOrNull()

    companion object {
        const val DEFAULT_BASE_URL = "https://100.101.127.39:8844"
        const val DEFAULT_PIN = "sha256/kz0TBpW+fCDYd/Oou8RhYQnI8hqrw41D4HRqQSc1THQ="
        const val DEFAULT_CERT_HEX = "30:87:c0:2c:19:b7:1d:8f:31:18:52:0c:9a:06:4c:d5:04:b6:61:24:1b:91:8c:3b:e3:60:c3:b9:03:f8:a7:fb"

        private val SHA256_BASE64_PIN_REGEX = Regex("^(sha256/)?[A-Za-z0-9+/=]{43,44}$")
        private val SHA256_HEX_PIN_REGEX = Regex("^(([0-9a-fA-F]{2}:){31}[0-9a-fA-F]{2}|[0-9a-fA-F]{64})$")

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

        fun isValidPin(pin: String): Boolean {
            val trimmed = pin.trim()
            return SHA256_BASE64_PIN_REGEX.matches(trimmed) || SHA256_HEX_PIN_REGEX.matches(trimmed)
        }

        /**
         * Normalise une empreinte base64 au format requis par OkHttp CertificatePinner :
         * `sha256/BASE64...`
         */
        fun normalizeToOkHttpPin(pin: String): String? {
            val trimmed = pin.trim()
            if (trimmed.startsWith("sha256/")) return trimmed
            if (Regex("^[A-Za-z0-9+/=]{43,44}$").matches(trimmed)) return "sha256/$trimmed"
            return null
        }
    }
}
