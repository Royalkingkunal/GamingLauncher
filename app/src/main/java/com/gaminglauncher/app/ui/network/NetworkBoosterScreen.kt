package com.gaminglauncher.app.ui.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminglauncher.app.ui.theme.*
import com.gaminglauncher.app.viewmodel.NetworkViewModel

@Composable
fun NetworkBoosterScreen(
    viewModel: NetworkViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.networkStats.collectAsState()
    val history by viewModel.pingHistory.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NETWORK BOOSTER",
                    style = Typography.headlineMedium,
                    color = NeonCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Real TCP socket handshake latency & jitter",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { viewModel.resetStats() },
                modifier = Modifier
                    .size(40.dp)
                    .background(AmoledDarkCard, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Stats", tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Ping & Stability Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("REAL-TIME LATENCY", style = Typography.labelSmall, color = NeonCyan)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${stats.pingMs}",
                                style = Typography.headlineLarge,
                                color = when {
                                    stats.pingMs < 45 -> CyberGreen
                                    stats.pingMs < 90 -> NeonCyan
                                    else -> AlertRed
                                },
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms",
                                style = Typography.titleMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("STABILITY SCORE", style = Typography.labelSmall, color = TextMuted)
                        Text(
                            text = "${stats.stabilityScore}/100",
                            style = Typography.titleLarge,
                            color = when {
                                stats.stabilityScore >= 80 -> CyberGreen
                                stats.stabilityScore >= 50 -> NeonCyan
                                else -> AlertRed
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                stats.stabilityScore >= 80 -> "Competitive Ready"
                                stats.stabilityScore >= 50 -> "Acceptable"
                                else -> "High Jitter"
                            },
                            style = Typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-Time Ping Sparkline Graph
                Text("LATENCY TIMELINE (ROLLING TCP SAMPLES)", style = Typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                PingSparklineCanvas(
                    samples = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(AmoledSurface, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Jitter & Packet Loss Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("JITTER (VARIANCE)", style = Typography.labelSmall, color = TextMuted)
                        Text("±${stats.jitterMs} ms", style = Typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PACKET LOSS", style = Typography.labelSmall, color = TextMuted)
                        Text("${stats.packetLossPercent}%", style = Typography.titleMedium, color = if (stats.packetLossPercent > 0) AlertRed else CyberGreen, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("NETWORK", style = Typography.labelSmall, color = TextMuted)
                        Text(stats.networkType, style = Typography.titleMedium, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wi-Fi Low-Latency Mode Switch Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LOW-LATENCY WI-FI LOCK", style = Typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Instructs Wi-Fi chipset to disable power-saving polling delays",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = stats.isLowLatencyLockActive,
                        onCheckedChange = { viewModel.toggleLowLatencyWifiLock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberGreen,
                            checkedTrackColor = AmoledSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = AmoledCardBorder, thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wi-Fi Link Speed: ${stats.wifiLinkSpeedMbps} Mbps", style = Typography.bodyMedium, color = TextSecondary)
                    Text("Band: ${stats.wifiFrequencyBand}", style = Typography.bodyMedium, color = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Smart Recommendation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMART NETWORK DIAGNOSIS", style = Typography.titleMedium, color = ElectricPurple)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stats.recommendation,
                    style = Typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun PingSparklineCanvas(
    samples: List<Long>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
        if (samples.size < 2) return@Canvas

        val maxPing = (samples.maxOrNull() ?: 100L).coerceAtLeast(60L).toFloat()
        val minPing = (samples.minOrNull() ?: 0L).coerceAtMost(20L).toFloat()
        val range = (maxPing - minPing).coerceAtLeast(10f)

        val stepX = size.width / (samples.size - 1)
        val path = Path()

        samples.forEachIndexed { index, ping ->
            val normalizedY = 1f - ((ping - minPing) / range)
            val x = index * stepX
            val y = normalizedY * size.height

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points
        samples.forEachIndexed { index, ping ->
            val normalizedY = 1f - ((ping - minPing) / range)
            val x = index * stepX
            val y = normalizedY * size.height
            drawCircle(
                color = if (ping > 90) AlertRed else CyberGreen,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
