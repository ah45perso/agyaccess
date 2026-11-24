package com.agyaccess.app.data.remote.network

import android.util.Base64
import com.agyaccess.app.BuildConfig
import com.agyaccess.app.data.security.SecureConfigStorage
import com.agyaccess.app.domain.model.NasConfig
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
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
 *   sur réseau privé/Tailscale via un [PinnedCertTrustManager] inviolable.
 * - Pré-configure les empreintes de sécurité Tailscale pour une connexion immédiate sans friction.
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
                configStorage.currentConfig.value.certificatePins.ifEmpty { listOf(NasConfig.DEFAULT_PIN) }
            }
        )

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }

        val pinner = buildCertificatePinner(host, effectivePins)

        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(strictTlsSpec))
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .certificatePinner(pinner)
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

    private fun buildCertificatePinner(host: String?, pins: List<String>): CertificatePinner {
        if (host.isNullOrBlank() || pins.isEmpty()) {
            return CertificatePinner.DEFAULT
        }
        val builder = CertificatePinner.Builder()
        pins.mapNotNull(NasConfig::normalizeToOkHttpPin).forEach { okHttpPin ->
            builder.add(host, okHttpPin)
        }
        return builder.build()
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

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            systemTrustManager.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            if (chain.isNullOrEmpty()) {
                throw CertificateException("Chaîne de certificats serveur vide ou absente.")
            }

            try {
                systemTrustManager.checkServerTrusted(chain, authType)
                return
            } catch (systemException: CertificateException) {
                val configuredPins = allowedPinsProvider()
                if (configuredPins.isEmpty()) {
                    throw systemException
                }

                val pinMatched = chain.any { cert ->
                    cert.checkValidity()
                    val spkiB64 = computeSpkiPin(cert)
                    val certHex = computeCertSha256Hex(cert)
                    val certB64 = computeCertSha256Base64(cert)

                    configuredPins.any { rawPin ->
                        val pin = rawPin.trim()
                        pin.equals(spkiB64, ignoreCase = true) ||
                            pin.equals("sha256/$spkiB64", ignoreCase = true) ||
                            pin.replace(":", "").equals(certHex.replace(":", ""), ignoreCase = true) ||
                            pin.equals(certB64, ignoreCase = true)
                    }
                }

                if (!pinMatched) {
                    throw CertificateException(
                        "Certificat serveur non approuvé : aucune empreinte SHA-256 ne correspond aux pins autorisés.",
                        systemException
                    )
                }
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            systemTrustManager.acceptedIssuers

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
}
