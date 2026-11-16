package com.gaminglauncher.app.core.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.gaminglauncher.app.core.model.GameInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameDetector(
    private val context: Context,
    private val storage: GameProfileStorage
) {
    private val packageManager = context.packageManager

    private val knownGameKeywords = listOf(
        "game", "pubg", "freefire", "codm", "clash", "brawl", "minecraft",
        "roblox", "genshin", "honkai", "asphalt", "shadowfight", "pokemon",
        "league", "wildrift", "subway", "temple", "candy", "fifa", "ea"
    )

    private val knownGamePrefixes = listOf(
        "com.mihoyo", "com.tencent", "com.activision", "com.epicgames",
        "com.pubg", "com.dts.freefire", "com.roblox", "com.mojang",
        "com.supercell", "com.ea.", "com.gameloft", "com.garena",
        "com.krafton", "com.riotgames", "com.nintendo"
    )

    suspend fun getInstalledGames(): List<GameInfo> = withContext(Dispatchers.IO) {
        val customPackages = storage.getCustomGamePackages()
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val games = mutableListOf<GameInfo>()

        for (app in installedApps) {
            val pkg = app.packageName
            // Skip system settings / launcher / internal apps
            if (pkg == context.packageName) continue

            val isCustomAdded = customPackages.contains(pkg)
            val isOfficialGame = isRecognizedAsGame(app)

            if (isOfficialGame || isCustomAdded) {
                val appName = try {
                    packageManager.getApplicationLabel(app).toString()
                } catch (_: Exception) {
                    pkg
                }

                val icon = try {
                    packageManager.getApplicationIcon(app)
                } catch (_: Exception) {
                    null
                }

                val profile = storage.loadGameProfile(pkg, appName)
                games.add(
                    profile.copy(
                        name = appName,
                        isAutoDetectedGame = isOfficialGame,
                        icon = icon
                    )
                )
            }
        }

        games.sortedWith(
            compareByDescending<GameInfo> { it.isUserFavorite }
                .thenByDescending { it.lastPlayedTimestamp }
                .thenBy { it.name.lowercase() }
        )
    }

    suspend fun getAllInstallableApps(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        installedApps
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
            .map { app ->
                val label = try {
                    packageManager.getApplicationLabel(app).toString()
                } catch (_: Exception) {
                    app.packageName
                }
                Pair(app.packageName, label)
            }
            .sortedBy { it.second.lowercase() }
    }

    private fun isRecognizedAsGame(app: ApplicationInfo): Boolean {
        // 1. Android official category check (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (app.category == ApplicationInfo.CATEGORY_GAME) return true
        }

        // 2. Legacy game flag
        @Suppress("DEPRECATION")
        if ((app.flags and ApplicationInfo.FLAG_IS_GAME) != 0) return true

        // 3. Known package prefix matching
        val lowerPkg = app.packageName.lowercase()
        if (knownGamePrefixes.any { lowerPkg.startsWith(it) }) return true

        // 4. Keyword matching in package name
        return knownGameKeywords.any { lowerPkg.contains(it) }
    }
}
