package com.gaminglauncher.app.core.monitor

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

class DisplayMonitor(private val context: Context) {

    data class DisplayCapabilities(
        val currentRefreshRate: Float,
        val supportedRefreshRates: List<Float>,
        val resolutionWidth: Int,
        val resolutionHeight: Int
    )

    fun getDisplayRefreshRateInfo(): DisplayCapabilities = getDisplayCapabilities()

    fun getDisplayCapabilities(): DisplayCapabilities {
        try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display: Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

            val mode = display?.mode
            val currentRate = mode?.refreshRate ?: 60f
            val width = mode?.physicalWidth ?: 1080
            val height = mode?.physicalHeight ?: 2400

            val supportedModes = display?.supportedModes ?: emptyArray()
            val supportedRates = supportedModes.map { it.refreshRate }
                .filter { it > 0f }
                .distinct()
                .sorted()
                .ifEmpty { listOf(60f) }

            return DisplayCapabilities(
                currentRefreshRate = if (currentRate > 0f) currentRate else 60f,
                supportedRefreshRates = supportedRates,
                resolutionWidth = width,
                resolutionHeight = height
            )
        } catch (_: Throwable) {
            return DisplayCapabilities(
                currentRefreshRate = 60f,
                supportedRefreshRates = listOf(60f),
                resolutionWidth = 1080,
                resolutionHeight = 2400
            )
        }
    }
}
