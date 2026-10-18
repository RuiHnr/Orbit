package com.ruirui.orbit.android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ruirui.orbit.android.ui.friends.FriendsScreen
import com.ruirui.orbit.android.ui.friends.FriendsViewModel

@Composable
fun MainScreen(
    friendsViewModel: FriendsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Map) }

    MainContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        friendsScreen = { FriendsScreen(viewModel = friendsViewModel) },
        mapScreen = {
            // placeholder
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Map Screen (Coming Soon)")
            }
        },
        modifier = modifier
    )
}