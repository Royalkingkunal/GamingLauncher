package com.gaminglauncher.app.core.model

import android.graphics.drawable.Drawable

data class GameInfo(
    val packageName: String,
    val name: String,
    val isAutoDetectedGame: Boolean = true,
    val isUserFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val totalPlayDurationSeconds: Long = 0L,
    val performancePreset: PerformancePreset = PerformancePreset.BEAST_MODE,
    val enableDnd: Boolean = true,
    val enableCrosshair: Boolean = false,
    val crosshairStyle: CrosshairStyle = CrosshairStyle.DOT,
    val crosshairColorHex: String = "#00F2FE",
    @Transient val icon: Drawable? = null
)

enum class PerformancePreset(val title: String, val description: String) {
    BATTERY_SAVER("Eco Saver", "Limits background tasks and targets stable 60Hz"),
    BALANCED("Balanced", "Smooth gaming performance with balanced thermal regulation"),
    BEAST_MODE("Beast Mode", "Unlocks maximum refresh rate and prioritizes network latency")
}

enum class CrosshairStyle(val displayName: String) {
    DOT("Center Dot"),
    CROSS("Tactical Cross"),
    CIRCLE("Circle Dot"),
    CHEVRON("Chevron Arrow")
}
