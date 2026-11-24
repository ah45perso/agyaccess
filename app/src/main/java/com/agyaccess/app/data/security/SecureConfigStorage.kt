package com.agyaccess.app.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.agyaccess.app.di.ApplicationScope
import com.agyaccess.app.domain.model.NasConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stockage persistant chiffré de la configuration NAS.
 * Pré-configuré avec l'accès Tailscale natif 1-Click.
 */
@Singleton
class SecureConfigStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: KeystoreCryptoManager,
    private val json: Json,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _cachedConfig = MutableStateFlow(NasConfig())

    val currentConfig: StateFlow<NasConfig> = _cachedConfig.asStateFlow()

    val configFlow: Flow<NasConfig> = dataStore.data.map { prefs ->
        decodePreferences(prefs)
    }

    init {
        appScope.launch {
            configFlow.collect { config ->
                _cachedConfig.value = config
            }
        }
    }

    suspend fun readConfig(): NasConfig {
        val loaded = configFlow.first()
        _cachedConfig.value = loaded
        return loaded
    }

    suspend fun saveConfig(config: NasConfig): Result<Unit> = runCatching {
        val targetUrl = config.effectiveBaseUrl()
        require(NasConfig.isValidHttpsUrl(targetUrl)) {
            "L'URL du NAS doit impérativement utiliser HTTPS."
        }
        config.certificatePins.forEach { pin ->
            require(NasConfig.isValidPin(pin)) {
                "Empreinte Certificate Pin invalide : '$pin'"
            }
        }

        val encryptedUrl = cryptoManager.encrypt(targetUrl.trimEnd('/'))
        val encryptedToken = cryptoManager.encrypt(config.apiToken.trim())
        val encryptedPins = cryptoManager.encrypt(json.encodeToString(config.certificatePins))
        val encryptedMutualName = cryptoManager.encrypt(config.mutualAuthHeaderName.trim())
        val encryptedMutualValue = cryptoManager.encrypt(config.mutualAuthHeaderValue.trim())

        dataStore.edit { prefs ->
            prefs[KEY_NAS_URL_ENC] = encryptedUrl
            prefs[KEY_API_TOKEN_ENC] = encryptedToken
            prefs[KEY_CERT_PINS_ENC] = encryptedPins
            prefs[KEY_MUTUAL_HEADER_NAME_ENC] = encryptedMutualName
            prefs[KEY_MUTUAL_HEADER_VALUE_ENC] = encryptedMutualValue
            prefs[KEY_BIOMETRIC_ON_LAUNCH] = config.requireBiometricOnLaunch
            prefs[KEY_BIOMETRIC_SENSITIVE_PROMPT] = config.requireBiometricForSensitivePrompts
        }
        _cachedConfig.value = config
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
        _cachedConfig.value = NasConfig()
    }

    private fun decodePreferences(prefs: Preferences): NasConfig {
        val rawUrl = decryptSafe(prefs[KEY_NAS_URL_ENC])
        val url = rawUrl.ifBlank { NasConfig.DEFAULT_BASE_URL }
        val token = decryptSafe(prefs[KEY_API_TOKEN_ENC])
        val pinsJson = decryptSafe(prefs[KEY_CERT_PINS_ENC])
        val pins = runCatching {
            if (pinsJson.isBlank()) listOf(NasConfig.DEFAULT_PIN) else json.decodeFromString<List<String>>(pinsJson)
        }.getOrDefault(listOf(NasConfig.DEFAULT_PIN))
        val mutualName = decryptSafe(prefs[KEY_MUTUAL_HEADER_NAME_ENC])
        val mutualValue = decryptSafe(prefs[KEY_MUTUAL_HEADER_VALUE_ENC])

        return NasConfig(
            baseUrl = url,
            apiToken = token,
            certificatePins = pins,
            mutualAuthHeaderName = mutualName,
            mutualAuthHeaderValue = mutualValue,
            requireBiometricOnLaunch = prefs[KEY_BIOMETRIC_ON_LAUNCH] ?: false,
            requireBiometricForSensitivePrompts = prefs[KEY_BIOMETRIC_SENSITIVE_PROMPT] ?: false
        )
    }

    private fun decryptSafe(cipherText: String?): String {
        if (cipherText.isNullOrEmpty()) return ""
        return runCatching { cryptoManager.decrypt(cipherText) }.getOrDefault("")
    }

    companion object {
        private val KEY_NAS_URL_ENC = stringPreferencesKey("nas_url_enc")
        private val KEY_API_TOKEN_ENC = stringPreferencesKey("api_token_enc")
        private val KEY_CERT_PINS_ENC = stringPreferencesKey("cert_pins_enc")
        private val KEY_MUTUAL_HEADER_NAME_ENC = stringPreferencesKey("mutual_header_name_enc")
        private val KEY_MUTUAL_HEADER_VALUE_ENC = stringPreferencesKey("mutual_header_value_enc")
        private val KEY_BIOMETRIC_ON_LAUNCH = booleanPreferencesKey("biometric_on_launch")
        private val KEY_BIOMETRIC_SENSITIVE_PROMPT = booleanPreferencesKey("biometric_sensitive_prompt")
    }
}
