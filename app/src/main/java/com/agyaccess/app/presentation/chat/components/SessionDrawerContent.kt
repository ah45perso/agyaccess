package com.agyaccess.app.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agyaccess.app.domain.model.SessionRuntimeState
import com.agyaccess.app.domain.model.SessionStatus
import com.agyaccess.app.presentation.theme.StatusErrorColor
import com.agyaccess.app.presentation.theme.StatusRunningColor
import com.agyaccess.app.presentation.theme.StatusWaitingColor

@Composable
fun SessionDrawerContent(
    activeSessions: List<SessionRuntimeState>,
    archivedSessions: List<SessionRuntimeState>,
    selectedSessionId: String?,
    nasHostDisplay: String,
    isRefreshing: Boolean,
    isCreatingSession: Boolean,
    onCreateSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onLockApp: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            // En-tête NAS + Actions rapides
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AgyAccess",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = nasHostDisplay.ifBlank { "NAS non configuré" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Synchroniser")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCreateSession,
                enabled = !isCreatingSession,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreatingSession) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nouvelle conversation")
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Conversations actives (${activeSessions.size})"
                    )
                }

                if (activeSessions.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune session active. Créez-en une pour discuter avec agy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(
                        items = activeSessions,
                        key = { it.session.sessionId }
                    ) { runtime ->
                        SessionDrawerRow(
                            runtime = runtime,
                            selected = runtime.session.sessionId == selectedSessionId,
                            onClick = { onSelectSession(runtime.session.sessionId) },
                            onToggleArchive = { onToggleArchive(runtime.session.sessionId) }
                        )
                    }
                }

                if (archivedSessions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Archivées (${archivedSessions.size})")
                    }
                    items(
                        items = archivedSessions,
                        key = { "arch_${it.session.sessionId}" }
                    ) { runtime ->
                        SessionDrawerRow(
                            runtime = runtime,
                            selected = runtime.session.sessionId == selectedSessionId,
                            onClick = { onSelectSession(runtime.session.sessionId) },
                            onToggleArchive = { onToggleArchive(runtime.session.sessionId) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NavigationDrawerItem(
                    label = { Text("Sécurité & NAS") },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    selected = false,
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onLockApp) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Verrouiller l'application"
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun SessionDrawerRow(
    runtime: SessionRuntimeState,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleArchive: () -> Unit
) {
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        icon = {
            SessionStatusIndicator(status = runtime.session.status, isStreaming = runtime.isRunning)
        },
        label = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = runtime.session.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected || runtime.hasUnreadUpdates) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (runtime.hasUnreadUpdates) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Text(
                    text = runtime.liveStatusBanner ?: runtime.session.status.displayLabel(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        badge = {
            IconButton(
                onClick = onToggleArchive,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (runtime.session.isArchived) {
                        Icons.Rounded.Unarchive
                    } else {
                        Icons.Rounded.Archive
                    },
                    contentDescription = if (runtime.session.isArchived) "Désarchiver" else "Archiver",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun SessionStatusIndicator(
    status: SessionStatus,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    if (isStreaming) {
        CircularProgressIndicator(
            modifier = modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = StatusRunningColor
        )
    } else {
        val color = when (status) {
            SessionStatus.RUNNING -> StatusRunningColor
            SessionStatus.WAITING -> StatusWaitingColor
            SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            SessionStatus.ERROR -> StatusErrorColor
            SessionStatus.IDLE -> MaterialTheme.colorScheme.outline
        }
        Box(
            modifier = modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

fun SessionStatus.displayLabel(): String = when (this) {
    SessionStatus.IDLE -> "Prêt"
    SessionStatus.RUNNING -> "En cours"
    SessionStatus.WAITING -> "En attente"
    SessionStatus.COMPLETED -> "Terminé"
    SessionStatus.ERROR -> "Erreur"
}
