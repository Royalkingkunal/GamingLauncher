package com.gaminglauncher.app.core.monitor

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

class DisplayMonitor(private val context: Context) {

    data class DisplayCapabilities(
        val currentRefreshRate: Float,
        val supportedRefreshRates: List<Float>,
        val resolutionWidth: Int,
        val resolutionHeight: Int
    )

    fun getDisplayRefreshRateInfo(): DisplayCapabilities = getDisplayCapabilities()

    fun getDisplayCapabilities(): DisplayCapabilities {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        @Suppress("DEPRECATION")
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display
            } catch (_: Exception) {
                windowManager?.defaultDisplay
            }
        } else {
            windowManager?.defaultDisplay
        }

        val mode = display?.mode
        val currentRate = mode?.refreshRate ?: 60f
        val width = mode?.physicalWidth ?: 1080
        val height = mode?.physicalHeight ?: 2400

        val supportedModes = display?.supportedModes ?: emptyArray()
        val supportedRates = supportedModes.map { it.refreshRate }
            .distinct()
            .sorted()
            .ifEmpty { listOf(60f) }

        return DisplayCapabilities(
            currentRefreshRate = currentRate,
            supportedRefreshRates = supportedRates,
            resolutionWidth = width,
            resolutionHeight = height
        )
    }
}
