package com.ruirui.orbit.android.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ruirui.orbit.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val friendsRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        observeRepository()
        refreshFriends()
    }

    fun handleIntent(intent: FriendsUiIntent) {
        when (intent) {
            is FriendsUiIntent.AcceptFriendRequest -> acceptRequest(intent.friendId)
            is FriendsUiIntent.Refresh -> refreshFriends()
            is FriendsUiIntent.SendFriendRequest -> sendRequest(intent.username)
            is FriendsUiIntent.ToggleAddFriendDialog -> _uiState.update {
                it.copy(addFriendDialogVisible = !it.addFriendDialogVisible, outboundUsernameInput = "", outboundRequestStatus = null)
            }
            is FriendsUiIntent.UpdateUsernameInput -> _uiState.update { it.copy(outboundUsernameInput = intent.username) }
            is FriendsUiIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            friendsRepository.friends.collect { friendsList ->
                _uiState.update { it.copy(friends = friendsList) }
            }
        }
        viewModelScope.launch {
            friendsRepository.friendRequests.collect { requestsList ->
                _uiState.update { it.copy(pendingRequests = requestsList) }
            }
        }
    }

    private fun refreshFriends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            friendsRepository.syncFriends()
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage ?: "Failed to sync friends.") }
                }
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    private fun acceptRequest(friendId: String) {
        viewModelScope.launch {
            friendsRepository.acceptFriendRequest(friendId)
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = "Failed to accept request: ${error.localizedMessage}") }
                }
        }
    }

    private fun sendRequest(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, outboundRequestStatus = null) }
            friendsRepository.sendFriendRequest(username.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            outboundRequestStatus = "Request sent successfully!",
                            outboundUsernameInput = ""
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            outboundRequestStatus = "Error: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    // In FriendsViewModel.kt
    companion object {
        fun provideFactory(friendsRepository: FriendRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FriendsViewModel(friendsRepository) as T
                }
            }
    }
}