package com.gaminglauncher.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gaminglauncher.app.ui.booster.PerformanceDashboardScreen
import com.gaminglauncher.app.ui.hud.TouchControlsScreen
import com.gaminglauncher.app.ui.launcher.GameLibraryScreen
import com.gaminglauncher.app.ui.navigation.Screen
import com.gaminglauncher.app.ui.network.NetworkBoosterScreen
import com.gaminglauncher.app.ui.settings.SettingsScreen
import com.gaminglauncher.app.ui.theme.*
import com.gaminglauncher.app.viewmodel.BoosterViewModel
import com.gaminglauncher.app.viewmodel.HudViewModel
import com.gaminglauncher.app.viewmodel.LauncherViewModel
import com.gaminglauncher.app.viewmodel.NetworkViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            checkNotificationPermission()
        } catch (_: Throwable) {}

        try {
            setContent {
                GamingLauncherTheme {
                    val app = GamingApp.instance

                    val launcherViewModel = remember { LauncherViewModel(app.gameRepository) }
                    val boosterViewModel = remember { BoosterViewModel() }
                    val networkViewModel = remember { NetworkViewModel() }
                    val hudViewModel = remember { HudViewModel() }

                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = AmoledBlack,
                        bottomBar = {
                            NavigationBar(
                                containerColor = AmoledDarkCard,
                                tonalElevation = 8.dp
                            ) {
                                Screen.items.forEach { screen ->
                                    val selected = currentScreen == screen
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(
                                                screen.icon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                screen.title,
                                                fontSize = 10.sp,
                                                color = if (selected) NeonCyan else TextMuted
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonCyan,
                                            unselectedIconColor = TextMuted,
                                            indicatorColor = AmoledSurface
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Crossfade(
                            targetState = currentScreen,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                Screen.Library -> GameLibraryScreen(viewModel = launcherViewModel)
                                Screen.Booster -> PerformanceDashboardScreen(viewModel = boosterViewModel)
                                Screen.Network -> NetworkBoosterScreen(viewModel = networkViewModel)
                                Screen.TouchHud -> TouchControlsScreen(viewModel = hudViewModel)
                                Screen.Settings -> SettingsScreen()
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            val tv = android.widget.TextView(this).apply {
                text = "Gaming Launcher Ready"
                setTextColor(android.graphics.Color.CYAN)
                setBackgroundColor(android.graphics.Color.BLACK)
                textSize = 18f
                setPadding(50, 100, 50, 50)
            }
            setContentView(tv)
        }
    }

    private fun checkNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } catch (_: Throwable) {}
    }
}
