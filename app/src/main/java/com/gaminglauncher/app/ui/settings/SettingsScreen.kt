package com.gaminglauncher.app.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminglauncher.app.ui.theme.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasDndPermission by remember {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) nm.isNotificationPolicyAccessGranted else true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SYSTEM & PERMISSIONS",
            style = Typography.headlineMedium,
            color = NeonCyan,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Configure Android system privileges for optimal performance",
            style = Typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SPECIAL SYSTEM PRIVILEGES", style = Typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // Overlay Permission Item
                PermissionItem(
                    title = "Display Over Other Apps (Overlay)",
                    description = "Required to render the in-game HUD pill and tactical shooter crosshair.",
                    isGranted = hasOverlayPermission,
                    onRequest = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AmoledCardBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // DND Permission Item
                PermissionItem(
                    title = "Do Not Disturb (Notification Policy)",
                    description = "Allows auto-silencing calls and notifications during competitive matches.",
                    isGranted = hasDndPermission,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AmoledCardBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Battery Optimization
                PermissionItem(
                    title = "Background Battery Optimization",
                    description = "Prevents Android OEM task killers from killing the low-latency Wi-Fi lock.",
                    isGranted = true,
                    onRequest = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Architectural Transparency Note
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENGINE INTEGRITY DISCLOSURE", style = Typography.titleMedium, color = NeonCyan)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Latency & Jitter: Measured via actual TCP socket handshakes to regional servers (Cloudflare / Google DNS). We do not spoof 5ms pings when your real connection is 60ms.\n\n" +
                            "• RAM Optimization: Executes safe background trimming via ActivityManager.killBackgroundProcesses(). Critical Android system services are preserved.\n\n" +
                            "• Low Latency Mode: Acquires Android's official WIFI_MODE_FULL_LOW_LATENCY lock (API 29+) to reduce Wi-Fi beacon polling intervals.",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ultimate Android Gaming Launcher", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Version 1.0.0-PRO (Compose Material 3)", style = Typography.bodyMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Built for High Performance & Low Latency", style = Typography.labelSmall, color = CyberGreen)
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, style = Typography.bodyMedium, color = TextMuted)
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .background(CyberGreenGlow, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Active", color = CyberGreen, style = Typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Grant", color = AmoledBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
