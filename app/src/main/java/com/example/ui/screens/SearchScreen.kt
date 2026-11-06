package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.CloudflareMusicApi
import com.example.CloudflareTrack
import com.example.OnlineMusicRepository
import com.example.OnlineMusicScreen
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.PlaybackState
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.monetization.EntitlementManager
import com.example.ui.components.VipLockGateCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val SEARCH_CLOUDFLARE_WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTrackSelected: (CloudflareTrack) -> Unit
) {
    val context = LocalContext.current
    val entitlementManager = remember { EntitlementManager.getInstance(context) }
    val isVip by entitlementManager.isVip.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CloudflareTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // The explicit worker URL
    val workerUrl = "https://alubm.parsaghorbani8861.workers.dev"

    // Only search if VIP
    LaunchedEffect(searchQuery, isVip) {
        if (!isVip) return@LaunchedEffect

        if (searchQuery.isNotBlank()) {
            delay(500) // Debounce
        }

        isLoading = true
        errorMessage = null

        val result = CloudflareMusicApi.searchOrFetchTracks(searchQuery, entitlementManager.getDeviceId())
        result.onSuccess { tracks ->
            searchResults = tracks
            isLoading = false
        }.onFailure { error ->
            errorMessage = error.localizedMessage ?: "خطا در جستجو"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجوی آهنگ یا خواننده...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isVip) {
            // Show VIP Lock Card if not premium
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                VipLockGateCard(
                    workerUrl = workerUrl,
                    onUnlocked = {
                        // isVip will become true automatically, this will trigger the LaunchedEffect
                    }
                )
            }
        } else {
            // Show Search Results
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFA855F7))
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 14.sp)
                }
            } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "نتیجه‌ای یافت نشد.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { track ->
                        TrackListItem(track = track, onClick = { onTrackSelected(track) })
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListItem(track: CloudflareTrack, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (!track.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("🎵", fontSize = 24.sp)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * تب سرچ و کاوش اصلی که بدون هیچ‌گونه مکث یا فریز روی ترد اصلی (Main Thread) باز می‌شود
 * و لیست کامل آهنگ‌های کلودفلر (https://alubm.parsaghorbani8861.workers.dev) را بارگذاری می‌کند.
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
    val context = LocalContext.current
    val entitlementManager = remember { EntitlementManager.getInstance(context) }
    val isVip by entitlementManager.isVip.collectAsState()
    val isPremium by EntitlementManager.isPremium.collectAsState()

    var onlineTracks by remember {
        mutableStateOf(OnlineMusicRepository.getMemoryCachedAppTracks())
    }
    var isLoadingOnline by remember { mutableStateOf(onlineTracks.isEmpty()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isLoadingOnline) {
        if (isLoadingOnline) {
            delay(5000L)
            isLoadingOnline = false
        }
    }

    LaunchedEffect(refreshTrigger, isVip, isPremium) {
        if (onlineTracks.isEmpty()) {
            isLoadingOnline = true
            val diskTracks = OnlineMusicRepository.loadCachedAppTracksAsync()
            if (diskTracks.isNotEmpty()) {
                onlineTracks = diskTracks
                isLoadingOnline = false
            }
        }
        try {
            val response = OnlineMusicRepository.fetchFromWorker()
            if (response.tracks.isNotEmpty()) {
                val mapped = withContext(Dispatchers.Default) {
                    response.tracks.map { it.toAppTrack() }
                }
                OnlineMusicRepository.setMemoryCachedAppTracks(mapped)
                onlineTracks = mapped
            } else {
                val cfResult = CloudflareMusicApi.searchOrFetchTracks("", entitlementManager.getDeviceId())
                cfResult.onSuccess { cfTracks ->
                    if (cfTracks.isNotEmpty()) {
                        val mapped = withContext(Dispatchers.Default) {
                            cfTracks.map { it.toAppTrack() }
                        }
                        OnlineMusicRepository.setMemoryCachedAppTracks(mapped)
                        onlineTracks = mapped
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingOnline = false
        }
    }

    val isPersian = appSettings.language == AppLanguage.PERSIAN

    var unifiedTracks by remember {
        mutableStateOf(if (onlineTracks.isNotEmpty()) onlineTracks else tracks)
    }

    LaunchedEffect(tracks, onlineTracks, isPersian) {
        val nextUnified = if (onlineTracks.isEmpty()) {
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
        if (nextUnified.isNotEmpty()) {
            OnlineMusicRepository.computeHubDataAsync(nextUnified, isPersian)
        }
        unifiedTracks = nextUnified
    }

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
