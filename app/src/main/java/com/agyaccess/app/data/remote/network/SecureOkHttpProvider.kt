package com.agyaccess.app.data.remote.network

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
 * - Applique dynamiquement le **Certificate Pinning** (`sha256/...`) pour l'hôte NAS configuré.
 * - Partage le pool de connexions (`newBuilder()`) entre le client REST et le client SSE
 *   (ce dernier utilisant `readTimeout = 0` pour les flux temps réel persistants).
 * - Rédige (masque) `Authorization` et les en-têtes d'auth mutuelle même en build Debug.
 * - Fournit un utilitaire prêt à l'emploi pour le mTLS (certificat client PKCS#12).
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

    private val baseClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectionSpecs(listOf(strictTlsSpec))
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
        builder.build()
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
        val host = config.extractHost()
        val pins = config.certificatePins.filter(NasConfig::isValidPin)
        val key = ClientCacheKey(host, pins)

        if (key == cachedKey && cachedRestClient != null && cachedSseClient != null) {
            return
        }

        val pinner = buildCertificatePinner(host, pins)

        cachedRestClient = baseClient.newBuilder()
            .certificatePinner(pinner)
            .build()

        cachedSseClient = baseClient.newBuilder()
            .certificatePinner(pinner)
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
        pins.forEach { pin -> builder.add(host, pin) }
        return builder.build()
    }

    /**
     * Utilitaire optionnel pour activer l'authentification mutuelle TLS (mTLS)
     * à partir d'un certificat client PKCS#12 (.p12) protégé par mot de passe.
     */
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
}
