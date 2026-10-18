package com.ruirui.orbit.android.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ruirui.orbit.android.theme.OrbitTheme

// 1. Interactive Preview (Light Mode)
@Preview(showBackground = true)
@Composable
fun AuthScreenPreviewLight() {
    OrbitTheme(darkTheme = false) {
        AuthContent(
            uiState = AuthUiState(usernameInput = "alice"),
            onUsernameChanged = {},
            onRegister = {}
        )
    }
}

@Composable
fun AuthContent(
    uiState: AuthUiState,
    onUsernameChanged: (String) -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = uiState.usernameInput,
                onValueChange = onUsernameChanged,
                label = { Text("Choose a username") }
            )

            Button(
                onClick = onRegister,
                enabled = uiState.isInputValid && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Join Orbit")
                }
            }
        }
    }
}