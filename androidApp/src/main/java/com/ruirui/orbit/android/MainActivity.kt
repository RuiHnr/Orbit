package com.ruirui.orbit.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.messaging.FirebaseMessaging
import com.ruirui.orbit.android.app.OrbitApp
import com.ruirui.orbit.android.events.AppEventBus
import com.ruirui.orbit.android.events.OrbitEvent
import com.ruirui.orbit.android.services.LocationSharingService
import com.ruirui.orbit.android.theme.OrbitTheme
import com.ruirui.orbit.android.ui.auth.AuthScreen
import com.ruirui.orbit.android.ui.auth.AuthViewModel
import com.ruirui.orbit.android.ui.friends.FriendsViewModel
import com.ruirui.orbit.android.ui.main.MainScreen
import com.ruirui.orbit.android.ui.map.MapViewModel
import com.ruirui.orbit.android.ui.permissions.PermissionHandler
import com.ruirui.orbit.models.DeviceToken
import com.ruirui.orbit.repository.AuthState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.provideFactory(OrbitApp.instance.container.authRepository)
    }

    private val friendsViewModel: FriendsViewModel by viewModels {
        FriendsViewModel.provideFactory(OrbitApp.instance.container.friendRepository)
    }

    private val mapViewModel: MapViewModel by viewModels {
        MapViewModel.provideFactory(
            locationRepository = OrbitApp.instance.container.locationRepository,
            friendRepository = OrbitApp.instance.container.friendRepository,
            syncOrchestrator = OrbitApp.instance.container.syncOrchestrator
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OrbitTheme {
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                when (val state = authState) {
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is AuthState.Unauthenticated -> {
                        AuthScreen(
                            viewModel = authViewModel,
                        )
                    }

                    is AuthState.Authenticated -> {
                        // 1. Register FCM token with backend whenever user is authenticated
                        LaunchedEffect(Unit) {
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result
                                    // TODO: Retrieval still fails on Android 13
                                    Log.d("Orbit", "FCM Token retrieved: $token")
                                    AppEventBus.emit(OrbitEvent.NotificationShown("FCM token retrieved"))
                                    OrbitApp.instance.container.appScope.launch {
                                        OrbitApp.instance.container.deviceApi.postTokens(
                                            DeviceToken(fcmToken = token, apnsToken = null)
                                        ).onSuccess {
                                            Log.d("Orbit", "FCM token successfully registered on backend")
                                            AppEventBus.emit(OrbitEvent.NotificationShown("Token registered on server"))
                                        }.onFailure { e ->
                                            Log.e("Orbit", "Failed to register FCM token on backend", e)
                                            AppEventBus.emit(OrbitEvent.SyncError("Token upload failed: ${e.message}"))
                                        }
                                    }
                                } else {
                                    val err = task.exception?.message ?: "Unknown Firebase error"
                                    Log.e("Orbit", "Firebase get token failed: $err", task.exception)
                                    AppEventBus.emit(OrbitEvent.SyncError("FCM error: $err"))
                                }
                            }
                        }
                        // 2. Permission handling and service launch
                        PermissionHandler {
                            LaunchedEffect(Unit) {
                                // Start foreground service if not already running
                                val intent = Intent(this@MainActivity, LocationSharingService::class.java)
                                ContextCompat.startForegroundService(this@MainActivity, intent)
                            }
                            MainScreen(
                                friendsViewModel = friendsViewModel,
                                mapViewModel = mapViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}