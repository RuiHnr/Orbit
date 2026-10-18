package com.ruirui.orbit.android.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruirui.orbit.android.app.OrbitApp

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        val presenceManager = OrbitApp.instance.container.presenceManager
        presenceManager.start()

        onDispose {
            presenceManager.stop()
        }
    }

    MapContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}