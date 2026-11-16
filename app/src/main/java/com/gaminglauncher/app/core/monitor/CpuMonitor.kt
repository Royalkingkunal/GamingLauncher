package com.gaminglauncher.app.core.monitor

import java.io.RandomAccessFile

class CpuMonitor {

    private var previousTotalTime: Long = 0L
    private var previousIdleTime: Long = 0L

    /**
     * Reads /proc/stat to compute the true multi-core CPU load percentage.
     * Falls back to active runtime processor count heuristics if /proc is restricted by OEM SELinux.
     */
    fun sampleCpuUsage(): Int = getCpuLoadPercentage()

    fun getCpuLoadPercentage(): Int {
        try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine()
            reader.close()

            if (line != null && line.startsWith("cpu ")) {
                val parts = line.split("\\s+".toRegex()).drop(1)
                if (parts.size >= 4) {
                    val user = parts[0].toLongOrNull() ?: 0L
                    val nice = parts[1].toLongOrNull() ?: 0L
                    val system = parts[2].toLongOrNull() ?: 0L
                    val idle = parts[3].toLongOrNull() ?: 0L
                    val iowait = if (parts.size > 4) parts[4].toLongOrNull() ?: 0L else 0L
                    val irq = if (parts.size > 5) parts[5].toLongOrNull() ?: 0L else 0L
                    val softirq = if (parts.size > 6) parts[6].toLongOrNull() ?: 0L else 0L

                    val currentIdle = idle + iowait
                    val currentTotal = user + nice + system + currentIdle + irq + softirq

                    val diffTotal = currentTotal - previousTotalTime
                    val diffIdle = currentIdle - previousIdleTime

                    previousTotalTime = currentTotal
                    previousIdleTime = currentIdle

                    if (diffTotal > 0) {
                        val usage = ((diffTotal - diffIdle).toDouble() / diffTotal.toDouble() * 100).toInt()
                        return usage.coerceIn(0, 100)
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: estimate from thread usage and core counts
        val cores = Runtime.getRuntime().availableProcessors()
        val load = ((Thread.activeCount().toDouble() / (cores * 8).toDouble()) * 100).toInt()
        return load.coerceIn(5, 85)
    }
}
