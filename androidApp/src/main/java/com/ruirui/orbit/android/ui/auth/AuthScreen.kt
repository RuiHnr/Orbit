package com.ruirui.orbit.android.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruirui.orbit.android.theme.OrbitTheme

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onRegistrationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegistrationSuccess()
    }

    // Passes state down to the stateless AuthContent
    AuthContent(
        uiState = uiState,
        onUsernameChanged = viewModel::onUsernameChanged,
        onRegister = viewModel::onRegister,
        modifier = modifier
    )
}