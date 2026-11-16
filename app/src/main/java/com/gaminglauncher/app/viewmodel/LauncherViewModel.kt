package com.gaminglauncher.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaminglauncher.app.core.data.GameRepository
import com.gaminglauncher.app.core.model.CrosshairStyle
import com.gaminglauncher.app.core.model.GameInfo
import com.gaminglauncher.app.core.model.PerformancePreset
import com.gaminglauncher.app.core.service.GameSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites

    val isLoading = repository.isLoading

    val filteredGames: StateFlow<List<GameInfo>> = combine(
        repository.games,
        _searchQuery,
        _onlyFavorites
    ) { games, query, favOnly ->
        games.filter { game ->
            val matchesQuery = query.isEmpty() || game.name.contains(query, ignoreCase = true)
            val matchesFav = !favOnly || game.isUserFavorite
            matchesQuery && matchesFav
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshGames()
    }

    fun refreshGames() {
        viewModelScope.launch {
            repository.refreshGames()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun launchGame(context: Context, game: GameInfo) {
        val launched = repository.launchGame(game.packageName)
        if (launched) {
            GameSessionService.startSession(
                context = context,
                packageName = game.packageName,
                gameName = game.name,
                enableDnd = game.enableDnd
            )
        }
    }

    fun toggleFavorite(game: GameInfo) {
        repository.toggleFavorite(game)
    }

    fun updatePerformancePreset(game: GameInfo, preset: PerformancePreset) {
        repository.updatePerformancePreset(game, preset)
    }

    fun updateCrosshairSettings(
        game: GameInfo,
        enabled: Boolean,
        style: CrosshairStyle,
        colorHex: String
    ) {
        repository.updateCrosshairSettings(game, enabled, style, colorHex)
    }

    fun toggleDnd(game: GameInfo) {
        repository.toggleDnd(game)
    }

    fun addCustomGame(packageName: String) {
        viewModelScope.launch {
            repository.addCustomGame(packageName)
        }
    }

    fun removeGame(packageName: String) {
        viewModelScope.launch {
            repository.removeGame(packageName)
        }
    }

    suspend fun getInstallableApps(): List<Pair<String, String>> {
        return repository.getAllInstallableApps()
    }
}
