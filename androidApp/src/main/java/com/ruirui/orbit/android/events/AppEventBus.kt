package com.ruirui.orbit.android.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventBus {
    private val _events = MutableSharedFlow<OrbitEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<OrbitEvent> = _events.asSharedFlow()

    /** Fire-and-forget; non-suspending so it's safe from services / callbacks. */
    fun emit(event: OrbitEvent) {
        _events.tryEmit(event)
    }
}