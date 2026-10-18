package com.ruirui.orbit.android.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruirui.orbit.models.LocationPayload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapContent(
    uiState: MapUiState,
    onIntent: (MapUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Orbit — Location Feed") },
                actions = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { onIntent(MapUiIntent.Refresh) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync now")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error banner
            uiState.errorMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠ $msg",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── Friend Location Cards ────────────────────────────────────────
            Text(
                text = "Friend Locations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            if (uiState.friendLocations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No friend locations yet.\nTap ↻ to sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = uiState.friendLocations.entries.toList(),
                        key = { it.key }
                    ) { (_, pair) ->
                        val (username, payload) = pair
                        FriendLocationCard(username = username, payload = payload)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Event Log ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Event Log", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = { onIntent(MapUiIntent.ClearLog) }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { onIntent(MapUiIntent.ToggleLog) }) {
                        Icon(
                            if (uiState.isLogExpanded) Icons.Default.ExpandLess
                            else Icons.Default.ExpandMore,
                            contentDescription = "Toggle log"
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.isLogExpanded) {
                if (uiState.eventLog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No events yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.eventLog,
                            key = { it.id }
                        ) { entry ->
                            EventLogRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun FriendLocationCard(
    username: String,
    payload: LocationPayload,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "👤 $username",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Lat: ${"%.6f".format(payload.lat)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Lng: ${"%.6f".format(payload.lng)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "As of ${payload.timestamp.toTimeString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventLogRow(
    entry: EventLogEntry,
    modifier: Modifier = Modifier
) {
    val tagColor = when (entry.tag) {
        EventTag.LOCATION_SENT     -> MaterialTheme.colorScheme.primary
        EventTag.LOCATION_RECEIVED -> MaterialTheme.colorScheme.secondary
        EventTag.PRIORITY_CHANGED  -> MaterialTheme.colorScheme.tertiary
        EventTag.NOTIFICATION      -> MaterialTheme.colorScheme.outline
        EventTag.ERROR             -> MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.timestampMs.toTimeString(),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Spacer(Modifier.width(6.dp))
        Surface(
            color = tagColor.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = "${entry.tag.emoji} ${entry.tag.label}",
                style = MaterialTheme.typography.labelSmall,
                color = tagColor,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.summary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}