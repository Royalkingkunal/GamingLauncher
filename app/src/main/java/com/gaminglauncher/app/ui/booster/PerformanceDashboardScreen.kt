package com.gaminglauncher.app.ui.booster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminglauncher.app.ui.theme.*
import com.gaminglauncher.app.viewmodel.BoosterViewModel

@Composable
fun PerformanceDashboardScreen(
    viewModel: BoosterViewModel,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.metrics.collectAsState()
    val boostMessage by viewModel.boostStatusMessage.collectAsState()
    val isBoosting by viewModel.isBoosting.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "PERFORMANCE ENGINE",
            style = Typography.headlineMedium,
            color = NeonCyan,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Truthful real-time hardware telemetry (non-spoofed)",
            style = Typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Big Ram Boost Action Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM MEMORY",
                            style = Typography.labelSmall,
                            color = NeonCyan
                        )
                        Text(
                            text = "${metrics.usedRamMb} MB / ${metrics.totalRamMb} MB",
                            style = Typography.headlineMedium,
                            color = TextPrimary
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { metrics.ramUsagePercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = if (metrics.ramUsagePercent > 80) AlertRed else NeonCyan,
                            trackColor = AmoledSurface,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "${metrics.ramUsagePercent}%",
                            style = Typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.boostRam() },
                    enabled = !isBoosting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberGreen,
                        disabledContainerColor = AmoledSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isBoosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AmoledBlack,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimizing RAM...", color = AmoledBlack, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Build, contentDescription = null, tint = AmoledBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BOOST SYSTEM RAM", color = AmoledBlack, fontWeight = FontWeight.Bold)
                    }
                }

                AnimatedVisibility(visible = boostMessage != null) {
                    boostMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            color = CyberGreen,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dual Gauge Row: CPU Load & Battery Thermal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CPU LOAD", style = Typography.labelSmall, color = ElectricPurple)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${metrics.cpuLoadPercent}%",
                        style = Typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Active multi-core sampling via /proc/stat",
                        style = Typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Battery Temp Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("BATTERY TEMP", style = Typography.labelSmall, color = CyberGreen)
                        if (metrics.isBatteryCharging) {
                            Text("⚡ Charging", style = Typography.labelSmall, color = CyberGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${metrics.batteryTemperatureC}°C",
                        style = Typography.headlineLarge,
                        color = if (metrics.batteryTemperatureC > 42f) AlertRed else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Level: ${metrics.batteryLevel}% • ${metrics.thermalStatus}",
                        style = Typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display & Refresh Rate Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DISPLAY REFRESH RATE", style = Typography.labelSmall, color = NeonCyan)
                        Text(
                            text = "${metrics.displayRefreshRate.toInt()} Hz Active",
                            style = Typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(AmoledSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SMOOTH MOTION",
                            color = NeonCyan,
                            style = Typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Supported Modes: ${metrics.supportedRefreshRates.joinToString(" • ") { "${it.toInt()}Hz" }}",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Transparency Disclosure
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛡 TRUTHFUL OPTIMIZATION ARCHITECTURE",
                    style = Typography.titleMedium,
                    color = ElectricPurple
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unlike snake-oil boosters that show fake 120 FPS graphs, this engine respects Android's security sandbox. Background memory optimization safely instructs ActivityManager to trim cached non-system processes, leaving full hardware headroom for your game.",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
