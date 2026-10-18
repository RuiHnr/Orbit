package com.ruirui.orbit.android.events

/** Typed events that can be observed anywhere in the Android process. */
sealed interface OrbitEvent {
    data class LocationSent(
        val lat: Double,
        val lng: Double,
        val recipientCount: Int,
        val timestampMs: Long = System.currentTimeMillis()
    ) : OrbitEvent

    data class LocationReceived(
        val senderUsername: String,
        val lat: Double,
        val lng: Double,
        val timestampMs: Long = System.currentTimeMillis()
    ) : OrbitEvent

    data class PriorityChanged(
        val newMode: String,   // "HIGH" or "LOW"
        val timestampMs: Long = System.currentTimeMillis()
    ) : OrbitEvent

    data class NotificationShown(
        val message: String,
        val timestampMs: Long = System.currentTimeMillis()
    ) : OrbitEvent

    data class SyncError(
        val message: String,
        val timestampMs: Long = System.currentTimeMillis()
    ) : OrbitEvent
}