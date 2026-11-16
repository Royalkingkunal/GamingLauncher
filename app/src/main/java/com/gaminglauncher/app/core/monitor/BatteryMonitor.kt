package com.gaminglauncher.app.core.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

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
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10.0f

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val levelPercent = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 0

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
    }
}
