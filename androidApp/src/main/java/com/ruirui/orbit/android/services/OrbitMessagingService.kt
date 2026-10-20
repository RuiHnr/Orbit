package com.ruirui.orbit.android.services

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruirui.orbit.android.app.OrbitApp
import com.ruirui.orbit.models.DeviceToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service for handling Firebase Cloud Messaging (FCM) push notifications.
 *
 * Responsible for:
 * 1. Receiving new FCM registration tokens
 * 2. Handling incoming push notifications
 * 3. Triggering location sharing mode changes
 */
class OrbitMessagingService : FirebaseMessagingService() {

    /**
     * Recognized sync modes for location sharing
     */
    object SyncMode {
        /** Frequent location updates (when friends are actively watching) */
        const val HIGH = "high"

        /** Less frequent updates (when no friends are watching) */
        const val LOW = "low"
    }

    override fun onRegistered(fid: String) {
        super.onRegistered(fid)
        CoroutineScope(Dispatchers.IO).launch {
            OrbitApp.instance.container.deviceApi.postTokens(
                DeviceToken(fid = fid, apnsToken = null)
            ).onSuccess {
                Log.d("Orbit", "FID sent to server: $fid")
            }.onFailure {
                Log.e("Orbit", "Failed to send FID", it)
            }
        }
    }

    /**
     * Called when a new FCM message is received.
     *
     * @param message The received FCM message
     */
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("Orbit", "FCM push received: ${message.data}")
        if (message.data["action"] == "sync") {
            // If it's a sync mode sync notification
            val mode = message.data["mode"]
            if (mode == SyncMode.HIGH || mode == SyncMode.LOW) {
                val intent = Intent(this, LocationSharingService::class.java).apply {
                    putExtra("mode", mode)
                }
                ContextCompat.startForegroundService(this, intent)
            }
        }
    }
}