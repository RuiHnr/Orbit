package com.ruirui.orbit.android.ui.friends

import com.ruirui.orbit.models.FriendDto

data class FriendsUiState(
    val friends: List<FriendDto> = emptyList(),
    val pendingRequests: List<FriendDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val addFriendDialogVisible: Boolean = false,
    val outboundUsernameInput: String = "",
    val outboundRequestStatus: String? = null
)

sealed interface FriendsUiIntent {
    data object Refresh : FriendsUiIntent
    data class SendFriendRequest(val username: String) : FriendsUiIntent
    data class AcceptFriendRequest(val friendId: String) : FriendsUiIntent
    data class UpdateUsernameInput(val username: String) : FriendsUiIntent
    data object ToggleAddFriendDialog : FriendsUiIntent
    data object ClearError : FriendsUiIntent
}
