package com.gaminglauncher.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Library : Screen("library", "Games", Icons.Default.PlayArrow)
    object Booster : Screen("booster", "Booster", Icons.Default.Build)
    object Network : Screen("network", "Network", Icons.Default.Share)
    object TouchHud : Screen("touch_hud", "Touch HUD", Icons.Default.Info)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Library, Booster, Network, TouchHud, Settings)
    }
}
