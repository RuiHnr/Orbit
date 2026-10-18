package com.ruirui.orbit.android.ui.map

import com.ruirui.orbit.models.LocationPayload

/** A single entry in the on-screen event log. */
data class EventLogEntry(
    val id: Long,                 // monotonically increasing, used as LazyColumn key
    val timestampMs: Long,
    val tag: EventTag,
    val summary: String           // human-readable one-liner
)

enum class EventTag(val label: String, val emoji: String) {
    LOCATION_SENT("SENT", "📤"),
    LOCATION_RECEIVED("RECV", "📥"),
    PRIORITY_CHANGED("PRIORITY", "⚡"),
    NOTIFICATION("NOTIF", "🔔"),
    ERROR("ERROR", "❌")
}

typealias FriendLocationMap = Map<String, Pair<String, LocationPayload>>

data class MapUiState(
    val friendLocations: FriendLocationMap = emptyMap(),

    val eventLog: List<EventLogEntry> = emptyList(),

    val isSyncing: Boolean = false,

    val errorMessage: String? = null,

    val isLogExpanded: Boolean = true
)

sealed interface MapUiIntent {
    data object Refresh : MapUiIntent
    data object ToggleLog : MapUiIntent
    data object ClearLog : MapUiIntent
    data object DismissError : MapUiIntent
}
