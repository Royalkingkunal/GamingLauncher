package com.gaminglauncher.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.gaminglauncher.app.core.data.GameDetector
import com.gaminglauncher.app.core.data.GameProfileStorage
import com.gaminglauncher.app.core.data.GameRepository
import com.gaminglauncher.app.core.monitor.BatteryMonitor
import com.gaminglauncher.app.core.monitor.CpuMonitor
import com.gaminglauncher.app.core.monitor.DisplayMonitor
import com.gaminglauncher.app.core.monitor.MemoryMonitor
import com.gaminglauncher.app.core.network.LowLatencyWifiManager
import com.gaminglauncher.app.core.network.PingService

class GamingApp : Application() {

    companion object {
        const val CHANNEL_ID_GAMING_SESSION = "gaming_session_channel"
        const val CHANNEL_ID_OVERLAY = "gaming_overlay_channel"

        lateinit var instance: GamingApp
            private set
    }

    val memoryMonitor: MemoryMonitor by lazy { MemoryMonitor(this) }
    val cpuMonitor: CpuMonitor by lazy { CpuMonitor() }
    val batteryMonitor: BatteryMonitor by lazy { BatteryMonitor(this) }
    val displayMonitor: DisplayMonitor by lazy { DisplayMonitor(this) }
    val pingService: PingService by lazy { PingService() }
    val wifiManager: LowLatencyWifiManager by lazy { LowLatencyWifiManager(this) }
    val gameStorage: GameProfileStorage by lazy { GameProfileStorage(this) }
    val gameDetector: GameDetector by lazy { GameDetector(this, gameStorage) }
    val gameRepository: GameRepository by lazy { GameRepository(this, gameDetector, gameStorage) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            createNotificationChannels()
        } catch (_: Throwable) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            val sessionChannel = NotificationChannel(
                CHANNEL_ID_GAMING_SESSION,
                "Gaming Session Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors gaming performance, battery thermals, and low-latency network locks."
                setShowBadge(false)
            }

            val overlayChannel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                "In-Game Floating HUD",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Active floating HUD overlay for real-time in-game diagnostics."
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(sessionChannel)
            notificationManager.createNotificationChannel(overlayChannel)
        }
    }
}
