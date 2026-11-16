package com.gaminglauncher.app.core.data

import android.content.Context
import android.content.Intent
import com.gaminglauncher.app.core.model.CrosshairStyle
import com.gaminglauncher.app.core.model.GameInfo
import com.gaminglauncher.app.core.model.PerformancePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(
    private val context: Context,
    private val detector: GameDetector,
    private val storage: GameProfileStorage
) {
    private val _games = MutableStateFlow<List<GameInfo>>(emptyList())
    val games: StateFlow<List<GameInfo>> = _games.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun refreshGames() {
        _isLoading.value = true
        try {
            val loaded = detector.getInstalledGames()
            _games.value = loaded
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getAllInstallableApps(): List<Pair<String, String>> {
        return detector.getAllInstallableApps()
    }

    suspend fun addCustomGame(packageName: String) {
        storage.addCustomGamePackage(packageName)
        refreshGames()
    }

    suspend fun removeGame(packageName: String) {
        storage.removeGamePackage(packageName)
        refreshGames()
    }

    fun toggleFavorite(game: GameInfo) {
        val updated = game.copy(isUserFavorite = !game.isUserFavorite)
        storage.saveGameProfile(updated)
        updateGameInState(updated)
    }

    fun updatePerformancePreset(game: GameInfo, preset: PerformancePreset) {
        val updated = game.copy(performancePreset = preset)
        storage.saveGameProfile(updated)
        updateGameInState(updated)
    }

    fun updateCrosshairSettings(
        game: GameInfo,
        enabled: Boolean,
        style: CrosshairStyle,
        colorHex: String
    ) {
        val updated = game.copy(
            enableCrosshair = enabled,
            crosshairStyle = style,
            crosshairColorHex = colorHex
        )
        storage.saveGameProfile(updated)
        updateGameInState(updated)
    }

    fun toggleDnd(game: GameInfo) {
        val updated = game.copy(enableDnd = !game.enableDnd)
        storage.saveGameProfile(updated)
        updateGameInState(updated)
    }

    fun launchGame(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    fun recordSession(packageName: String, durationSeconds: Long) {
        storage.recordGameSession(packageName, durationSeconds)
    }

    private fun updateGameInState(updated: GameInfo) {
        _games.value = _games.value.map {
            if (it.packageName == updated.packageName) updated else it
        }
    }
}
