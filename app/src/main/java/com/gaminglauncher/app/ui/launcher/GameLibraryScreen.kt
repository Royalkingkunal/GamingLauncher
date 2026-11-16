package com.gaminglauncher.app.ui.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaminglauncher.app.core.model.CrosshairStyle
import com.gaminglauncher.app.core.model.GameInfo
import com.gaminglauncher.app.core.model.PerformancePreset
import com.gaminglauncher.app.ui.theme.*
import com.gaminglauncher.app.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val games by viewModel.filteredGames.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlyFavorites by viewModel.onlyFavorites.collectAsState()

    var selectedGameForDetails by remember { mutableStateOf<GameInfo?>(null) }
    var showAddGameDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AmoledBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGameDialog = true },
                containerColor = NeonCyan,
                contentColor = AmoledBlack,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Game")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GAME VAULT",
                        style = Typography.headlineMedium,
                        color = NeonCyan,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${games.size} Games Ready to Launch",
                        style = Typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshGames() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(AmoledDarkCard, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar & Favorite Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search games...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = AmoledCardBorder,
                        focusedContainerColor = AmoledDarkCard,
                        unfocusedContainerColor = AmoledDarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                FilterChip(
                    selected = onlyFavorites,
                    onClick = { viewModel.toggleFavoritesFilter() },
                    label = {
                        Icon(
                            if (onlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (onlyFavorites) AlertRed else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmoledDarkCard,
                        containerColor = AmoledDarkCard
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (onlyFavorites) AlertRed else AmoledCardBorder,
                        enabled = true,
                        selected = onlyFavorites
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && games.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (games.isEmpty()) {
                EmptyGamesState(onAddClick = { showAddGameDialog = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(games, key = { it.packageName }) { game ->
                        GameCard(
                            game = game,
                            onLaunch = { viewModel.launchGame(context, game) },
                            onToggleFavorite = { viewModel.toggleFavorite(game) },
                            onOpenDetails = { selectedGameForDetails = game }
                        )
                    }
                }
            }
        }
    }

    // Game Details & Profile Bottom Sheet
    selectedGameForDetails?.let { game ->
        GameProfileSheet(
            game = game,
            onDismiss = { selectedGameForDetails = null },
            onPresetChange = { preset -> viewModel.updatePerformancePreset(game, preset) },
            onDndToggle = { viewModel.toggleDnd(game) },
            onCrosshairToggle = { enabled, style, color ->
                viewModel.updateCrosshairSettings(game, enabled, style, color)
            },
            onRemove = {
                viewModel.removeGame(game.packageName)
                selectedGameForDetails = null
            }
        )
    }

    // Add Any App as Game Dialog
    if (showAddGameDialog) {
        AddGameDialog(
            viewModel = viewModel,
            onDismiss = { showAddGameDialog = false }
        )
    }
}

@Composable
fun GameCard(
    game: GameInfo,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, AmoledCardBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = AmoledDarkCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game Icon
                val bitmap = remember(game.icon) { drawableToBitmap(game.icon) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = game.name,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(AmoledSurface, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan)
                    }
                }

                // Favorite Icon
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (game.isUserFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (game.isUserFavorite) AlertRed else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = game.name,
                style = Typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = game.performancePreset.title,
                style = Typography.labelSmall,
                color = when (game.performancePreset) {
                    PerformancePreset.BEAST_MODE -> NeonCyan
                    PerformancePreset.BALANCED -> CyberGreen
                    PerformancePreset.BATTERY_SAVER -> ElectricPurple
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLaunch,
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("PLAY", color = AmoledBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                IconButton(
                    onClick = onOpenDetails,
                    modifier = Modifier
                        .size(36.dp)
                        .background(AmoledSurface, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameProfileSheet(
    game: GameInfo,
    onDismiss: () -> Unit,
    onPresetChange: (PerformancePreset) -> Unit,
    onDndToggle: () -> Unit,
    onCrosshairToggle: (Boolean, CrosshairStyle, String) -> Unit,
    onRemove: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AmoledDarkCard,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "${game.name} - Profile",
                style = Typography.headlineMedium,
                color = NeonCyan
            )
            Text(
                text = game.packageName,
                style = Typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Performance Preset", style = Typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformancePreset.values().forEach { preset ->
                    val isSelected = game.performancePreset == preset
                    Button(
                        onClick = { onPresetChange(preset) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonCyan else AmoledSurface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = preset.title,
                            color = if (isSelected) AmoledBlack else TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DND Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Gaming DND", style = Typography.bodyLarge, color = TextPrimary)
                    Text("Silence notifications while playing", style = Typography.bodyMedium, color = TextMuted)
                }
                Switch(
                    checked = game.enableDnd,
                    onCheckedChange = { onDndToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = AmoledSurface)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Crosshair Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Shooter Tactical Crosshair", style = Typography.bodyLarge, color = TextPrimary)
                    Text("Overlay tactical HUD aiming aid", style = Typography.bodyMedium, color = TextMuted)
                }
                Switch(
                    checked = game.enableCrosshair,
                    onCheckedChange = { enabled ->
                        onCrosshairToggle(enabled, game.crosshairStyle, game.crosshairColorHex)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = CyberGreen, checkedTrackColor = AmoledSurface)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove from Game Library")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AddGameDialog(
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit
) {
    var allApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var searchAppQuery by remember { mutableStateOf("") }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        allApps = viewModel.getInstallableApps()
        isLoadingApps = false
    }

    val filtered = remember(allApps, searchAppQuery) {
        if (searchAppQuery.isEmpty()) allApps
        else allApps.filter { it.second.contains(searchAppQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledDarkCard,
        title = {
            Text("Add App as Game", style = Typography.headlineMedium, color = NeonCyan)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchAppQuery,
                    onValueChange = { searchAppQuery = it },
                    placeholder = { Text("Search installed apps...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = AmoledCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingApps) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        items(filtered) { (pkg, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addCustomGame(pkg)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(pkg, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                                }
                                Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            HorizontalDivider(color = AmoledCardBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
fun EmptyGamesState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("NO GAMES DETECTED", style = Typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We scanned for official games. You can manually register any installed app into your gaming vault.",
            style = Typography.bodyMedium,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = AmoledBlack)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Any Installed App", color = AmoledBlack, fontWeight = FontWeight.Bold)
        }
    }
}

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    try {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    } catch (_: Throwable) {
        return null
    }
}
