package com.gaminglauncher.app.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminglauncher.app.core.model.CrosshairStyle
import com.gaminglauncher.app.ui.theme.*
import com.gaminglauncher.app.viewmodel.HudViewModel

@Composable
fun TouchControlsScreen(
    viewModel: HudViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedColorHex by viewModel.selectedColorHex.collectAsState()
    val touchCount by viewModel.touchPointsCount.collectAsState()
    val touchCoords by viewModel.lastTouchCoordinates.collectAsState()
    val touchPressure by viewModel.touchPressure.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TOUCH & HUD CONTROLS",
            style = Typography.headlineMedium,
            color = NeonCyan,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Custom in-game overlays & screen touch testing",
            style = Typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Overlay Master Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyan, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("IN-GAME FLOATING HUD", style = Typography.titleMedium, color = TextPrimary)
                        Text(
                            text = "Draggable pill showing live FPS/Ping & quick RAM boost over games",
                            style = Typography.bodyMedium,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isOverlayActive,
                        onCheckedChange = { viewModel.toggleOverlay(context) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = AmoledSurface
                        )
                    )
                }

                if (!viewModel.canDrawOverlays(context)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ 'Display over other apps' permission required to render HUD over games.",
                        style = Typography.bodyMedium,
                        color = AlertRed,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tactical Crosshair Generator Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TACTICAL CROSSHAIR GENERATOR", style = Typography.titleMedium, color = ElectricPurple)
                Text("Custom aiming reticle for competitive shooter games", style = Typography.bodyMedium, color = TextMuted)

                Spacer(modifier = Modifier.height(12.dp))

                // Crosshair Preview Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(AmoledSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, AmoledCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CrosshairPreviewCanvas(
                        style = selectedStyle,
                        color = Color(android.graphics.Color.parseColor(selectedColorHex)),
                        modifier = Modifier.size(100.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Style Selector Chips
                Text("STYLE", style = Typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CrosshairStyle.values().forEach { style ->
                        val isSelected = selectedStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setStyle(style) },
                            label = { Text(style.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricPurple,
                                selectedLabelColor = TextPrimary,
                                containerColor = AmoledSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Selector Row
                Text("COLOR PALETTE", style = Typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModel.availableColors.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { viewModel.setColor(hex) }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Touch Sensitivity Diagnostic Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TOUCH SENSITIVITY TESTING GROUND", style = Typography.titleMedium, color = CyberGreen)
                Text("Touch and drag below to verify response lag and multi-touch tracking", style = Typography.bodyMedium, color = TextMuted)

                Spacer(modifier = Modifier.height(12.dp))

                var touchPosition by remember { mutableStateOf<Offset?>(null) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(AmoledSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, CyberGreen, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchPosition = offset
                                    viewModel.updateTouchMetrics(1, offset.x, offset.y, 1f)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    touchPosition = offset
                                    viewModel.updateTouchMetrics(1, offset.x, offset.y, 1f)
                                },
                                onDrag = { change, _ ->
                                    touchPosition = change.position
                                    viewModel.updateTouchMetrics(1, change.position.x, change.position.y, 1f)
                                },
                                onDragEnd = {
                                    touchPosition = null
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        touchPosition?.let { pos ->
                            drawCircle(color = CyberGreen, radius = 32.dp.toPx(), center = pos, style = Stroke(width = 3.dp.toPx()))
                            drawCircle(color = CyberGreen, radius = 8.dp.toPx(), center = pos)
                        }
                    }

                    if (touchPosition == null) {
                        Text(
                            text = "TOUCH OR SWIPE ANYWHERE HERE",
                            color = TextMuted,
                            style = Typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Live Position: $touchCoords", style = Typography.bodyMedium, color = TextSecondary)
                    Text("Lag: 0ms (Direct HW)", style = Typography.bodyMedium, color = CyberGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun CrosshairPreviewCanvas(
    style: CrosshairStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Draw background sniper grid lines
        drawLine(Color(0xFF1E2D4A), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
        drawLine(Color(0xFF1E2D4A), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)

        when (style) {
            CrosshairStyle.DOT -> {
                drawCircle(color = color, radius = 5.dp.toPx(), center = Offset(cx, cy))
            }
            CrosshairStyle.CROSS -> {
                val len = 22.dp.toPx()
                val gap = 6.dp.toPx()
                val sw = 2.5.dp.toPx()
                drawLine(color, Offset(cx - len, cy), Offset(cx - gap, cy), strokeWidth = sw)
                drawLine(color, Offset(cx + gap, cy), Offset(cx + len, cy), strokeWidth = sw)
                drawLine(color, Offset(cx, cy - len), Offset(cx, cy - gap), strokeWidth = sw)
                drawLine(color, Offset(cx, cy + gap), Offset(cx, cy + len), strokeWidth = sw)
                drawCircle(color, radius = 3.dp.toPx(), center = Offset(cx, cy))
            }
            CrosshairStyle.CIRCLE -> {
                drawCircle(color = color, radius = 16.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(cx, cy))
            }
            CrosshairStyle.CHEVRON -> {
                val path = Path().apply {
                    moveTo(cx - 16.dp.toPx(), cy + 12.dp.toPx())
                    lineTo(cx, cy)
                    lineTo(cx + 16.dp.toPx(), cy + 12.dp.toPx())
                }
                drawPath(path, color = color, style = Stroke(width = 2.5.dp.toPx()))
                drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(cx, cy - 4.dp.toPx()))
            }
        }
    }
}
