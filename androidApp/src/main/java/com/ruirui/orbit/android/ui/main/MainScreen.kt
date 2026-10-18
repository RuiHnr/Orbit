package com.ruirui.orbit.android.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ruirui.orbit.android.ui.friends.FriendsScreen
import com.ruirui.orbit.android.ui.friends.FriendsViewModel
import com.ruirui.orbit.android.ui.map.MapScreen
import com.ruirui.orbit.android.ui.map.MapViewModel

@Composable
fun MainScreen(
    friendsViewModel: FriendsViewModel,
    mapViewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Map) }

    MainContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        friendsScreen = { FriendsScreen(viewModel = friendsViewModel) },
        mapScreen = { MapScreen(mapViewModel) },
        modifier = modifier
    )
}