package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.OnlineMusicRepository
import com.example.OnlineMusicScreen
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.PlaybackState
import com.example.data.model.Playlist
import com.example.data.model.Track

/**
 * تب سرچ و کاوش اصلی که با زبان برنامه هماهنگ است (پیش‌فرض انگلیسی و تغییر خودکار به فارسی)
 * و از کش خصوصی داخلی برای بارگذاری فوری پروفایل‌ها و عکس‌های هنرمندان استفاده می‌کند.
 */
@Composable
fun SearchScreen(
    tracks: List<Track>,
    playlists: List<Playlist> = emptyList(),
    playbackState: PlaybackState,
    palette: AmbientPalette,
    appSettings: AppSettings,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDeletePlaylist: (String) -> Unit = {},
    onAddToPlaylist: (Playlist, Track) -> Unit = { _, _ -> },
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onEditMetadata: (Track) -> Unit = {},
    onDeleteTrack: (Track) -> Unit = {},
    onPlayTrackList: (List<Track>) -> Unit = {},
    onDownloadTrack: (Track) -> Unit = {},
    onPauseOfflinePlayer: () -> Unit = {},
    bottomPadding: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    // بارگذاری آنی کاتالوگ ذخیره‌شده در حافظه خصوصی داخلی گوشی برای باز شدن فوری در مراجعات بعدی
    var onlineTracks by remember {
        val cached = OnlineMusicRepository.loadCachedWorkerCatalog()
        mutableStateOf(cached?.tracks?.map { it.toAppTrack() }.orEmpty())
    }
    var isLoadingOnline by remember { mutableStateOf(onlineTracks.isEmpty()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        if (onlineTracks.isEmpty()) {
            isLoadingOnline = true
        }
        try {
            val response = OnlineMusicRepository.fetchFromWorker()
            if (response.tracks.isNotEmpty()) {
                onlineTracks = response.tracks.map { it.toAppTrack() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingOnline = false
        }
    }

    val unifiedTracks = remember(tracks, onlineTracks) {
        if (onlineTracks.isEmpty()) {
            tracks
        } else {
            (onlineTracks + tracks).distinctBy {
                "${it.title.trim().lowercase()}__${it.artist.trim().lowercase()}"
            }
        }
    }

    val isPersian = appSettings.language == AppLanguage.PERSIAN

    Box(modifier = modifier.fillMaxSize()) {
        OnlineMusicScreen(
            onlineTracks = unifiedTracks,
            isLoading = isLoadingOnline,
            currentPlayingTrack = playbackState.currentTrack,
            isPersian = isPersian,
            onPlayTrack = onPlayTrack,
            onRefresh = { refreshTrigger++ },
            bottomPadding = bottomPadding + 20.dp,
            onDownloadTrack = onDownloadTrack
        )
    }
}

@Composable
fun SearchScreen(
    localTracks: List<Track>,
    onlineTracks: List<Track> = emptyList(),
    currentPlayingTrack: Track? = null,
    isLoadingOnline: Boolean = false,
    isPersian: Boolean = false,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onRefreshOnline: () -> Unit = {}
) {
    val unifiedTracks = remember(localTracks, onlineTracks) {
        (onlineTracks + localTracks).distinctBy { "${it.title.trim()}_${it.artist.trim()}" }
    }

    OnlineMusicScreen(
        onlineTracks = unifiedTracks,
        isLoading = isLoadingOnline,
        currentPlayingTrack = currentPlayingTrack,
        isPersian = isPersian,
        onPlayTrack = onPlayTrack,
        onRefresh = onRefreshOnline
    )
}
