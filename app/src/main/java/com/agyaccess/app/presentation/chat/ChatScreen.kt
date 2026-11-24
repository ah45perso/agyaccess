package com.agyaccess.app.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agyaccess.app.domain.model.ConnectionState
import com.agyaccess.app.domain.model.NasConfig
import com.agyaccess.app.domain.model.SessionRuntimeState
import com.agyaccess.app.presentation.chat.components.MessageBubble
import com.agyaccess.app.presentation.chat.components.PromptInputBar
import com.agyaccess.app.presentation.chat.components.SessionDrawerContent
import com.agyaccess.app.presentation.chat.components.SessionStatusIndicator
import com.agyaccess.app.presentation.chat.components.displayLabel
import com.agyaccess.app.presentation.lock.BiometricLockScreen
import com.agyaccess.app.presentation.settings.SettingsDialog
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ChatIntent) -> Unit
) {
    if (uiState.isAppLocked) {
        BiometricLockScreen(
            onRequestUnlock = { onIntent(ChatIntent.RequestAppUnlock) }
        )
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val activeSession = uiState.activeSession
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Supprimer la conversation") },
            text = { Text("Voulez-vous vraiment supprimer définitivement cette conversation ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = sessionToDelete ?: return@TextButton
                        onIntent(ChatIntent.DeleteSession(id))
                        sessionToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SessionDrawerContent(
                activeSessions = uiState.activeSessions,
                archivedSessions = uiState.archivedSessions,
                selectedSessionId = uiState.selectedSessionId,
                nasHostDisplay = uiState.nasConfig.extractHost().orEmpty(),
                isRefreshing = uiState.isRefreshingSessions,
                isCreatingSession = uiState.isCreatingSession,
                onCreateSession = {
                    onIntent(ChatIntent.CreateNewSession)
                    coroutineScope.launch { drawerState.close() }
                },
                onSelectSession = { sessionId ->
                    onIntent(ChatIntent.SelectSession(sessionId))
                    coroutineScope.launch { drawerState.close() }
                },
                onToggleArchive = { onIntent(ChatIntent.ToggleArchiveSession(it)) },
                onDeleteSession = { sessionId ->
                    sessionToDelete = sessionId
                    coroutineScope.launch { drawerState.close() }
                },
                onRefresh = { onIntent(ChatIntent.RefreshSessions) },
                onOpenSettings = {
                    coroutineScope.launch { drawerState.close() }
                    onIntent(ChatIntent.OpenSettings)
                },
                onLockApp = {
                    coroutineScope.launch { drawerState.close() }
                    onIntent(ChatIntent.LockAppNow)
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ChatTopBar(
                    activeSession = activeSession,
                    runningBackgroundCount = uiState.runningBackgroundCount,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onCreateSession = { onIntent(ChatIntent.CreateNewSession) },
                    onDeleteSession = { sessionToDelete = it },
                    onOpenSettings = { onIntent(ChatIntent.OpenSettings) }
                )
            },
            bottomBar = {
                PromptInputBar(
                    draft = uiState.currentDraft,
                    isRunning = activeSession?.isRunning == true,
                    selectedModelOverride = uiState.selectedModelOverride,
                    biometricSensitiveGuardEnabled = uiState.nasConfig.requireBiometricForSensitivePrompts,
                    onDraftChange = { onIntent(ChatIntent.UpdateDraft(it)) },
                    onModelSelect = { onIntent(ChatIntent.SelectModelOverride(it)) },
                    onSend = { onIntent(ChatIntent.SubmitPrompt) },
                    onCancel = { onIntent(ChatIntent.CancelActiveSession) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Bandeau de statut temps réel (réflexion, appel d'outil, reconnexion SSE)
                LiveStatusStrip(activeSession = activeSession)

                // Bandeau d'erreur global (réseau / NAS / TLS)
                AnimatedVisibility(visible = !uiState.errorBanner.isNullOrBlank()) {
                    ErrorBanner(
                        message = uiState.errorBanner.orEmpty(),
                        onDismiss = { onIntent(ChatIntent.DismissError) }
                    )
                }

                // Corps de conversation ou écran d'accueil
                if (!uiState.nasConfig.isConfigured) {
                    UnconfiguredNasPlaceholder(
                        onQuickConnect = {
                            onIntent(ChatIntent.SaveSettings(NasConfig()))
                            onIntent(ChatIntent.RefreshSessions)
                        },
                        onOpenSettings = { onIntent(ChatIntent.OpenSettings) }
                    )
                } else if (activeSession == null || activeSession.messages.isEmpty()) {
                    EmptyConversationPlaceholder(
                        sessionTitle = activeSession?.session?.title,
                        backgroundRunningCount = uiState.runningBackgroundCount
                    )
                } else {
                    MessagesList(
                        sessionState = activeSession,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (uiState.isSettingsOpen) {
        SettingsDialog(
            initialConfig = uiState.nasConfig,
            onDismiss = { onIntent(ChatIntent.CloseSettings) },
            onSave = { onIntent(ChatIntent.SaveSettings(it)) },
            onClearSecrets = { onIntent(ChatIntent.ClearSecrets) }
        )
    }
}

@Composable
private fun ChatTopBar(
    activeSession: SessionRuntimeState?,
    runningBackgroundCount: Int,
    onOpenDrawer: () -> Unit,
    onCreateSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                if (runningBackgroundCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge { Text(runningBackgroundCount.toString()) }
                        }
                    ) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Ouvrir les sessions")
                    }
                } else {
                    Icon(Icons.Rounded.Menu, contentDescription = "Ouvrir les sessions")
                }
            }
        },
        title = {
            Column {
                Text(
                    text = activeSession?.session?.title ?: "AgyAccess",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                if (activeSession != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SessionStatusIndicator(
                            status = activeSession.session.status,
                            isStreaming = activeSession.isRunning,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = when (activeSession.connectionState) {
                                ConnectionState.RECONNECTING ->
                                    "Reconnexion (${activeSession.reconnectAttempt})…"
                                ConnectionState.CONNECTING -> "Connexion SSE…"
                                else -> activeSession.session.status.displayLabel()
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        actions = {
            if (activeSession != null) {
                IconButton(onClick = { onDeleteSession(activeSession.session.sessionId) }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = "Supprimer la conversation",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(onClick = onCreateSession) {
                Icon(Icons.Rounded.AddComment, contentDescription = "Nouvelle session")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Paramètres NAS & Sécurité")
            }
        }
    )
}

@Composable
private fun LiveStatusStrip(activeSession: SessionRuntimeState?) {
    val bannerText = activeSession?.liveStatusBanner
    val isRunning = activeSession?.isRunning == true

    AnimatedVisibility(visible = isRunning || !bannerText.isNullOrBlank()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (!bannerText.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Fermer l'alerte",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MessagesList(
    sessionState: SessionRuntimeState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lastMessageLength = (sessionState.messages.lastOrNull() as? com.agyaccess.app.domain.model.ChatMessage.Assistant)
        ?.content?.length ?: 0

    // Défilement automatique fluide lors de la réception de nouveaux messages ou tokens
    LaunchedEffect(sessionState.session.sessionId, sessionState.messages.size, lastMessageLength) {
        if (sessionState.messages.isNotEmpty()) {
            listState.animateScrollToItem(sessionState.messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = sessionState.messages,
            key = { it.id }
        ) { message ->
            MessageBubble(message = message)
        }
    }
}

@Composable
private fun EmptyConversationPlaceholder(
    sessionTitle: String?,
    backgroundRunningCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = sessionTitle ?: "Prêt à interagir avec agy",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Envoyez un prompt pour démarrer le streaming en temps réel. " +
                    "Vous pouvez basculer librement vers d'autres sessions sans interrompre les tâches en cours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (backgroundRunningCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$backgroundRunningCount session(s) active(s) en tâche de fond",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun UnconfiguredNasPlaceholder(
    onQuickConnect: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Connexion Tailscale au NAS agy",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Connectez-vous automatiquement en 1 clic grâce à votre réseau privé sécurisé Tailscale.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onQuickConnect,
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("⚡ Se connecter au NAS")
            }
            TextButton(onClick = onOpenSettings) {
                Text("Options de configuration manuelles")
            }
        }
    }
}
