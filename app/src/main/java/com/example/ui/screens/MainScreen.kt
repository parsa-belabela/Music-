package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Track
import kotlinx.coroutines.launch
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
    val currentAudioProfile by viewModel.currentTrackAudioProfile.collectAsStateWithLifecycle()

    // 15 Features States
    val timeSlotTracks by viewModel.timeSlotTracks.collectAsStateWithLifecycle()
    val onThisDayHighlight by viewModel.onThisDayHighlight.collectAsStateWithLifecycle()
    val weeklyRecapStats by viewModel.weeklyRecapStats.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val unlockedStyles by viewModel.unlockedStyles.collectAsStateWithLifecycle()
    val didRestoreSession by viewModel.didRestoreSession.collectAsStateWithLifecycle()

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
    val showHearingProfileTest by viewModel.showHearingProfileTest.collectAsStateWithLifecycle()
    val showDuplicatesReview by viewModel.showDuplicatesReview.collectAsStateWithLifecycle()

    var showVipPaywallForFeature by remember { mutableStateOf<String?>(null) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var achievementItems by remember { mutableStateOf<List<com.example.monetization.AchievementItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    var showInitialSplash by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    val lang = appSettings.language

    val openAchievementsAction: () -> Unit = {
        coroutineScope.launch {
            val stats = viewModel.getListeningStatsForAchievements()
            achievementItems = com.example.monetization.AchievementManager.getAchievements(
                context = context,
                totalListenedMs = stats.first,
                uniqueTracksCount = stats.second,
                activeStreakDays = stats.third
            )
            showAchievementsDialog = true
        }
    }

    val sharedPrefs = remember { context.getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE) }
    var showTutorial by remember { mutableStateOf(!sharedPrefs.getBoolean("has_completed_tutorial", false)) }

    BackHandler(
        enabled = showTutorial || showVipPaywallForFeature != null || showAchievementsDialog || isNowPlayingExpanded || showLyricsEditor || showEqualizer || showQueue || showSleepTimer || showHearingProfileTest || showDuplicatesReview || editingTrackMetadata != null || showShareCard != null || currentTab != MainTab.HOME
    ) {
        when {
            showTutorial -> {
                sharedPrefs.edit().putBoolean("has_completed_tutorial", true).apply()
                showTutorial = false
            }
            showVipPaywallForFeature != null -> showVipPaywallForFeature = null
            showAchievementsDialog -> showAchievementsDialog = false
            showShareCard != null -> viewModel.dismissShareCard()
            showHearingProfileTest -> viewModel.showHearingProfileTest.value = false
            showDuplicatesReview -> viewModel.showDuplicatesReview.value = false
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
                                    currentPositionProvider = { viewModel.currentPositionMs.value },
                                    onSeekTo = { viewModel.seekTo(it) }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
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
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.testTag("main_navigation_bar")
                                ) {
                                    val navColors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = palette.accent,
                                        selectedTextColor = palette.accent,
                                        unselectedIconColor = Color(0xFF75758C),
                                        unselectedTextColor = Color(0xFF75758C),
                                        indicatorColor = Color.Transparent
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == MainTab.HOME,
                                        onClick = { currentTab = MainTab.HOME },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 26.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                                                    .background(if (currentTab == MainTab.HOME) palette.primary.copy(alpha = 0.28f) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        label = { Text(Localization.getString("home", lang), maxLines = 1) },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.SEARCH,
                                        onClick = { currentTab = MainTab.SEARCH },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 26.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                                                    .background(if (currentTab == MainTab.SEARCH) palette.primary.copy(alpha = 0.28f) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        label = { Text(Localization.getString("search", lang), maxLines = 1) },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.LIBRARY,
                                        onClick = { currentTab = MainTab.LIBRARY },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 26.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                                                    .background(if (currentTab == MainTab.LIBRARY) palette.primary.copy(alpha = 0.28f) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.LibraryMusic, contentDescription = "Library", modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        label = { Text(Localization.getString("library", lang), maxLines = 1) },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.PLAYLISTS,
                                        onClick = { currentTab = MainTab.PLAYLISTS },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 26.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                                                    .background(if (currentTab == MainTab.PLAYLISTS) palette.primary.copy(alpha = 0.28f) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.QueueMusic, contentDescription = "Playlists", modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        label = { Text(Localization.getString("playlists", lang), maxLines = 1) },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.WRAPPED,
                                        onClick = { currentTab = MainTab.WRAPPED },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 26.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                                                    .background(if (currentTab == MainTab.WRAPPED) palette.primary.copy(alpha = 0.28f) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = "Stats", modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        label = { Text(Localization.getString("wrapped", lang), maxLines = 1) },
                                        colors = navColors
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == MainTab.SETTINGS,
                                        onClick = { currentTab = MainTab.SETTINGS },
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 44.dp, height = 26.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(13.dp))
                                                    .background(if (currentTab == MainTab.SETTINGS) palette.primary.copy(alpha = 0.28f) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                                            }
                                        },
                                        label = { Text(Localization.getString("settings", lang), maxLines = 1) },
                                        colors = navColors
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                val bottomBarPadding = paddingValues.calculateBottomPadding()

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
                            onExpandNowPlaying = { viewModel.isNowPlayingExpanded.value = true },
                            timeSlotTracks = timeSlotTracks,
                            onThisDayHighlight = onThisDayHighlight,
                            weeklyRecapStats = weeklyRecapStats,
                            didRestoreSession = didRestoreSession,
                            duplicateCount = duplicateGroups.sumOf { it.losers.size },
                            onOpenDuplicatesReview = { viewModel.showDuplicatesReview.value = true },
                            onToggleFocusMode = {
                                viewModel.updateSettings(appSettings.copy(focusModeEnabled = !appSettings.focusModeEnabled))
                            },
                            currentPositionProvider = { viewModel.currentPositionMs.value },
                            onSeekTo = { viewModel.seekTo(it) },
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
                            onCreatePlaylistWithTracks = { name, trackIds -> viewModel.createPlaylistWithTracks(name, trackIds) },
                            onAddTracksToPlaylist = { plId, trackIds -> viewModel.addTracksToPlaylist(plId, trackIds) },
                            onRenamePlaylist = { plId, newName -> viewModel.renamePlaylist(plId, newName) },
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
                            unlockedStyles = unlockedStyles,
                            onOpenHearingProfileTest = { viewModel.showHearingProfileTest.value = true },
                            onOpenDuplicatesReview = {
                                viewModel.checkDuplicates()
                            },
                            onSelectNowPlayingStyle = { styleId -> viewModel.selectNowPlayingStyle(styleId) },
                            onOpenVipPaywall = { featureId -> showVipPaywallForFeature = featureId ?: "vip_general" },
                            onOpenAchievements = openAchievementsAction,
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
                        audioProfile = currentAudioProfile,
                        onCollapse = { viewModel.isNowPlayingExpanded.value = false },
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onNext = { viewModel.nextTrack() },
                        onPrevious = { viewModel.previousTrack() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onCycleShuffleMode = { viewModel.toggleShuffle() },
                        onCycleRepeat = { viewModel.cycleRepeatMode() },
                        onOpenLyricsEditor = { viewModel.showLyricsEditor.value = true },
                        onOpenEqualizer = { viewModel.showEqualizer.value = true },
                        onOpenQueue = { viewModel.showQueue.value = true },
                        onSelectVisualizerMode = { viewModel.setVisualizerMode(it) },
                        onOpenSleepTimer = { viewModel.showSleepTimer.value = true },
                        onOpenMetadataEditor = { viewModel.editingTrackMetadata.value = it },
                        onOpenHearingCalibration = { viewModel.showHearingProfileTest.value = true },
                        onOpenVipPaywall = { featureId -> showVipPaywallForFeature = featureId ?: "vip_general" },
                        onSelectNowPlayingStyle = { styleId ->
                            viewModel.updateSettings(appSettings.copy(selectedNowPlayingStyle = styleId))
                        },
                        onUpdateTrackArtwork = { trk, artworkUri ->
                            viewModel.updateTrackArtwork(trk, artworkUri)
                        },
                        connectedDevice = connectedDevice,
                        onShareSong = { trk -> viewModel.showShareCard(trk) },
                        onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) }
                    )
                }
            }
        }

        // Interactive Onboarding Tutorial Overlay
        AnimatedVisibility(
            visible = showTutorial && !showInitialSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            InteractiveAppTutorial(
                lang = lang,
                onFinishTutorial = {
                    sharedPrefs.edit().putBoolean("has_completed_tutorial", true).apply()
                    showTutorial = false
                }
            )
        }

        // Cinematic Splash
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

        if (showHearingProfileTest) {
            HearingProfileTestSheet(
                palette = palette,
                lang = lang,
                onApplyCalibratedProfile = { calibratedBands ->
                    viewModel.applyHearingCalibration(calibratedBands)
                    viewModel.showHearingProfileTest.value = false
                    Toast.makeText(context, if (lang == com.example.data.model.AppLanguage.PERSIAN) "پروفایل اختصاصی شنوایی شما ذخیره و اعمال شد." else "Hearing calibrated profile applied!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.showHearingProfileTest.value = false }
            )
        }

        if (showDuplicatesReview && duplicateGroups.isNotEmpty()) {
            DuplicatesReviewDialog(
                groups = duplicateGroups,
                palette = palette,
                lang = lang,
                onKeepAllHigherQuality = {
                    viewModel.applyDuplicateGroupWinners(duplicateGroups)
                    viewModel.showDuplicatesReview.value = false
                    Toast.makeText(context, if (lang == com.example.data.model.AppLanguage.PERSIAN) "فایل‌های تکراری با موفقیت بهینه‌سازی شدند." else "Duplicate tracks optimized!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.showDuplicatesReview.value = false }
            )
        }

        if (showLyricsEditor) {
            LyricsEditorSheet(
                track = playbackState.currentTrack,
                currentLyrics = currentLyrics,
                currentPositionMs = viewModel.currentPositionMs.value,
                palette = palette,
                lang = appSettings.language,
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
                lang = appSettings.language,
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
                lang = appSettings.language,
                onSelectMinutes = { viewModel.setSleepTimer(it) },
                onDismiss = { viewModel.showSleepTimer.value = false }
            )
        }

        editingTrackMetadata?.let { trk ->
            MetadataEditorDialog(
                track = trk,
                lang = appSettings.language,
                onSave = { title, artist, album, genre, year, artworkUri ->
                    viewModel.updateTrackMetadata(trk, title, artist, album, genre, year, artworkUri)
                    viewModel.editingTrackMetadata.value = null
                },
                onDismiss = { viewModel.editingTrackMetadata.value = null }
            )
        }

        if (showVipPaywallForFeature != null) {
            SupportDonationDialog(
                palette = palette,
                language = lang,
                onDismiss = { showVipPaywallForFeature = null },
                onSupportSuccess = {
                    showVipPaywallForFeature = null
                    com.example.data.repository.UserProfileManager.refreshProfile(context)
                    viewModel.refreshUnlockedStyles()
                    viewModel.updateSettings(appSettings.copy())
                }
            )
        }

        if (showAchievementsDialog) {
            AchievementsDialog(
                achievements = achievementItems,
                palette = palette,
                lang = lang,
                onDismiss = { showAchievementsDialog = false },
                onRewardClaimed = { bonusHours ->
                    viewModel.refreshUnlockedStyles()
                    viewModel.updateSettings(appSettings.copy())
                    coroutineScope.launch {
                        val stats = viewModel.getListeningStatsForAchievements()
                        achievementItems = com.example.monetization.AchievementManager.getAchievements(
                            context = context,
                            totalListenedMs = stats.first,
                            uniqueTracksCount = stats.second,
                            activeStreakDays = stats.third
                        )
                    }
                }
            )
        }
    }
}
