package com.ruirui.orbit.android.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.ruirui.orbit.android.theme.OrbitTheme

@Preview
@Composable
fun PermissionRationalePreview() {
    OrbitTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            PermissionRationaleScreen(
                notifGranted = false,
                locationGranted = true,
                backgroundGranted = false,
                onRequestNotification = {},
                onRequestLocation = {},
                onRequestBackground = {}
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    content: @Composable () -> Unit
) {
    // 1. Post Notifications (Android 13+)
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // 2. Fine + Coarse location (request together)
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // 3. Background location (Android 10+, requested after fine/coarse)
    val backgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null

    val notifGranted = notificationPermission?.status?.isGranted ?: true
    val locationGranted = locationPermissions.allPermissionsGranted
    val backgroundGranted = backgroundPermission?.status?.isGranted ?: true
    val allGranted = notifGranted && locationGranted && backgroundGranted

    if (allGranted) {
        content()
        return
    }

    PermissionRationaleScreen(
        notifGranted = notifGranted,
        locationGranted = locationGranted,
        backgroundGranted = backgroundGranted,
        onRequestNotification = { notificationPermission?.launchPermissionRequest() },
        onRequestLocation = { locationPermissions.launchMultiplePermissionRequest() },
        onRequestBackground = { backgroundPermission?.launchPermissionRequest() }
    )
}

@Composable
fun PermissionRationaleScreen(
notifGranted: Boolean,
locationGranted: Boolean,
backgroundGranted: Boolean,
onRequestNotification: () -> Unit,
onRequestLocation: () -> Unit,
onRequestBackground: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Orbit needs a few permissions to share your location with friends.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        if (!notifGranted) {
            PermissionRow(
                title = "Notifications",
                description = "Required to show the background location-sharing notice.",
                onGrant = onRequestNotification
            )
            Spacer(Modifier.height(12.dp))
        }

        // Fine + Coarse location
        if (!locationGranted) {
            PermissionRow(
                title = "Precise Location",
                description = "Needed to read your GPS position and share it with friends.",
                onGrant = onRequestLocation
            )
            Spacer(Modifier.height(12.dp))
        }

        if (locationGranted && !backgroundGranted) {
            PermissionRow(
                title = "Background Location",
                description = "Allows Orbit to keep sharing your location while the app is" +
                " in the background. On the next screen, choose \"Allow all the time\".",
                onGrant = onRequestBackground
            )
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    onGrant: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.height(4.dp))

            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Button(onClick = onGrant) { Text("Grant") }
        }
    }
}