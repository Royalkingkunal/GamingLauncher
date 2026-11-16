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

    lateinit var memoryMonitor: MemoryMonitor
        private set
    lateinit var cpuMonitor: CpuMonitor
        private set
    lateinit var batteryMonitor: BatteryMonitor
        private set
    lateinit var displayMonitor: DisplayMonitor
        private set
    lateinit var pingService: PingService
        private set
    lateinit var wifiManager: LowLatencyWifiManager
        private set
    lateinit var gameStorage: GameProfileStorage
        private set
    lateinit var gameDetector: GameDetector
        private set
    lateinit var gameRepository: GameRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            createNotificationChannels()
        } catch (_: Throwable) {}

        memoryMonitor = MemoryMonitor(this)
        cpuMonitor = CpuMonitor()
        batteryMonitor = BatteryMonitor(this)
        displayMonitor = DisplayMonitor(this)
        pingService = PingService()
        wifiManager = LowLatencyWifiManager(this)
        gameStorage = GameProfileStorage(this)
        gameDetector = GameDetector(this, gameStorage)
        gameRepository = GameRepository(this, gameDetector, gameStorage)
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
