package com.ruirui.orbit.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruirui.orbit.android.app.OrbitApp
import com.ruirui.orbit.android.services.LocationSharingService
import com.ruirui.orbit.android.theme.OrbitTheme
import com.ruirui.orbit.android.ui.auth.AuthScreen
import com.ruirui.orbit.android.ui.auth.AuthViewModel
import com.ruirui.orbit.android.ui.friends.FriendsViewModel
import com.ruirui.orbit.android.ui.main.MainScreen
import com.ruirui.orbit.android.ui.map.MapViewModel
import com.ruirui.orbit.repository.AuthState

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
                        // TODO: Permission Checks

                        // Start foreground service if not already running
                        val intent = Intent(this, LocationSharingService::class.java)
                        ContextCompat.startForegroundService(this, intent)

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