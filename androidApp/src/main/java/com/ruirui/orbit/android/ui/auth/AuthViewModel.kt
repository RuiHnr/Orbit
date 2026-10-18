package com.ruirui.orbit.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ruirui.orbit.network.ApiException
import com.ruirui.orbit.repository.AuthRepository
import com.ruirui.orbit.repository.AuthState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Pass-through to global auth state from KMP
    val authState: StateFlow<AuthState> = authRepository.authState

    private var checkUsernameJob: Job? = null

    fun onUsernameChanged(newUsername: String) {
        _uiState.update {
            it.copy(
                usernameInput = newUsername,
                errorMessage = null,
                isUsernameAvailable = null,
                isCheckingUsername = false
            )
        }

        checkUsernameJob?.cancel()

        val trimmed = newUsername.trim()
        if (trimmed.length < 3 || !trimmed.all { it.isLetterOrDigit() || it == '_' }) {
            return
        }

        // Debounce network check by 500ms
        checkUsernameJob = viewModelScope.launch {
            delay(500.milliseconds)
            _uiState.update { it.copy(isCheckingUsername = true) }

            authRepository.checkUsernameAvailability(trimmed)
                .onSuccess { available ->
                    _uiState.update { it.copy(
                        isCheckingUsername = false,
                        isUsernameAvailable = available
                    ) }
                }
                .onFailure {
                    _uiState.update { it.copy(isCheckingUsername = false) }
                }
        }
    }

    fun onRegister() {
        val username = _uiState.value.usernameInput.trim()
        if (!_uiState.value.isInputValid) {
            _uiState.update { it.copy(errorMessage = "Username must be 3-24 alphanumeric characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.register(username)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                },
                onFailure = { error ->
                    val message = when {
                        error is ApiException && error.statusCode == 409 ->
                            "Username is already taken. Please choose another."
                        else ->
                            error.message ?: "Registration failed. Please try again."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}