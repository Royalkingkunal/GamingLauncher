package com.gaminglauncher.app.core.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

class BatteryMonitor(private val context: Context) {

    data class BatterySnapshot(
        val temperatureCelsius: Float,
        val levelPercent: Int,
        val isCharging: Boolean,
        val thermalStatus: String
    ) {
        val level: Int get() = levelPercent
    }

    fun getBatteryStatus(): BatterySnapshot = getBatterySnapshot()

    fun getBatterySnapshot(): BatterySnapshot {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = try {
                context.registerReceiver(null, intentFilter)
            } catch (_: Throwable) {
                null
            }

            val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempCelsius = if (tempTenths > 0) tempTenths / 10.0f else 35.0f

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val levelPercent = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 80

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val thermalStatus = when {
                tempCelsius <= 37.0f -> "Optimal"
                tempCelsius <= 41.0f -> "Warm"
                tempCelsius <= 45.0f -> "High"
                else -> "Throttling Risk"
            }

            return BatterySnapshot(
                temperatureCelsius = tempCelsius,
                levelPercent = levelPercent,
                isCharging = isCharging,
                thermalStatus = thermalStatus
            )
        } catch (_: Throwable) {
            return BatterySnapshot(
                temperatureCelsius = 35.0f,
                levelPercent = 80,
                isCharging = false,
                thermalStatus = "Optimal"
            )
        }
    }
}
