package com.gaminglauncher.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

class LowLatencyWifiManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var lowLatencyLock: WifiManager.WifiLock? = null

    data class NetworkDetails(
        val type: String,
        val linkSpeedMbps: Int,
        val frequencyBand: String,
        val rssi: Int,
        val isLowLatencyActive: Boolean,
        val recommendation: String
    )

    fun getNetworkDetails(): NetworkDetails {
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        var linkSpeed = 0
        var freqBand = "N/A"
        var rssi = -100

        val type = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular (5G/LTE)"
            else -> "Offline"
        }

        if (isWifi && wifiManager != null) {
            try {
                @Suppress("DEPRECATION")
                val info: WifiInfo? = wifiManager.connectionInfo
                if (info != null) {
                    linkSpeed = info.linkSpeed
                    rssi = info.rssi
                    val freq = info.frequency
                    freqBand = when {
                        freq in 2400..2500 -> "2.4 GHz"
                        freq in 4900..5900 -> "5.0 GHz (Recommended)"
                        freq >= 5925 -> "6.0 GHz (Wi-Fi 6E/7)"
                        else -> "Wi-Fi"
                    }
                }
            } catch (_: Exception) {}
        }

        val recommendation = when {
            !isWifi && !isCellular -> "No internet connection detected."
            isWifi && freqBand.contains("2.4") ->
                "2.4 GHz band detected. Connect to a 5 GHz network for significantly reduced jitter and lower ping spikes."
            isWifi && linkSpeed in 1..40 ->
                "Weak Wi-Fi link speed ($linkSpeed Mbps). Move closer to your router for optimal gaming performance."
            isWifi ->
                "High-speed 5 GHz Wi-Fi active. Low-latency gaming profile available."
            else ->
                "Mobile data active. Mobile latency varies with cell tower proximity. Wi-Fi is recommended for competitive gaming."
        }

        return NetworkDetails(
            type = type,
            linkSpeedMbps = linkSpeed,
            frequencyBand = freqBand,
            rssi = rssi,
            isLowLatencyActive = lowLatencyLock?.isHeld == true,
            recommendation = recommendation
        )
    }

    /**
     * Enables Android low-latency Wi-Fi mode.
     * Tells device Wi-Fi chipset to minimize power-saving background polling during gaming.
     */
    fun enableLowLatencyMode(): Boolean {
        if (wifiManager == null) return false
        if (lowLatencyLock?.isHeld == true) return true

        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            lowLatencyLock = wifiManager.createWifiLock(mode, "GamingLauncher::LowLatencyLock").apply {
                setReferenceCounted(false)
                acquire()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun disableLowLatencyMode() {
        try {
            if (lowLatencyLock?.isHeld == true) {
                lowLatencyLock?.release()
            }
        } catch (_: Exception) {}
        lowLatencyLock = null
    }
}
