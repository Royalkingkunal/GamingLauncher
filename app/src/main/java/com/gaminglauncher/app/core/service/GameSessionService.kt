package com.gaminglauncher.app.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gaminglauncher.app.GamingApp
import com.gaminglauncher.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameSessionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var sessionJob: Job? = null

    private var activePackageName: String = ""
    private var activeGameName: String = "Active Game"
    private var sessionStartTime: Long = 0L
    private var dndWasEnabled: Boolean = false

    companion object {
        const val ACTION_START_SESSION = "com.gaminglauncher.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.gaminglauncher.action.STOP_SESSION"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_GAME_NAME = "extra_game_name"
        const val EXTRA_ENABLE_DND = "extra_enable_dnd"
        const val NOTIFICATION_ID = 1001

        fun startSession(context: Context, packageName: String, gameName: String, enableDnd: Boolean = false) {
            val intent = Intent(context, GameSessionService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_GAME_NAME, gameName)
                putExtra(EXTRA_ENABLE_DND, enableDnd)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSession(context: Context) {
            val intent = Intent(context, GameSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SESSION -> {
                endSession()
                stopSelf()
            }
            ACTION_START_SESSION -> {
                activePackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                activeGameName = intent.getStringExtra(EXTRA_GAME_NAME) ?: "Gaming Session"
                val requestDnd = intent.getBooleanExtra(EXTRA_ENABLE_DND, false)

                startSession(requestDnd)
            }
        }
        return START_NOT_STICKY
    }

    private fun startSession(requestDnd: Boolean) {
        sessionStartTime = System.currentTimeMillis()

        // 1. Enable Low-Latency Wi-Fi lock
        GamingApp.instance.wifiManager.enableLowLatencyMode()

        // 2. Handle DND if authorized
        if (requestDnd) {
            enableDndMode()
        }

        // 3. Post Foreground Service Notification
        val notification = buildSessionNotification(0L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 4. Start periodic ticker
        sessionJob?.cancel()
        sessionJob = serviceScope.launch {
            while (isActive) {
                delay(3000)
                val elapsedSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000L
                val updatedNotification = buildSessionNotification(elapsedSeconds)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }
    }

    private fun buildSessionNotification(elapsedSeconds: Long): Notification {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val app = GamingApp.instance
        val battery = app.batteryMonitor.getBatteryStatus()
        val wifiDetails = app.wifiManager.getNetworkDetails()

        val resumeIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GameSessionService::class.java).apply {
            action = ACTION_STOP_SESSION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            append("⏱ $timeString | ")
            append("🌡 ${battery.temperatureCelsius}°C | ")
            append(if (wifiDetails.isLowLatencyActive) "⚡ Wi-Fi Boost ON" else "Wi-Fi Normal")
        }

        return NotificationCompat.Builder(this, GamingApp.CHANNEL_ID_GAMING_SESSION)
            .setContentTitle("🎮 Playing: $activeGameName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Session", stopPendingIntent)
            .build()
    }

    private fun enableDndMode() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
            try {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                dndWasEnabled = true
            } catch (_: Exception) {}
        }
    }

    private fun disableDndMode() {
        if (!dndWasEnabled) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
            try {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            } catch (_: Exception) {}
        }
        dndWasEnabled = false
    }

    private fun endSession() {
        sessionJob?.cancel()
        val elapsedSeconds = if (sessionStartTime > 0L) {
            (System.currentTimeMillis() - sessionStartTime) / 1000L
        } else 0L

        // Record session to persistent profile
        if (activePackageName.isNotEmpty() && elapsedSeconds > 5L) {
            GamingApp.instance.gameStorage.recordGameSession(activePackageName, elapsedSeconds)
        }

        // Release Wi-Fi low latency lock
        GamingApp.instance.wifiManager.disableLowLatencyMode()

        // Restore notification policy
        disableDndMode()
    }

    override fun onDestroy() {
        endSession()
        serviceScope.cancel()
        super.onDestroy()
    }
}
