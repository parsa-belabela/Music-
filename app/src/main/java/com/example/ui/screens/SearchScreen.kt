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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * تب سرچ و کاوش اصلی که بدون هیچ‌گونه مکث یا فریز روی ترد اصلی (Main Thread) باز می‌شود
 * و از کش خصوصی داخلی برای بارگذاری فوری کاتالوگ، کاورهای تلگرام و عکس‌های هنرمندان استفاده می‌کند.
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
    // خواندن فوری از حافظه رم (۰ میلی‌ثانیه روی ترد اصلی، بدون هیچ‌گونه I/O یا پارس JSON در زمان کلیک روی تب سرچ)
    var onlineTracks by remember {
        mutableStateOf(OnlineMusicRepository.getMemoryCachedAppTracks())
    }
    var isLoadingOnline by remember { mutableStateOf(onlineTracks.isEmpty()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        if (onlineTracks.isEmpty()) {
            isLoadingOnline = true
            // ۱. بارگذاری کش دیسک روی ترد پس‌زمینه (IO) بدون قفل کردن رابط کاربری
            val diskTracks = OnlineMusicRepository.loadCachedAppTracksAsync()
            if (diskTracks.isNotEmpty()) {
                onlineTracks = diskTracks
                isLoadingOnline = false
            }
        }
        try {
            // ۲. دریافت آخرین وضعیت از سرور کلادفلر در پس‌زمینه
            val response = OnlineMusicRepository.fetchFromWorker()
            if (response.tracks.isNotEmpty()) {
                val mapped = withContext(Dispatchers.Default) {
                    response.tracks.map { it.toAppTrack() }
                }
                OnlineMusicRepository.setMemoryCachedAppTracks(mapped)
                onlineTracks = mapped
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingOnline = false
        }
    }

    var unifiedTracks by remember {
        mutableStateOf(if (onlineTracks.isNotEmpty()) onlineTracks else tracks)
    }

    LaunchedEffect(tracks, onlineTracks) {
        unifiedTracks = if (onlineTracks.isEmpty()) {
            tracks
        } else if (tracks.isEmpty()) {
            onlineTracks
        } else {
            withContext(Dispatchers.Default) {
                (onlineTracks + tracks).distinctBy {
                    "${it.title.trim().lowercase()}__${it.artist.trim().lowercase()}"
                }
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
        if (onlineTracks.isEmpty()) localTracks
        else if (localTracks.isEmpty()) onlineTracks
        else (onlineTracks + localTracks).distinctBy { "${it.title.trim()}_${it.artist.trim()}" }
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
