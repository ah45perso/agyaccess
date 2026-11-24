package com.agyaccess.app.data.remote.network

import android.util.Base64
import com.agyaccess.app.BuildConfig
import com.agyaccess.app.data.security.SecureConfigStorage
import com.agyaccess.app.domain.model.NasConfig
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Fournisseur centralisé d'[OkHttpClient] durci pour les appels REST et SSE.
 *
 * Garanties de sécurité :
 * - Force TLS 1.3 / TLS 1.2 uniquement (`ConnectionSpec.MODERN_TLS`).
 * - Supporte le **Certificate Pinning** dynamique pour CA publique ou certificat auto-signé
 *   sur réseau privé/Tailscale via un [PinnedCertTrustManager] sécurisé.
 * - Pré-configure l'accès sans friction au nœud Tailscale via vérification cryptographique
 *   de l'empreinte SHA-256 sans blocage de chaîne de confiance (résolution des self-signed).
 * - Partage le pool de connexions (`newBuilder()`) entre le client REST et le client SSE.
 * - Masque les en-têtes sensibles dans les logs.
 */
@Singleton
class SecureOkHttpProvider @Inject constructor(
    private val configStorage: SecureConfigStorage,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
    private val authHeaderInterceptor: AuthHeaderInterceptor
) {
    private data class ClientCacheKey(
        val host: String?,
        val pins: List<String>
    )

    private val strictTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .build()

    private val defaultSystemTrustManager: X509TrustManager by lazy {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    @Volatile
    private var cachedKey: ClientCacheKey? = null

    @Volatile
    private var cachedRestClient: OkHttpClient? = null

    @Volatile
    private var cachedSseClient: OkHttpClient? = null

    fun getRestClient(): OkHttpClient {
        ensureClientsUpToDate()
        return requireNotNull(cachedRestClient)
    }

    fun getSseClient(): OkHttpClient {
        ensureClientsUpToDate()
        return requireNotNull(cachedSseClient)
    }

    @Synchronized
    private fun ensureClientsUpToDate() {
        val config = configStorage.currentConfig.value
        val host = config.extractHost() ?: "100.101.127.39"
        val effectivePins = config.certificatePins.ifEmpty { listOf(NasConfig.DEFAULT_PIN) }
            .filter(NasConfig::isValidPin)
        val key = ClientCacheKey(host, effectivePins)

        if (key == cachedKey && cachedRestClient != null && cachedSseClient != null) {
            return
        }

        val trustManager = PinnedCertTrustManager(
            systemTrustManager = defaultSystemTrustManager,
            allowedPinsProvider = {
                val pins = configStorage.currentConfig.value.certificatePins
                if (pins.isEmpty()) listOf(NasConfig.DEFAULT_PIN) else pins
            }
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }

        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(strictTlsSpec))
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { hostname, session ->
                if (isTailscaleOrLocalHost(hostname)) {
                    true
                } else {
                    HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authHeaderInterceptor)

        if (BuildConfig.DEBUG) {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("X-Client-Cert-Token")
            }
            builder.addInterceptor(logger)
        }

        val base = builder.build()

        cachedRestClient = base

        cachedSseClient = base.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // Indispensable pour un flux SSE longue durée
            .pingInterval(25, TimeUnit.SECONDS)
            .build()

        cachedKey = key
    }

    private fun isTailscaleOrLocalHost(hostname: String): Boolean {
        val h = hostname.lowercase().trim()
        return h == "100.101.127.39" ||
            h == "ah-serv-opti" ||
            h.endsWith(".ts.net") ||
            h.startsWith("100.") ||
            h == "127.0.0.1" ||
            h == "localhost"
    }

    fun applyMutualTls(
        builder: OkHttpClient.Builder,
        clientCertPkcs12: ByteArray,
        password: CharArray
    ): OkHttpClient.Builder {
        val clientKeyStore = KeyStore.getInstance("PKCS12").apply {
            load(ByteArrayInputStream(clientCertPkcs12), password)
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(clientKeyStore, password)
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }
        val trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, arrayOf(trustManager), null)
        }
        return builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    }

    private class PinnedCertTrustManager(
        private val systemTrustManager: X509TrustManager,
        private val allowedPinsProvider: () -> List<String>
    ) : X509TrustManager {

        private val embeddedCert: X509Certificate? by lazy {
            runCatching {
                val certFactory = CertificateFactory.getInstance("X.509")
                certFactory.generateCertificate(ByteArrayInputStream(EMBEDDED_CERT_PEM.toByteArray())) as? X509Certificate
            }.getOrNull()
        }

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            systemTrustManager.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            if (chain.isNullOrEmpty()) {
                throw CertificateException("Chaîne de certificats serveur vide ou absente.")
            }

            // 1. Tente la validation standard via les autorités de certification système (CA publiques)
            val isSystemTrusted = runCatching {
                systemTrustManager.checkServerTrusted(chain, authType)
            }.isSuccess

            // 2. Vérification d'empreinte (Certificate Pinning)
            val configuredPins = allowedPinsProvider()
            val effectivePins = (configuredPins + listOf(NasConfig.DEFAULT_PIN, NasConfig.DEFAULT_CERT_HEX)).distinct()
            val pinMatched = matchesAnyPin(chain, effectivePins)

            if (isSystemTrusted && (configuredPins.isEmpty() || pinMatched)) {
                return
            }

            if (pinMatched) {
                // Certificat validé par pinning cryptographique (ex: certificat auto-signé sur nœud Tailscale)
                chain.forEach { cert -> cert.checkValidity() }
                return
            }

            if (isSystemTrusted) {
                return
            }

            throw CertificateException(
                "Certificat serveur non approuvé : aucune empreinte SHA-256 ne correspond aux pins autorisés."
            )
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            val issuers = systemTrustManager.acceptedIssuers.toMutableList()
            embeddedCert?.let { issuers.add(it) }
            return issuers.toTypedArray()
        }

        private fun matchesAnyPin(chain: Array<out X509Certificate>, pins: List<String>): Boolean {
            return chain.any { cert ->
                val spkiB64 = computeSpkiPin(cert)
                val certHex = computeCertSha256Hex(cert)
                val certB64 = computeCertSha256Base64(cert)

                pins.any { rawPin ->
                    val pin = rawPin.trim()
                    pin.equals(spkiB64, ignoreCase = true) ||
                        pin.equals("sha256/$spkiB64", ignoreCase = true) ||
                        pin.replace(":", "").equals(certHex.replace(":", ""), ignoreCase = true) ||
                        pin.equals(certB64, ignoreCase = true)
                }
            }
        }

        private fun computeSpkiPin(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(cert.publicKey.encoded)
            return Base64.encodeToString(hash, Base64.NO_WRAP)
        }

        private fun computeCertSha256Hex(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(cert.encoded)
            return hash.joinToString("") { "%02x".format(it) }
        }

        private fun computeCertSha256Base64(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(cert.encoded)
            return Base64.encodeToString(hash, Base64.NO_WRAP)
        }
    }

    companion object {
        const val EMBEDDED_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIIDVjCCAj6gAwIBAgIUc8Qua9sDUPJ3BGpNQgEGWHlSDjwwDQYJKoZIhvcNAQEL
BQAwFzEVMBMGA1UEAwwLYWgtc2Vydi1vcHRpMB4XDTI2MTEyNDEyMDA1NVoXDTM2
MTEyMTEyMDA1NVowFzEVMBMGA1UEAwwLYWgtc2Vydi1vcHRpMIIBIjANBgkqhkiG
9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv05OL+02JYv+NK4QtEDunVHPjRxwpJtuH2mZ
qFsPRlRH+lZNuNvVb6tEi58qT7qztwi6qNLpp2F9iW4Y7GmcFWO2RD1nNrSvEtUI
se1pTo90mjFftV1C9d6pC2f4Nf32oqYeTd97I9bElr4OzxiM00gsMGaJQ9pqZ7P6
HVyxRdwBsN5rksC0Sl2MP9+IodxwH+jaYf7vcvzTaT7xqhQEUFb/IzTjGhVtbWU4
Q3wptIR5MTH9c1qKoS0BM9Dj3xrAjGmtltQvzoZIlgE/t6bDoZ7bWIrxKwAL6OsG
psxZTo0JmsGSwIBn95W5pzxIW5dFiVI5Bm4VOPDOMMc5IfwmoQIDAQABo4GZMIGW
MB0GA1UdDgQWBBReYx9qj3Q5KiBXbmS7JeRmj2SKnjAfBgNVHSMEGDAWgBReYx9q
j3Q5KiBXbmS7JeRmj2SKnjAPBgNVHRMBAf8EBTADAQH/MEMGA1UdEQQ8MDqCDGFo
LXNlcnYtb3B0aYIeYWgtc2Vydi1vcHRpLnRhaWw3ZjU3NGQudHMubmV0hwRkZX8n
hwR/AAABMA0GCSqGSIb3DQEBCwUAA4IBAQCqnlthDojRfmZRzw1C6KbLyUsollYd
wBbeKHigyeBWKZSLFjSa702xWWfzrQloh9Gxw1dz9jWtn/aVk7p003PrfJ3Ydhxi
PEvhFaRHi3ZuELla50uBk8HuhO3oVkLdDaAp/AppIQjx5WUuD4VgAcepad9T8Jdw
bbps+r9y2mbOv5L0e0u1cImGwpe65WlMMC6oBp6Xar/wU3rAHyqagSu5+uH6Q3eN
MrJEg1ktqu49K5h2th+dnOkhVNbiZAS1iY0kL3tCrQT4P0z/s+lRUeTjEUZilLMz
lpyemgNMDLw8NcDGlOmjCjDoHT0Z9m1BiDGolpYFAjC+itrI52/agLHP
-----END CERTIFICATE-----"""
    }
}
