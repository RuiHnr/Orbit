package com.ruirui.orbit.android.ui.friends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier


@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    FriendsContent(
        state = state,
        onIntent = viewModel::handleIntent,
        modifier = modifier
    )
}