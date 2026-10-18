package com.ruirui.orbit.android.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ruirui.orbit.android.theme.OrbitTheme

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    OrbitTheme(darkTheme = false) {
        MainContent(
            selectedTab = MainTab.Friends,
            onTabSelected = {},
            friendsScreen = { Text("Friends Preview") },
            mapScreen = { Text("Map Preview") }
        )
    }
}
@Composable
fun MainContent(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    friendsScreen: @Composable () -> Unit,
    mapScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.Map -> mapScreen()
                MainTab.Friends -> friendsScreen()
            }
        }
    }
}