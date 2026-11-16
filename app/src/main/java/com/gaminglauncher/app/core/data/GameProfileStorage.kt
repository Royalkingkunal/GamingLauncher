package com.gaminglauncher.app.core.data

import android.content.Context
import android.content.SharedPreferences
import com.gaminglauncher.app.core.model.CrosshairStyle
import com.gaminglauncher.app.core.model.GameInfo
import com.gaminglauncher.app.core.model.PerformancePreset
import org.json.JSONArray
import org.json.JSONObject

class GameProfileStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gaming_launcher_profiles", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_GAMES = "custom_games_list"
        private const val KEY_PROFILES = "game_profiles_json"
    }

    /**
     * Retrieves saved custom added game packages.
     */
    fun getCustomGamePackages(): Set<String> {
        return prefs.getStringSet(KEY_CUSTOM_GAMES, emptySet()) ?: emptySet()
    }

    fun addCustomGamePackage(packageName: String) {
        val current = getCustomGamePackages().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_CUSTOM_GAMES, current).apply()
    }

    fun removeGamePackage(packageName: String) {
        val current = getCustomGamePackages().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_CUSTOM_GAMES, current).apply()

        // Also remove profile
        val profiles = getAllProfilesJson()
        profiles.remove(packageName)
        prefs.edit().putString(KEY_PROFILES, profiles.toString()).apply()
    }

    fun saveGameProfile(game: GameInfo) {
        val profiles = getAllProfilesJson()
        val obj = JSONObject().apply {
            put("packageName", game.packageName)
            put("name", game.name)
            put("isFavorite", game.isUserFavorite)
            put("lastPlayed", game.lastPlayedTimestamp)
            put("playDuration", game.totalPlayDurationSeconds)
            put("preset", game.performancePreset.name)
            put("dnd", game.enableDnd)
            put("crosshair", game.enableCrosshair)
            put("crosshairStyle", game.crosshairStyle.name)
            put("crosshairColor", game.crosshairColorHex)
        }
        profiles.put(game.packageName, obj)
        prefs.edit().putString(KEY_PROFILES, profiles.toString()).apply()
    }

    fun loadGameProfile(packageName: String, fallbackName: String): GameInfo {
        val profiles = getAllProfilesJson()
        val obj = profiles.optJSONObject(packageName) ?: return GameInfo(
            packageName = packageName,
            name = fallbackName
        )

        val presetName = obj.optString("preset", PerformancePreset.BEAST_MODE.name)
        val preset = try {
            PerformancePreset.valueOf(presetName)
        } catch (_: Exception) {
            PerformancePreset.BEAST_MODE
        }

        val styleName = obj.optString("crosshairStyle", CrosshairStyle.DOT.name)
        val style = try {
            CrosshairStyle.valueOf(styleName)
        } catch (_: Exception) {
            CrosshairStyle.DOT
        }

        return GameInfo(
            packageName = packageName,
            name = obj.optString("name", fallbackName),
            isUserFavorite = obj.optBoolean("isFavorite", false),
            lastPlayedTimestamp = obj.optLong("lastPlayed", 0L),
            totalPlayDurationSeconds = obj.optLong("playDuration", 0L),
            performancePreset = preset,
            enableDnd = obj.optBoolean("dnd", true),
            enableCrosshair = obj.optBoolean("crosshair", false),
            crosshairStyle = style,
            crosshairColorHex = obj.optString("crosshairColor", "#00F2FE")
        )
    }

    fun recordGameSession(packageName: String, sessionDurationSeconds: Long) {
        val profile = loadGameProfile(packageName, packageName)
        val updated = profile.copy(
            lastPlayedTimestamp = System.currentTimeMillis(),
            totalPlayDurationSeconds = profile.totalPlayDurationSeconds + sessionDurationSeconds
        )
        saveGameProfile(updated)
    }

    private fun getAllProfilesJson(): JSONObject {
        val raw = prefs.getString(KEY_PROFILES, null) ?: "{}"
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
