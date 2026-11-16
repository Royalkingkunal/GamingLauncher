package com.gaminglauncher.app.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.gaminglauncher.app.core.model.CrosshairStyle
import com.gaminglauncher.app.core.service.GamingOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HudViewModel : ViewModel() {

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _selectedStyle = MutableStateFlow(CrosshairStyle.DOT)
    val selectedStyle: StateFlow<CrosshairStyle> = _selectedStyle.asStateFlow()

    private val _selectedColorHex = MutableStateFlow("#00F2FE")
    val selectedColorHex: StateFlow<String> = _selectedColorHex.asStateFlow()

    private val _touchPointsCount = MutableStateFlow(0)
    val touchPointsCount: StateFlow<Int> = _touchPointsCount.asStateFlow()

    private val _lastTouchCoordinates = MutableStateFlow("X: --  Y: --")
    val lastTouchCoordinates: StateFlow<String> = _lastTouchCoordinates.asStateFlow()

    private val _touchPressure = MutableStateFlow(0f)
    val touchPressure: StateFlow<Float> = _touchPressure.asStateFlow()

    val availableColors = listOf(
        "#00F2FE", // Cyan
        "#00F5A0", // Cyber Green
        "#FF0055", // Neon Red
        "#9D4EDD", // Electric Purple
        "#FFD600"  // Gold
    )

    fun setStyle(style: CrosshairStyle) {
        _selectedStyle.value = style
    }

    fun setColor(colorHex: String) {
        _selectedColorHex.value = colorHex
    }

    fun updateTouchMetrics(pointerCount: Int, x: Float, y: Float, pressure: Float) {
        _touchPointsCount.value = pointerCount
        _lastTouchCoordinates.value = String.format("X: %.1f  Y: %.1f", x, y)
        _touchPressure.value = pressure
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun toggleOverlay(context: Context): Boolean {
        if (!canDrawOverlays(context)) {
            requestOverlayPermission(context)
            return false
        }

        return if (_isOverlayActive.value) {
            GamingOverlayService.stop(context)
            _isOverlayActive.value = false
            false
        } else {
            GamingOverlayService.start(context)
            _isOverlayActive.value = true
            true
        }
    }
}
