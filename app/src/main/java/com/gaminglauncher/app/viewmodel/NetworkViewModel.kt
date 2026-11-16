package com.gaminglauncher.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaminglauncher.app.GamingApp
import com.gaminglauncher.app.core.model.NetworkStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkViewModel : ViewModel() {

    private val app: GamingApp get() = GamingApp.instance ?: GamingApp()

    private val _networkStats = MutableStateFlow(NetworkStats())
    val networkStats: StateFlow<NetworkStats> = _networkStats.asStateFlow()

    private val _pingHistory = MutableStateFlow<List<Long>>(emptyList())
    val pingHistory: StateFlow<List<Long>> = _pingHistory.asStateFlow()

    private val _isMonitoring = MutableStateFlow(true)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    init {
        startNetworkMonitoring()
    }

    private fun startNetworkMonitoring() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    if (_isMonitoring.value) {
                        val pingResult = withContext(Dispatchers.IO) {
                            app.pingService.measurePing(1000)
                        }

                        val wifiDetails = app.wifiManager.getNetworkDetails()

                        // Calculate stability score (100 is perfect)
                        val lossPenalty = pingResult.packetLossPercent * 2
                        val jitterPenalty = (pingResult.jitterMs / 3).toInt()
                        val latencyPenalty = if (pingResult.latencyMs > 100) ((pingResult.latencyMs - 100) / 10).toInt() else 0
                        val stability = (100 - lossPenalty - jitterPenalty - latencyPenalty).coerceIn(10, 100)

                        _networkStats.value = NetworkStats(
                            pingMs = pingResult.latencyMs,
                            jitterMs = pingResult.jitterMs,
                            packetLossPercent = pingResult.packetLossPercent,
                            networkType = wifiDetails.type,
                            wifiLinkSpeedMbps = wifiDetails.linkSpeedMbps,
                            wifiFrequencyBand = wifiDetails.frequencyBand,
                            wifiRssi = wifiDetails.rssi,
                            stabilityScore = stability,
                            isLowLatencyLockActive = wifiDetails.isLowLatencyActive,
                            recommendation = wifiDetails.recommendation
                        )

                        // Keep last 20 ping values for sparkline
                        val currentList = _pingHistory.value.toMutableList()
                        if (currentList.size >= 20) currentList.removeAt(0)
                        currentList.add(pingResult.latencyMs)
                        _pingHistory.value = currentList
                    }
                } catch (_: Throwable) {}

                delay(1200)
            }
        }
    }

    fun toggleLowLatencyWifiLock(): Boolean {
        val currentlyActive = _networkStats.value.isLowLatencyLockActive
        return if (currentlyActive) {
            app.wifiManager.disableLowLatencyMode()
            _networkStats.value = _networkStats.value.copy(isLowLatencyLockActive = false)
            false
        } else {
            val success = app.wifiManager.enableLowLatencyMode()
            if (success) {
                _networkStats.value = _networkStats.value.copy(isLowLatencyLockActive = true)
            }
            success
        }
    }

    fun resetStats() {
        app.pingService.resetStatistics()
        _pingHistory.value = emptyList()
    }
}
