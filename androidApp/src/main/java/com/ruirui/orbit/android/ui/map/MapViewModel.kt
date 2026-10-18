package com.ruirui.orbit.android.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ruirui.orbit.android.events.AppEventBus
import com.ruirui.orbit.android.events.OrbitEvent
import com.ruirui.orbit.repository.FriendRepository
import com.ruirui.orbit.repository.LocationRepository
import com.ruirui.orbit.sync.SyncOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class MapViewModel(
    private val locationRepository: LocationRepository,
    private val friendRepository: FriendRepository,
    private val syncOrchestrator: SyncOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val logIdCounter = AtomicLong(0)

    init {
        observeLocations()
        observeEvents()
    }

    /** Observes KMP StateFlow for friend locations */
    private fun observeLocations() {
        viewModelScope.launch {
            combine(
                locationRepository.friendLocations,
                friendRepository.friends
            ) { locMap, friends ->
                // Build username lookup: userId -> username
                val usernameById = friends.associate { it.userId to it.username }
                locMap.mapValues { (userId, payload) ->
                    Pair(usernameById[userId] ?: userId, payload)
                }
            }.collect { newLocations ->
                val old = _uiState.value.friendLocations

                // Detect new or updated locations and append log entries
                newLocations.forEach { userId, pair ->
                    val (username, payload) = pair
                    val prev = old[userId]?.second
                    if (prev == null || prev.timestamp != payload.timestamp) {
                        appendLog(
                            tag = EventTag.LOCATION_RECEIVED,
                            summary = "$username → ${payload.lat.fmt()}, ${payload.lng.fmt()}"
                        )
                    }
                }

                _uiState.update { it.copy(friendLocations = newLocations) }
            }
        }
    }

    /** Observes AppEventBus for cross-layer events */
    private fun observeEvents() {
        viewModelScope.launch {
            AppEventBus.events.collect { event ->
                when (event) {
                    is OrbitEvent.LocationSent -> appendLog(
                        tag = EventTag.LOCATION_SENT,
                        summary = "Sent to ${event.recipientCount} friend(s) — " +
                                "${event.lat.fmt()}, ${event.lng.fmt()}"
                    )
                    is OrbitEvent.LocationReceived -> { /* handle via [observeLocations] */ }
                    is OrbitEvent.NotificationShown -> appendLog(
                        tag = EventTag.NOTIFICATION,
                        summary = event.message
                    )
                    is OrbitEvent.PriorityChanged -> appendLog(
                        tag = EventTag.PRIORITY_CHANGED,
                        summary = "Mode → ${event.newMode}"
                    )
                    is OrbitEvent.SyncError -> appendLog(
                        tag = EventTag.ERROR,
                        summary = event.message
                    )
                }
            }
        }
    }

    fun onIntent(intent: MapUiIntent) {
        when (intent) {
            is MapUiIntent.Refresh -> refresh()
            is MapUiIntent.ToggleLog -> _uiState.update { it.copy(isLogExpanded = !it.isLogExpanded) }
            is MapUiIntent.ClearLog -> _uiState.update { it.copy(eventLog = emptyList()) }
            is MapUiIntent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            val result = syncOrchestrator.syncAll()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun appendLog(tag: EventTag, summary: String) {
        val entry = EventLogEntry(
            id = logIdCounter.incrementAndGet(),
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            summary = summary
        )
        _uiState.update { state ->
            // Newest first; cap at 200 to avoid unbounded growth
            val updated = (listOf(entry) + state.eventLog).take(200)
            state.copy(eventLog = updated)
        }
    }

    private fun Double.fmt() = "%.5f".format(this)

    companion object {
        fun provideFactory(
            locationRepository: LocationRepository,
            friendRepository: FriendRepository,
            syncOrchestrator: SyncOrchestrator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MapViewModel(locationRepository, friendRepository, syncOrchestrator) as T
        }
    }
}