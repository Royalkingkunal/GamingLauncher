package com.gaminglauncher.app.core.model

data class NetworkStats(
    val pingMs: Long = 0L,
    val jitterMs: Long = 0L,
    val packetLossPercent: Int = 0,
    val networkType: String = "Detecting...",
    val wifiLinkSpeedMbps: Int = 0,
    val wifiFrequencyBand: String = "Unknown",
    val wifiRssi: Int = 0,
    val stabilityScore: Int = 100,
    val isLowLatencyLockActive: Boolean = false,
    val recommendation: String = "Testing connection stability..."
)
