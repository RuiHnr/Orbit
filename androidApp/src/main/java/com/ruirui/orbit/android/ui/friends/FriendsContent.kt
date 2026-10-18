package com.ruirui.orbit.android.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.ruirui.orbit.android.theme.OrbitTheme
import com.ruirui.orbit.models.FriendDto

@Preview(showBackground = true)
@Composable
fun FriendsContentPreview() {
    // Create sample data
    val sampleFriends = listOf(
        FriendDto(userId = "1", username = "Alice"),
        FriendDto(userId = "2", username = "Bob")
    )

    val sampleRequests = listOf(
        FriendDto(userId = "3", username = "IncomingUser")
    )

    val sampleState = FriendsUiState(
        friends = sampleFriends,
        pendingRequests = sampleRequests,
        isSyncing = false
    )

    // Wrap in MaterialTheme for consistent styling
    OrbitTheme(darkTheme = true) {
        FriendsContent(
            state = sampleState,
            onIntent = {} // No-op for previews
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsContent(
    state: FriendsUiState,
    onIntent: (FriendsUiIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Friends (${state.friends.size})", "Requests (${state.pendingRequests.size})")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                actions = {
                    IconButton(onClick = { onIntent(FriendsUiIntent.Refresh) }) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Friends")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(FriendsUiIntent.ToggleAddFriendDialog) }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            state.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onIntent(FriendsUiIntent.ClearError) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> FriendsListContent(friends = state.friends)
                1 -> RequestsListContent(
                    requests = state.pendingRequests,
                    onAccept = { friendId -> onIntent(FriendsUiIntent.AcceptFriendRequest(friendId)) }
                )
            }
        }

        if (state.addFriendDialogVisible) {
            AddFriendDialog(
                usernameInput = state.outboundUsernameInput,
                isLoading = state.isLoading,
                statusMessage = state.outboundRequestStatus,
                onUsernameChanged = { username -> onIntent(FriendsUiIntent.UpdateUsernameInput(username)) },
                onDismiss = { onIntent(FriendsUiIntent.ToggleAddFriendDialog) },
                onSubmit = { onIntent(FriendsUiIntent.SendFriendRequest(state.outboundUsernameInput)) }
            )
        }
    }
}