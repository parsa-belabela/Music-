package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Track
import com.example.ui.components.*
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.ui.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    HOME("Home"),
    LIBRARY("Library"),
    PLAYLISTS("Playlists"),
    SEARCH("Search"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by viewModel.playbackState.collectAsState()
    val analysisData by viewModel.analysisData.collectAsState()
    val palette by viewModel.activePalette.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val allTracks by viewModel.allTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()

    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val showLyricsEditor by viewModel.showLyricsEditor.collectAsState()
    val showEqualizer by viewModel.showEqualizer.collectAsState()
    val showQueue by viewModel.showQueue.collectAsState()
    val showSleepTimer by viewModel.showSleepTimer.collectAsState()
    val editingTrackMetadata by viewModel.editingTrackMetadata.collectAsState()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            if (!isNowPlayingExpanded) {
                Column {
                    // Persistent Floating Mini Player
                    if (playbackState.currentTrack != null) {
                        MiniPlayer(
                            playbackState = playbackState,
                            palette = palette,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onExpandNowPlaying = { viewModel.isNowPlayingExpanded.value = true },
                            analysisDataProvider = { analysisData },
                            currentPositionProvider = { viewModel.currentPositionMs.value }
                        )
                    }

                    // Floating Liquid Glass Navigation Bar (iOS 26 dynamic tab philosophy)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .liquidGlass(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.08f
                            )
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            modifier = Modifier.testTag("main_navigation_bar")
                        ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.HOME,
                            onClick = { currentTab = MainTab.HOME },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                unselectedIconColor = Color(0xFF75758C),
                                unselectedTextColor = Color(0xFF75758C),
                                indicatorColor = palette.primary.copy(alpha = 0.25f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.LIBRARY,
                            onClick = { currentTab = MainTab.LIBRARY },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                unselectedIconColor = Color(0xFF75758C),
                                unselectedTextColor = Color(0xFF75758C),
                                indicatorColor = palette.primary.copy(alpha = 0.25f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.PLAYLISTS,
                            onClick = { currentTab = MainTab.PLAYLISTS },
                            icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists") },
                            label = { Text("Playlists") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                unselectedIconColor = Color(0xFF75758C),
                                unselectedTextColor = Color(0xFF75758C),
                                indicatorColor = palette.primary.copy(alpha = 0.25f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.SEARCH,
                            onClick = { currentTab = MainTab.SEARCH },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                unselectedIconColor = Color(0xFF75758C),
                                unselectedTextColor = Color(0xFF75758C),
                                indicatorColor = palette.primary.copy(alpha = 0.25f)
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { currentTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = palette.accent,
                                selectedTextColor = palette.accent,
                                unselectedIconColor = Color(0xFF75758C),
                                unselectedTextColor = Color(0xFF75758C),
                                indicatorColor = palette.primary.copy(alpha = 0.25f)
                            )
                        )
                    }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        playbackState = playbackState,
                        allTracks = allTracks,
                        recentlyPlayed = recentlyPlayed,
                        favoriteTracks = favoriteTracks,
                        palette = palette,
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onOpenLibrary = { currentTab = MainTab.LIBRARY }
                    )
                }
                MainTab.LIBRARY -> {
                    LibraryScreen(
                        tracks = allTracks,
                        playbackState = playbackState,
                        palette = palette,
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                        onPlayNext = { viewModel.playNextInQueue(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onEditMetadata = { viewModel.editingTrackMetadata.value = it },
                        onRescanMedia = {
                            viewModel.scanDeviceLibrary { count ->
                                Toast.makeText(context, "Scanned $count tracks", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                MainTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlists = playlists,
                        allTracks = allTracks,
                        favoriteTracks = favoriteTracks,
                        mostPlayedTracks = mostPlayed,
                        recentlyAddedTracks = recentlyAdded,
                        palette = palette,
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                        onPlayTrackList = { list ->
                            list.firstOrNull()?.let { viewModel.playTrack(it, list) }
                        }
                    )
                }
                MainTab.SEARCH -> {
                    SearchScreen(
                        allTracks = allTracks,
                        palette = palette,
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        settings = appSettings,
                        palette = palette,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onSetPreset = { viewModel.setVisualizerPreset(it) },
                        onExportBackup = { viewModel.exportBackupJson() },
                        onImportBackup = { viewModel.importBackupJson(it) },
                        onRescanLibrary = {
                            viewModel.scanDeviceLibrary { count ->
                                Toast.makeText(context, "Rescanned $count tracks", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Fullscreen Modal for Now Playing Screen
            AnimatedVisibility(
                visible = isNowPlayingExpanded && playbackState.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    playbackState = playbackState,
                    analysisData = analysisData,
                    palette = palette,
                    currentLyrics = currentLyrics,
                    appSettings = appSettings,
                    currentPositionProvider = { viewModel.currentPositionMs.value },
                    onCollapse = { viewModel.isNowPlayingExpanded.value = false },
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextTrack() },
                    onPrevious = { viewModel.previousTrack() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onOpenLyricsEditor = { viewModel.showLyricsEditor.value = true },
                    onOpenEqualizer = { viewModel.showEqualizer.value = true },
                    onOpenQueue = { viewModel.showQueue.value = true },
                    onSelectVisualizerMode = { viewModel.setVisualizerMode(it) },
                    onOpenSleepTimer = { viewModel.showSleepTimer.value = true },
                    onOpenMetadataEditor = { viewModel.editingTrackMetadata.value = it }
                )
            }
        }
    }

    // Modal Sheets & Dialogs
    if (showLyricsEditor) {
        LyricsEditorSheet(
            track = playbackState.currentTrack,
            currentLyrics = currentLyrics,
            currentPositionMs = playbackState.currentPositionMs,
            palette = palette,
            onSave = { rawLrc, offsetMs ->
                playbackState.currentTrack?.let {
                    viewModel.saveLyrics(it.id, rawLrc, offsetMs)
                }
            },
            onClose = { viewModel.showLyricsEditor.value = false }
        )
    }

    if (showEqualizer) {
        EqualizerSheet(
            settings = appSettings,
            palette = palette,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onClose = { viewModel.showEqualizer.value = false }
        )
    }

    if (showQueue) {
        QueueSheet(
            playbackState = playbackState,
            palette = palette,
            onPlayTrack = { viewModel.playTrack(it) },
            onRemoveFromQueue = { viewModel.removeFromQueue(it) },
            onClearQueue = { viewModel.clearQueue() },
            onReorder = { from, to -> viewModel.reorderQueue(from, to) },
            onSaveAsPlaylist = { name -> viewModel.createPlaylist(name) },
            onClose = { viewModel.showQueue.value = false }
        )
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            currentMinutes = appSettings.sleepTimerMinutes,
            onSelectMinutes = { viewModel.setSleepTimer(it) },
            onDismiss = { viewModel.showSleepTimer.value = false }
        )
    }

    editingTrackMetadata?.let { trk ->
        MetadataEditorDialog(
            track = trk,
            onSave = { title, artist, album, genre, year ->
                viewModel.updateTrackMetadata(trk, title, artist, album, genre, year)
                viewModel.editingTrackMetadata.value = null
            },
            onDismiss = { viewModel.editingTrackMetadata.value = null }
        )
    }
}
