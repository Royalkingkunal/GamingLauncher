package com.gaminglauncher.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaminglauncher.app.GamingApp
import com.gaminglauncher.app.core.model.PerformanceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BoosterViewModel : ViewModel() {

    private val app = GamingApp.instance

    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private val _boostStatusMessage = MutableStateFlow<String?>(null)
    val boostStatusMessage: StateFlow<String?> = _boostStatusMessage.asStateFlow()

    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    init {
        startHardwarePolling()
    }

    private fun startHardwarePolling() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val displayInfo = app.displayMonitor.getDisplayRefreshRateInfo()
                    val mem = app.memoryMonitor.getMemorySnapshot()
                    val cpu = app.cpuMonitor.sampleCpuUsage()
                    val battery = app.batteryMonitor.getBatteryStatus()

                    _metrics.value = PerformanceMetrics(
                        totalRamMb = mem.totalMb,
                        availableRamMb = mem.availMb,
                        usedRamMb = mem.usedMb,
                        ramUsagePercent = mem.usagePercentage,
                        cpuLoadPercent = cpu,
                        batteryTemperatureC = battery.temperatureCelsius,
                        batteryLevel = battery.level,
                        isBatteryCharging = battery.isCharging,
                        displayRefreshRate = displayInfo.currentRefreshRate,
                        supportedRefreshRates = displayInfo.supportedRefreshRates,
                        thermalStatus = battery.thermalStatus
                    )
                } catch (_: Throwable) {}

                delay(1500)
            }
        }
    }

    fun boostRam() {
        viewModelScope.launch {
            _isBoosting.value = true
            _boostStatusMessage.value = "Analyzing background processes..."
            delay(400)

            val freed = withContext(Dispatchers.IO) {
                app.memoryMonitor.boostMemory()
            }

            _isBoosting.value = false
            _boostStatusMessage.value = if (freed.freedRamMb > 0) {
                "✓ Optimized: Freed ~${freed.freedRamMb}MB across ${freed.killedProcessCount} processes"
            } else {
                "✓ Memory is already optimal (${freed.currentAvailableMb}MB free)"
            }

            delay(3500)
            _boostStatusMessage.value = null
        }
    }
}
