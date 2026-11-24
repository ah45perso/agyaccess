package com.agyaccess.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agyaccess.app.data.security.BiometricAuthManager
import com.agyaccess.app.presentation.chat.ChatEffect
import com.agyaccess.app.presentation.chat.ChatIntent
import com.agyaccess.app.presentation.chat.ChatScreen
import com.agyaccess.app.presentation.chat.ChatViewModel
import com.agyaccess.app.presentation.theme.AgyAccessTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activité principale héritant de [FragmentActivity] (requis par `BiometricPrompt`).
 * Active `FLAG_SECURE` afin d'empêcher les captures d'écran et l'exposition des
 * clés API / conversations dans l'aperçu des applications récentes Android.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        setContent {
            AgyAccessTheme {
                val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    chatViewModel.effects.collect { effect ->
                        when (effect) {
                            is ChatEffect.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(effect.message)
                            }

                            is ChatEffect.TriggerAppUnlockBiometric -> {
                                when (
                                    val result = biometricAuthManager.authenticate(
                                        activity = this@MainActivity,
                                        title = "Déverrouiller AgyAccess",
                                        subtitle = "Accès sécurisé à votre agent agy"
                                    )
                                ) {
                                    is BiometricAuthManager.AuthResult.Success -> {
                                        chatViewModel.onIntent(ChatIntent.OnAppUnlocked)
                                    }

                                    is BiometricAuthManager.AuthResult.Unavailable -> {
                                        // Si aucun matériel/code biométrique n'est enrôlé, évite le blocage permanent
                                        chatViewModel.onIntent(ChatIntent.OnAppUnlocked)
                                        snackbarHostState.showSnackbar(result.reason)
                                    }

                                    is BiometricAuthManager.AuthResult.Failed -> {
                                        snackbarHostState.showSnackbar(
                                            "Authentification refusée : ${result.message}"
                                        )
                                    }

                                    is BiometricAuthManager.AuthResult.Cancelled -> Unit
                                }
                            }

                            is ChatEffect.TriggerSensitivePromptBiometric -> {
                                when (
                                    val result = biometricAuthManager.authenticate(
                                        activity = this@MainActivity,
                                        title = "Confirmer l'instruction sensible",
                                        subtitle = "Vérification biométrique requise avant envoi",
                                        description = effect.pending.prompt.take(140)
                                    )
                                ) {
                                    is BiometricAuthManager.AuthResult.Success -> {
                                        chatViewModel.onIntent(
                                            ChatIntent.ConfirmSensitivePrompt(effect.pending)
                                        )
                                    }

                                    is BiometricAuthManager.AuthResult.Unavailable -> {
                                        snackbarHostState.showSnackbar(
                                            "Envoi bloqué : ${result.reason}"
                                        )
                                    }

                                    is BiometricAuthManager.AuthResult.Failed -> {
                                        snackbarHostState.showSnackbar(
                                            "Envoi annulé (${result.message})."
                                        )
                                    }

                                    is BiometricAuthManager.AuthResult.Cancelled -> {
                                        snackbarHostState.showSnackbar("Envoi sensible annulé.")
                                    }
                                }
                            }
                        }
                    }
                }

                ChatScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onIntent = chatViewModel::onIntent
                )
            }
        }
    }
}
