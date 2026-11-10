package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.ui.components.*
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MusicPlayerViewModel
import com.example.util.Localization
import kotlinx.coroutines.launch

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
    val livePositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()

    val timeSlotTracks by viewModel.timeSlotTracks.collectAsStateWithLifecycle()
    val onlineTrendingTracks by viewModel.onlineTrendingTracks.collectAsStateWithLifecycle()
    val selectedTrendingMode by viewModel.selectedTrendingMode.collectAsStateWithLifecycle()
    val isLoadingOnlineRecommendations by viewModel.isLoadingOnlineRecommendations.collectAsStateWithLifecycle()
    val userTopArtists by viewModel.userTopArtists.collectAsStateWithLifecycle()
    val onlineRecentTracks by viewModel.onlineRecentTracks.collectAsStateWithLifecycle()
    val onThisDayHighlight by viewModel.onThisDayHighlight.collectAsStateWithLifecycle()
    val weeklyRecapStats by viewModel.weeklyRecapStats.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val unlockedStyles by viewModel.unlockedStyles.collectAsStateWithLifecycle()
    val didRestoreSession by viewModel.didRestoreSession.collectAsStateWithLifecycle()
    val isScanningMedia by viewModel.isScanningMedia.collectAsStateWithLifecycle()

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
    val loopStartMs by viewModel.loopStartMs.collectAsStateWithLifecycle()
    val loopEndMs by viewModel.loopEndMs.collectAsStateWithLifecycle()
    val showShareCard by viewModel.showShareCard.collectAsStateWithLifecycle()
    val showHearingProfileTest by viewModel.showHearingProfileTest.collectAsStateWithLifecycle()
    val showDuplicatesReview by viewModel.showDuplicatesReview.collectAsStateWithLifecycle()

    var showVipPaywallForFeature by remember { mutableStateOf<String?>(null) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var achievementItems by remember { mutableStateOf<List<com.example.monetization.AchievementItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

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

    BackHandler(
        enabled = showVipPaywallForFeature != null || showAchievementsDialog || isNowPlayingExpanded || showLyricsEditor || showEqualizer || showQueue || showSleepTimer || showHearingProfileTest || showDuplicatesReview || editingTrackMetadata != null || showShareCard != null || currentTab != MainTab.HOME
    ) {
        when {
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
                    if (!isNowPlayingExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.28f to Color(0xD9121212),
                                            1.0f to Color(0xFF0A0A0A)
                                        )
                                    )
                                )
                                .navigationBarsPadding()
                        ) {
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

                            // Spotify-Architecture Flat Bottom Navigation Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .padding(horizontal = 4.dp)
                                    .testTag("main_navigation_bar"),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpotifyNavBarItem(
                                    selected = currentTab == MainTab.HOME,
                                    selectedIcon = Icons.Filled.Home,
                                    unselectedIcon = Icons.Outlined.Home,
                                    label = Localization.getString("home", lang),
                                    onClick = { currentTab = MainTab.HOME },
                                    modifier = Modifier.weight(1f)
                                )
                                SpotifyNavBarItem(
                                    selected = currentTab == MainTab.SEARCH,
                                    selectedIcon = Icons.Filled.Search,
                                    unselectedIcon = Icons.Outlined.Search,
                                    label = Localization.getString("search", lang),
                                    onClick = { currentTab = MainTab.SEARCH },
                                    modifier = Modifier.weight(1f)
                                )
                                SpotifyNavBarItem(
                                    selected = currentTab == MainTab.LIBRARY,
                                    selectedIcon = Icons.Filled.LibraryMusic,
                                    unselectedIcon = Icons.Outlined.LibraryMusic,
                                    label = Localization.getString("library", lang),
                                    onClick = { currentTab = MainTab.LIBRARY },
                                    modifier = Modifier.weight(1f)
                                )
                                SpotifyNavBarItem(
                                    selected = currentTab == MainTab.PLAYLISTS,
                                    selectedIcon = Icons.Filled.QueueMusic,
                                    unselectedIcon = Icons.Outlined.QueueMusic,
                                    label = Localization.getString("playlists", lang),
                                    onClick = { currentTab = MainTab.PLAYLISTS },
                                    modifier = Modifier.weight(1f)
                                )
                                SpotifyNavBarItem(
                                    selected = currentTab == MainTab.WRAPPED,
                                    selectedIcon = Icons.Filled.BarChart,
                                    unselectedIcon = Icons.Outlined.BarChart,
                                    label = Localization.getString("wrapped", lang),
                                    onClick = { currentTab = MainTab.WRAPPED },
                                    modifier = Modifier.weight(1f)
                                )
                                SpotifyNavBarItem(
                                    selected = currentTab == MainTab.SETTINGS,
                                    selectedIcon = Icons.Filled.Settings,
                                    unselectedIcon = Icons.Outlined.Settings,
                                    label = Localization.getString("settings", lang),
                                    onClick = { currentTab = MainTab.SETTINGS },
                                    modifier = Modifier.weight(1f)
                                )
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
                            onPlayTrack = { track, list ->
                                viewModel.playTrack(track, list)
                            },
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
                            currentPositionProvider = { viewModel.currentPositionMs.value },
                            onSeekTo = { viewModel.seekTo(it) },
                            onToggleGymMode = { viewModel.toggleGymMode() },
                            onlineTrendingTracks = onlineTrendingTracks,
                            selectedTrendingMode = selectedTrendingMode,
                            isLoadingOnlineRecommendations = isLoadingOnlineRecommendations,
                            userTopArtists = userTopArtists,
                            onlineRecentTracks = onlineRecentTracks,
                            onSelectTrendingMode = { viewModel.selectTrendingMode(it) },
                            onRefreshOnlineRecommendations = { viewModel.refreshOnlineRecommendations() },
                            onDownloadTrack = { viewModel.downloadTrackToDeviceLibrary(it) },
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
                            onPlayTrack = { track, list ->
                                viewModel.playTrack(track, list)
                            },
                            onPlayNext = { viewModel.playNextInQueue(it) },
                            onAddToQueue = { viewModel.addToQueue(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDownloadTrack = { viewModel.downloadTrackToDeviceLibrary(it) },
                            onPauseOfflinePlayer = { viewModel.pause() },
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MainTab.LIBRARY -> {
                        LibraryScreen(
                            tracks = allTracks,
                            playlists = playlists,
                            playbackState = playbackState,
                            palette = palette,
                            settings = appSettings,
                            onPlayTrack = { track, list ->
                                viewModel.playTrack(track, list)
                            },
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
                            isScanningMedia = isScanningMedia,
                            bottomPadding = bottomBarPadding,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                    MainTab.WRAPPED -> {
                        LaunchedEffect(Unit) {
                            viewModel.refreshWrappedPeriods()
                        }
                        WrappedScreen(
                            appSettings = appSettings,
                            palette = palette,
                            periods = wrappedPeriods,
                            selectedPeriod = selectedWrappedPeriod,
                            wrappedStats = wrappedStats,
                            onSelectPeriod = { viewModel.selectWrappedPeriod(it) },
                            onPlayTrackById = { trackId ->
                                val onlineTaste = try {
                                    com.example.OnlineTasteEngine.getRecentOnlineTracks(context)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                                val onlineMem = try {
                                    com.example.OnlineMusicRepository.getMemoryCachedAppTracks()
                                } catch (_: Exception) {
                                    emptyList()
                                }
                                val combined = (allTracks + onlineTaste + onlineMem).distinctBy { it.id }
                                combined.firstOrNull { it.id == trackId }?.let { target ->
                                    viewModel.playTrack(target, combined)
                                }
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

                // Smooth Spotify-style slide-up transition for Full NowPlayingScreen
                AnimatedVisibility(
                    visible = isNowPlayingExpanded && playbackState.currentTrack != null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(220)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(200))
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
                        onPlaybackSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                        loopStartMs = loopStartMs,
                        loopEndMs = loopEndMs,
                        onSetAbLoop = { a, b -> viewModel.setAbLoop(a, b) },
                        onClearAbLoop = { viewModel.clearAbLoop() },
                        onDownloadTrack = { trk -> viewModel.downloadTrackToDeviceLibrary(trk) }
                    )
                }
            }
        }

        // Modal Sheets & Dialogs
        showShareCard?.let { trk ->
            ShareSongDialog(
                track = trk,
                palette = palette,
                language = lang,
                lyrics = currentLyrics,
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
                    Toast.makeText(
                        context,
                        if (lang == AppLanguage.PERSIAN) "پروفایل اختصاصی شنوایی شما ذخیره و اعمال شد." else "Hearing calibrated profile applied!",
                        Toast.LENGTH_SHORT
                    ).show()
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
                    Toast.makeText(
                        context,
                        if (lang == AppLanguage.PERSIAN) "فایل‌های تکراری با موفقیت بهینه‌سازی شدند." else "Duplicate tracks optimized!",
                        Toast.LENGTH_SHORT
                    ).show()
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
            VipPaywallSheet(
                onDismiss = { showVipPaywallForFeature = null },
                onUnlockedSuccess = {
                    showVipPaywallForFeature = null
                    viewModel.refreshUnlockedStyles()
                }
            )
        }

        if (showAchievementsDialog) {
            AchievementsDialog(
                achievements = achievementItems,
                palette = palette,
                lang = lang,
                onDismiss = { showAchievementsDialog = false },
                onRewardClaimed = { _ ->
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

@Composable
private fun SpotifyNavBarItem(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (selected) Color.White else TextSecondary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

