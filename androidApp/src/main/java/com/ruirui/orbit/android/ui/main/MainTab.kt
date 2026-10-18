package com.ruirui.orbit.android.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(val title: String, val icon: ImageVector) {
    Map("Map", Icons.Default.Map),
    Friends("Friends", Icons.Default.People)
}