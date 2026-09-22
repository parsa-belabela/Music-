package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Track
import com.example.ui.components.*
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.liquidGlass
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.util.Localization

enum class MainTab {
    HOME,
    SEARCH,
    LIBRARY,
    WRAPPED,
    PLAYLISTS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val palette by viewModel.activePalette.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()

    // Wrapped States
    val wrappedPeriods by viewModel.wrappedPeriods.collectAsStateWithLifecycle()
    val selectedWrappedPeriod by viewModel.selectedWrappedPeriod.collectAsStateWithLifecycle()
    val wrappedStats by viewModel.wrappedStats.collectAsStateWithLifecycle()

    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val showLyricsEditor by viewModel.showLyricsEditor.collectAsStateWithLifecycle()
    val showEqualizer by viewModel.showEqualizer.collectAsStateWithLifecycle()
    val showQueue by viewModel.showQueue.collectAsStateWithLifecycle()
    val showSleepTimer by viewModel.showSleepTimer.collectAsStateWithLifecycle()
    val editingTrackMetadata by viewModel.editingTrackMetadata.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedAudioDevice.collectAsStateWithLifecycle()
    val showShareCard by viewModel.showShareCard.collectAsStateWithLifecycle()

    var showInitialSplash by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    val lang = appSettings.language

    // Defensive Back Navigation: Prevent app exit on edge-swipe or back button when sheets/tabs are active
    BackHandler(
        enabled = isNowPlayingExpanded || showLyricsEditor || showEqualizer || showQueue || showSleepTimer || editingTrackMetadata != null || showShareCard != null || currentTab != MainTab.HOME
    ) {
        when {
            showShareCard != null -> viewModel.dismissShareCard()
            showLyricsEditor -> viewModel.showLyricsEditor.value = false
            showEqualizer -> viewModel.showEqualizer.value = false
            showQueue -> viewModel.showQueue.value = false
            showSleepTimer -> viewModel.showSleepTimer.value = false
            editingTrackMetadata != null -> viewModel.editingTrackMetadata.value = null
            isNowPlayingExpanded -> viewModel.isNowPlayingExpanded.value = false
            currentTab != MainTab.HOME -> currentTab = MainTab.HOME
        }
    }

    CompositionLocalProvider(LocalAppTheme provides appSettings.theme) {
        Box(modifier = modifier.fillMaxSize()) {
            // Living RGB Motion Graphic Aurora Background behind the entire application (Draw phase dynamic energy reading)
            ModernAuroraMotionBackground(
                palette = palette,
                appTheme = appSettings.theme,
                energyBoostProvider = { viewModel.analysisData.value.haloExpansion },
                modifier = Modifier.fillMaxSize()
            )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isNowPlayingExpanded && !showInitialSplash) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        // Persistent Floating Mini Player
                        if (playbackState.currentTrack != null) {
                            MiniPlayer(
                                playbackState = playbackState,
                                palette = palette,
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onNext = { viewModel.nextTrack() },
                                onPrevious = { viewModel.previousTrack() },
                                onExpandNowPlaying = { viewModel.isNowPlayingExpanded.value = true },
                                connectedDevice = connectedDevice,
                                analysisDataProvider = { viewModel.analysisData.value },
                                currentPositionProvider = { viewModel.currentPositionMs.value }
                            )
                        }

                        // Floating Liquid Glass Navigation Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .liquidGlass(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.12f,
                                    borderWidth = 1.dp
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
                                    label = { Text(Localization.getString("home", lang), maxLines = 1) },
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
                                    label = { Text(Localization.getString("search", lang), maxLines = 1) },
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
                                    label = { Text(Localization.getString("library", lang), maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = palette.accent,
                                        selectedTextColor = palette.accent,
                                        unselectedIconColor = Color(0xFF75758C),
                                        unselectedTextColor = Color(0xFF75758C),
                                        indicatorColor = palette.primary.copy(alpha = 0.25f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == MainTab.WRAPPED,
                                    onClick = {
                                        currentTab = MainTab.WRAPPED
                                        viewModel.refreshWrappedPeriods()
                                    },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Wrapped") },
                                    label = { Text(Localization.getString("wrapped", lang), maxLines = 1) },
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
                                    label = { Text(Localization.getString("settings", lang), maxLines = 1) },
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
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            val bottomBarPadding = paddingValues.calculateBottomPadding()
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (currentTab) {
                    MainTab.HOME -> {
                        HomeScreen(
                            playbackState = playbackState,
                            allTracks = allTracks,
                            recentlyPlayed = recentlyPlayed,
                            favoriteTracks = favoriteTracks,
                            palette = palette,
                            appSettings = appSettings,
                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onOpenLibrary = { currentTab = MainTab.LIBRARY },
                            onOpenWrapped = { currentTab = MainTab.WRAPPED },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                    MainTab.SEARCH -> {
                        SearchScreen(
                            tracks = allTracks,
                            playlists = playlists,
                            playbackState = playbackState,
                            palette = palette,
                            appSettings = appSettings,
                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                            onCreatePlaylistWithTracks = { name, trackList -> viewModel.createPlaylistWithTracks(name, trackList) },
                            onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                            onAddToPlaylist = { pl, trk -> viewModel.addTrackToPlaylist(pl.id, trk.id) },
                            onRemoveTrackFromPlaylist = { plId, trkId -> viewModel.removeTrackFromPlaylist(plId, trkId) },
                            onEditMetadata = { viewModel.editingTrackMetadata.value = it },
                            onDeleteTrack = { viewModel.deleteTrack(it) },
                            onPlayTrackList = { list -> list.firstOrNull()?.let { viewModel.playTrack(it, list) } },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                    MainTab.LIBRARY -> {
                        LibraryScreen(
                            tracks = allTracks,
                            playlists = playlists,
                            playbackState = playbackState,
                            palette = palette,
                            settings = appSettings,
                            onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { pl, trk -> viewModel.addTrackToPlaylist(pl.id, trk.id) },
                            onEditMetadata = { viewModel.editingTrackMetadata.value = it },
                            onDeleteTrack = { viewModel.deleteTrack(it) },
                            onRescanMedia = {
                                viewModel.scanDeviceLibrary { count ->
                                    Toast.makeText(context, "Scanned $count tracks", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                            onCreatePlaylistWithTracks = { name, trackList -> viewModel.createPlaylistWithTracks(name, trackList) },
                            onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                            onRemoveTrackFromPlaylist = { plId, trkId -> viewModel.removeTrackFromPlaylist(plId, trkId) },
                            onShareTrack = { track -> viewModel.showShareCard(track) },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                    MainTab.WRAPPED -> {
                        WrappedScreen(
                            appSettings = appSettings,
                            palette = palette,
                            periods = wrappedPeriods,
                            selectedPeriod = selectedWrappedPeriod,
                            wrappedStats = wrappedStats,
                            onSelectPeriod = { viewModel.selectWrappedPeriod(it) },
                            onPlayTrackById = { trackId ->
                                allTracks.firstOrNull { it.id == trackId }?.let { viewModel.playTrack(it, allTracks) }
                            },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
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
                            settings = appSettings,
                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                            onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                            onRemoveTrackFromPlaylist = { plId, trkId -> viewModel.removeTrackFromPlaylist(plId, trkId) },
                            onPlayTrack = { trk, list -> viewModel.playTrack(trk, list) },
                            onPlayTrackList = { list ->
                                list.firstOrNull()?.let { viewModel.playTrack(it, list) }
                            },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
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
                            },
                            onClearPlaybackHistory = { viewModel.clearPlaybackHistory() },
                            onOpenEqualizer = { viewModel.showEqualizer.value = true },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
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
                        palette = palette,
                        currentLyrics = currentLyrics,
                        appSettings = appSettings,
                        analysisDataProvider = { viewModel.analysisData.value },
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
                        onOpenMetadataEditor = { viewModel.editingTrackMetadata.value = it },
                        connectedDevice = connectedDevice,
                        onShareSong = { trk -> viewModel.showShareCard(trk) },
                        onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) }
                    )
                }
            }
        }

        // Cinematic Initial Splash Loading Screen
        AnimatedVisibility(
            visible = showInitialSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) + scaleOut(targetScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(500))
        ) {
            CinematicGlassSplashScreen(
                theme = appSettings.theme,
                onSplashFinished = { showInitialSplash = false }
            )
        }

        // Modal Sheets & Dialogs
        showShareCard?.let { trk ->
            ShareSongDialog(
                track = trk,
                palette = palette,
                language = lang,
                onDismiss = { viewModel.dismissShareCard() }
            )
        }
        if (showLyricsEditor) {
            LyricsEditorSheet(
                track = playbackState.currentTrack,
                currentLyrics = currentLyrics,
                currentPositionMs = viewModel.currentPositionMs.value,
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
}
}
