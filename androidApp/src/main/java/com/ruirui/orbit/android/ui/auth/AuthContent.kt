package com.ruirui.orbit.android.ui.auth

import com.ruirui.orbit.android.ui.components.ErrorMessageCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruirui.orbit.android.theme.OrbitTheme

// 1. Interactive Preview (Light Mode)
@Preview(showBackground = true)
@Composable
fun AuthScreenPreviewLight() {
    val sampleState = AuthUiState(
        usernameInput = "alice",
        isLoading = false,
        errorMessage = "Error occurred",
        isCheckingUsername = false,
        isUsernameAvailable = true
    )

    OrbitTheme(darkTheme = true) {
        AuthContent(
            uiState = sampleState,
            onUsernameChanged = {},
            onDismissError = {},
            onRegister = {}
        )
    }
}

@Composable
fun AuthContent(
    uiState: AuthUiState,
    onUsernameChanged: (String) -> Unit,
    onRegister: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            OutlinedTextField(
                value = uiState.usernameInput,
                onValueChange = onUsernameChanged,
                label = { Text("Choose a username") },
                singleLine = true,
                isError = uiState.isUsernameAvailable == false,
                trailingIcon = {
                    if (uiState.isCheckingUsername) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (uiState.isUsernameAvailable == true) {
                        Icon(Icons.Default.Check,
                            contentDescription = "Available",
                            tint = MaterialTheme.colorScheme.primary
                            )
                    } else if (uiState.isUsernameAvailable == false) {
                        Icon(Icons.Default.Close,
                            contentDescription = "Unavailable",
                            tint = MaterialTheme.colorScheme.error
                            )
                    }
                }
            )

            Button(
                onClick = onRegister,
                enabled = uiState.isInputValid && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Register")
                }
            }

            uiState.errorMessage?.let { error ->
                ErrorMessageCard(
                    errorMessage = error,
                    onDismiss = onDismissError
                )
            }
        }
    }
}