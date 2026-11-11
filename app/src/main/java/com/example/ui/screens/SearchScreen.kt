package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
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
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    val workerUrl = "https://alubm.parsaghorbani8861.workers.dev"

    LaunchedEffect(searchQuery, isVip) {
        if (!isVip) return@LaunchedEffect

        if (searchQuery.isNotBlank()) {
            delay(400)
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = "جستجو",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 23.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "جستجوی آهنگ یا خواننده...",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = AuraSurfaceElevated,
                unfocusedContainerColor = AuraSurface,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isVip) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                VipLockGateCard(
                    workerUrl = workerUrl,
                    onUnlocked = {}
                )
            }
        } else {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = AuraSpotifyGreen,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage!!, color = Color(0xFFEF4444), fontSize = 14.sp)
                    }
                }
                searchResults.isEmpty() && searchQuery.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "نتیجه‌ای یافت نشد.", color = TextSecondary, fontSize = 15.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(searchResults) { track ->
                            TrackListItem(track = track, onClick = { onTrackSelected(track) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListItem(track: CloudflareTrack, onClick: () -> Unit) {
    val context = LocalContext.current
    val smallCoverUrl = track.coverSmall ?: track.coverUrl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AuraSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (!smallCoverUrl.isNullOrBlank()) {
                val req = com.example.rememberAuraCoverRequest(
                    trackId = track.id,
                    coverUrl = smallCoverUrl,
                    isLarge = false
                )
                AsyncImage(
                    model = req,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                fontSize = 12.5.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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

    val repoTracks by OnlineMusicRepository.onlineTracksFlow.collectAsState()
    var onlineTracks by remember {
        mutableStateOf(OnlineMusicRepository.getMemoryCachedAppTracks())
    }
    var isLoadingOnline by remember { mutableStateOf(onlineTracks.isEmpty() && repoTracks.isEmpty()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(repoTracks) {
        if (repoTracks.isNotEmpty()) {
            onlineTracks = repoTracks
            isLoadingOnline = false
        }
    }

    LaunchedEffect(refreshTrigger, isVip, isPremium) {
        if (onlineTracks.isEmpty() && repoTracks.isEmpty()) {
            isLoadingOnline = true
            val diskTracks = OnlineMusicRepository.loadCachedAppTracksAsync()
            if (diskTracks.isNotEmpty()) {
                onlineTracks = diskTracks
                isLoadingOnline = false
            }
        }
        val currentMaxCount = maxOf(onlineTracks.size, repoTracks.size)
        if (refreshTrigger == 0 && currentMaxCount >= 2500) {
            isLoadingOnline = false
            return@LaunchedEffect
        }
        try {
            val response = OnlineMusicRepository.fetchFromWorker(forceRefresh = refreshTrigger > 0 || currentMaxCount in 1 until 2500)
            if (response.tracks.isNotEmpty()) {
                val mapped = withContext(Dispatchers.Default) {
                    response.tracks.map { it.toAppTrack() }
                }
                OnlineMusicRepository.setMemoryCachedAppTracks(mapped)
                onlineTracks = mapped
            } else if (onlineTracks.isEmpty() && repoTracks.isEmpty()) {
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

    val effectiveTracks = if (repoTracks.size >= onlineTracks.size && repoTracks.isNotEmpty()) {
        repoTracks
    } else {
        onlineTracks
    }
    val isPersian = appSettings.language == AppLanguage.PERSIAN

    LaunchedEffect(effectiveTracks, isPersian) {
        if (effectiveTracks.isNotEmpty()) {
            OnlineMusicRepository.computeHubDataAsync(effectiveTracks, isPersian)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        OnlineMusicScreen(
            onlineTracks = effectiveTracks,
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
    OnlineMusicScreen(
        onlineTracks = onlineTracks,
        isLoading = isLoadingOnline,
        currentPlayingTrack = currentPlayingTrack,
        isPersian = isPersian,
        onPlayTrack = onPlayTrack,
        onRefresh = onRefreshOnline
    )
}

