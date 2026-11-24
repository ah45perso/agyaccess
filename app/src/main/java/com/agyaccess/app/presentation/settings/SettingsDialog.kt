package com.agyaccess.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agyaccess.app.domain.model.NasConfig

@Composable
fun SettingsDialog(
    initialConfig: NasConfig,
    onDismiss: () -> Unit,
    onSave: (NasConfig) -> Unit,
    onClearSecrets: () -> Unit
) {
    var baseUrl by remember(initialConfig) {
        mutableStateOf(initialConfig.baseUrl.ifBlank { NasConfig.DEFAULT_BASE_URL })
    }
    var apiToken by remember(initialConfig) { mutableStateOf(initialConfig.apiToken) }
    var pinsText by remember(initialConfig) {
        mutableStateOf(
            (if (initialConfig.certificatePins.isEmpty()) listOf(NasConfig.DEFAULT_PIN) else initialConfig.certificatePins)
                .joinToString("\n")
        )
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
    var showAdvanced by remember { mutableStateOf(false) }

    val parsedPins = pinsText
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()

    val isUrlValid = NasConfig.isValidHttpsUrl(baseUrl)
    val arePinsValid = parsedPins.all(NasConfig::isValidPin)
    val canSave = isUrlValid && arePinsValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connexion au NAS agy") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Bouton 1-Click Tailscale principal
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Accès Tailscale 1-Click",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Si Tailscale est actif sur votre téléphone, appuyez ci-dessous pour vous connecter instantanément sans rien saisir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val fastConfig = NasConfig(
                                    baseUrl = NasConfig.DEFAULT_BASE_URL,
                                    apiToken = "",
                                    certificatePins = listOf(NasConfig.DEFAULT_PIN),
                                    requireBiometricOnLaunch = biometricOnLaunch,
                                    requireBiometricForSensitivePrompts = biometricOnSensitive
                                )
                                onSave(fastConfig)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Se connecter au NAS")
                        }
                    }
                }

                HorizontalDivider()

                // 2. Options avancées / Personnalisation
                OutlinedButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (showAdvanced) "Masquer les options manuelles" else "Options manuelles / personnalisées")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = showAdvanced) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("URL HTTPS du serveur agy") },
                            placeholder = { Text(NasConfig.DEFAULT_BASE_URL) },
                            isError = baseUrl.isNotBlank() && !isUrlValid,
                            supportingText = {
                                if (baseUrl.isNotBlank() && !isUrlValid) {
                                    Text("HTTPS obligatoire.")
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = apiToken,
                            onValueChange = { apiToken = it },
                            label = { Text("Clé API / Token Bearer (optionnel)") },
                            placeholder = { Text("Non requis si connecté via Tailscale") },
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
                            label = { Text("Certificate Pinning SHA-256") },
                            placeholder = { Text(NasConfig.DEFAULT_PIN) },
                            isError = !arePinsValid,
                            supportingText = {
                                Text("Empreinte du certificat auto-signé sur Tailscale (1 par ligne).")
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
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
