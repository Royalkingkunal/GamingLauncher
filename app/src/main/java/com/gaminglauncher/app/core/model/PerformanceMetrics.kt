package com.gaminglauncher.app.core.model

data class PerformanceMetrics(
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val usedRamMb: Long = 0L,
    val ramUsagePercent: Int = 0,
    val cpuLoadPercent: Int = 0,
    val batteryTemperatureC: Float = 0f,
    val batteryLevel: Int = 0,
    val isBatteryCharging: Boolean = false,
    val displayRefreshRate: Float = 60f,
    val supportedRefreshRates: List<Float> = listOf(60f),
    val thermalStatus: String = "Normal"
)
