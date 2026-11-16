package com.gaminglauncher.app.core.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class MemoryMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    data class MemorySnapshot(
        val totalMb: Long,
        val availableMb: Long,
        val usedMb: Long,
        val usagePercentage: Int
    ) {
        val percentageUsed: Int get() = usagePercentage
        val availMb: Long get() = availableMb
    }

    data class BoostResult(
        val freedRamMb: Long,
        val killedProcessCount: Int,
        val currentAvailableMb: Long
    )

    fun getMemorySnapshot(): MemorySnapshot {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalBytes = memoryInfo.totalMem
        val availBytes = memoryInfo.availMem
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)

        val totalMb = totalBytes / (1024 * 1024)
        val availMb = availBytes / (1024 * 1024)
        val usedMb = usedBytes / (1024 * 1024)
        val percentage = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

        return MemorySnapshot(
            totalMb = totalMb,
            availableMb = availMb,
            usedMb = usedMb,
            usagePercentage = percentage.coerceIn(0, 100)
        )
    }

    /**
     * Safely terminates non-system background applications to release RAM.
     * Respects Android API boundaries using standard ActivityManager.killBackgroundProcesses.
     */
    fun boostMemory(): BoostResult {
        val before = getMemorySnapshot().availableMb
        val pm = context.packageManager
        var killedCount = 0

        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                // Never target system apps or our own launcher
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem && app.packageName != context.packageName) {
                    activityManager?.killBackgroundProcesses(app.packageName)
                    killedCount++
                }
            }
        } catch (_: Exception) {}

        System.gc()
        val after = getMemorySnapshot().availableMb
        val freedMb = (after - before).coerceAtLeast(0L)
        return BoostResult(freedMb, killedCount, after)
    }

    fun performSafeBoost(): Long {
        return boostMemory().freedRamMb
    }
}
