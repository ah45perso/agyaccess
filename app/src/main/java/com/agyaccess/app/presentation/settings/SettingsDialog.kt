package com.agyaccess.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.agyaccess.app.domain.model.NasConfig

@Composable
fun SettingsDialog(
    initialConfig: NasConfig,
    onDismiss: () -> Unit,
    onSave: (NasConfig) -> Unit,
    onClearSecrets: () -> Unit
) {
    var baseUrl by remember(initialConfig) { mutableStateOf(initialConfig.baseUrl) }
    var apiToken by remember(initialConfig) { mutableStateOf(initialConfig.apiToken) }
    var pinsText by remember(initialConfig) {
        mutableStateOf(initialConfig.certificatePins.joinToString("\n"))
    }
    var mutualHeaderName by remember(initialConfig) {
        mutableStateOf(initialConfig.mutualAuthHeaderName)
    }
    var mutualHeaderValue by remember(initialConfig) {
        mutableStateOf(initialConfig.mutualAuthHeaderValue)
    }
    var biometricOnLaunch by remember(initialConfig) {
        mutableStateOf(initialConfig.requireBiometricOnLaunch)
    }
    var biometricOnSensitive by remember(initialConfig) {
        mutableStateOf(initialConfig.requireBiometricForSensitivePrompts)
    }
    var showToken by remember { mutableStateOf(false) }

    val parsedPins = pinsText
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()

    val isUrlValid = NasConfig.isValidHttpsUrl(baseUrl)
    val arePinsValid = parsedPins.all(NasConfig::isValidPin)
    val canSave = isUrlValid && apiToken.isNotBlank() && arePinsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sécurité & Connexion NAS") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("URL HTTPS du NAS *") },
                    placeholder = { Text("https://agy.mon-nas.local:8443") },
                    isError = baseUrl.isNotBlank() && !isUrlValid,
                    supportingText = {
                        if (baseUrl.isNotBlank() && !isUrlValid) {
                            Text("HTTPS obligatoire (http:// est bloqué par la politique réseau).")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("Clé API / JWT Bearer *") },
                    singleLine = true,
                    visualTransformation = if (showToken) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = "Afficher / masquer la clé"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pinsText,
                    onValueChange = { pinsText = it },
                    label = { Text("Certificate Pinning SHA-256 (optionnel)") },
                    placeholder = { Text("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") },
                    isError = !arePinsValid,
                    supportingText = {
                        Text(
                            if (!arePinsValid) {
                                "Format requis : sha256/BASE64 (1 empreinte par ligne)."
                            } else {
                                "1 empreinte par ligne. Laissez vide pour la validation TLS système."
                            }
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mutualHeaderName,
                    onValueChange = { mutualHeaderName = it },
                    label = { Text("En-tête Auth Mutuelle (optionnel)") },
                    placeholder = { Text("X-Client-Cert-Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (mutualHeaderName.isNotBlank()) {
                    OutlinedTextField(
                        value = mutualHeaderValue,
                        onValueChange = { mutualHeaderValue = it },
                        label = { Text("Valeur de l'en-tête mutuel") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Biométrie au lancement",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Exige BiometricPrompt à l'ouverture de l'app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricOnLaunch,
                        onCheckedChange = { biometricOnLaunch = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Confirmation biométrique sensible",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Valide les prompts à fort impact avant envoi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricOnSensitive,
                        onCheckedChange = { biometricOnSensitive = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = onClearSecrets,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = " Effacer les secrets Keystore",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        NasConfig(
                            baseUrl = baseUrl.trim(),
                            apiToken = apiToken.trim(),
                            certificatePins = parsedPins,
                            mutualAuthHeaderName = mutualHeaderName.trim(),
                            mutualAuthHeaderValue = mutualHeaderValue.trim(),
                            requireBiometricOnLaunch = biometricOnLaunch,
                            requireBiometricForSensitivePrompts = biometricOnSensitive
                        )
                    )
                }
            ) {
                Text("Chiffrer et enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
