package com.agyaccess.app.data.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Gestionnaire d'authentification biométrique natif (`BiometricPrompt`).
 *
 * Permet de protéger :
 *  - le déverrouillage au lancement de l'application,
 *  - l'envoi d'instructions jugées sensibles vers l'agent agy.
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed interface AuthResult {
        data object Success : AuthResult
        data object Cancelled : AuthResult
        data class Unavailable(val reason: String) : AuthResult
        data class Failed(val errorCode: Int, val message: String) : AuthResult
    }

    private fun supportedAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_STRONG
        }
    }

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(supportedAuthenticators()) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun availabilityErrorMessage(): String? {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(supportedAuthenticators())) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Aucun capteur biométrique détecté sur cet appareil."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Le capteur biométrique est temporairement indisponible."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Aucune empreinte ou code d'appareil n'est configuré dans Android."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Une mise à jour de sécurité Android est requise pour la biométrie."
            else -> "Authentification biométrique indisponible."
        }
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String? = null
    ): AuthResult {
        availabilityErrorMessage()?.let { reason ->
            return AuthResult.Unavailable(reason)
        }

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    if (continuation.isActive) {
                        continuation.resume(AuthResult.Success)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!continuation.isActive) return
                    val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    if (cancelled) {
                        continuation.resume(AuthResult.Cancelled)
                    } else {
                        continuation.resume(AuthResult.Failed(errorCode, errString.toString()))
                    }
                }

                override fun onAuthenticationFailed() {
                    // Android maintient le prompt ouvert pour un nouvel essai ; rien à terminer ici.
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setConfirmationRequired(true)

            if (!description.isNullOrBlank()) {
                promptInfoBuilder.setDescription(description)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            } else {
                promptInfoBuilder
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .setNegativeButtonText("Annuler")
            }

            continuation.invokeOnCancellation {
                prompt.cancelAuthentication()
            }

            prompt.authenticate(promptInfoBuilder.build())
        }
    }
}
