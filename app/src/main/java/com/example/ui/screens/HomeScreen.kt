package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.audio.AmbientPalette
import com.example.data.model.AppSettings
import com.example.data.model.PlaybackState
import com.example.data.model.PlayerStatus
import com.example.data.model.Track
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

@Composable
fun HomeScreen(
    playbackState: PlaybackState,
    allTracks: List<Track>,
    recentlyPlayed: List<Track>,
    favoriteTracks: List<Track>,
    palette: AmbientPalette,
    appSettings: AppSettings,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWrapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appSettings.language
    val context = LocalContext.current

    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val rotationDuration = if (isPlaying) 2800 else 14000

    val infiniteTransition = rememberInfiniteTransition(label = "heroLightBeam")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lightBeamAngle"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Header & Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.getString("aura_music", lang),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = palette.accent
                        )
                    )
                    Text(
                        text = Localization.getString("audio_environment", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(palette.primary.copy(alpha = 0.2f))
                        .border(1.dp, palette.primary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Aura Engine",
                        tint = palette.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Hero CURRENTLY PLAYING Card with Blurred Album Artwork & Rotating Glowing Light Ring
        item {
            val heroTrack = playbackState.currentTrack ?: allTracks.firstOrNull()
            val cardShape = RoundedCornerShape(24.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(cardShape)
                    .clickable {
                        heroTrack?.let { onPlayTrack(it, allTracks) }
                    }
                    .testTag("hero_quick_play_card")
            ) {
                // 1. Blurred Album Artwork Background inside the card
                if (heroTrack?.artworkUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(heroTrack.artworkUri)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(32.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.primary.copy(alpha = 0.35f), Color(0xFF0D0F1B))
                                )
                            )
                    )
                }

                // Dark Translucent Scrim for optimal text contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x800A0C16),
                                    Color(0xCC080A12)
                                )
                            )
                        )
                )

                // 2. Continuous Rotating Glowing Colored Light Beam attached around the perimeter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            rotate(rotationAngle) {
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            palette.accent,
                                            palette.secondary,
                                            palette.primary,
                                            palette.accent.copy(alpha = 0.8f),
                                            palette.secondary.copy(alpha = 0.9f),
                                            palette.primary.copy(alpha = 0.75f),
                                            palette.accent
                                        )
                                    ),
                                    radius = size.maxDimension * 0.85f,
                                    blendMode = BlendMode.Screen
                                )
                            }
                        }
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    palette.accent.copy(alpha = 0.65f),
                                    palette.secondary.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            ),
                            shape = cardShape
                        )
                )

                // 3. Card Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(palette.secondary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (playbackState.status == PlayerStatus.PLAYING) "CURRENTLY PLAYING" else "QUICK PLAY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = palette.secondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )
                        }

                        Text(
                            text = heroTrack?.genre ?: "Audio",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0C0))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (heroTrack != null) {
                            TrackArtworkThumbnail(
                                artworkUri = heroTrack.artworkUri,
                                accentColor = palette.secondary,
                                size = 56.dp,
                                shape = RoundedCornerShape(14.dp),
                                iconSize = 28.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = heroTrack?.title ?: "Select a Track",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = heroTrack?.artist ?: "Local-First Hi-Fi Audio",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFA0A0B8)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Big circular Play Button
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(palette.primary)
                                .clickable {
                                    if (playbackState.currentTrack != null) onTogglePlay()
                                    else heroTrack?.let { onPlayTrack(it, allTracks) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val isPlaying = playbackState.status == PlayerStatus.PLAYING
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recently Played
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    TextButton(onClick = onOpenLibrary) {
                        Text("See All", color = palette.accent, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentlyPlayed) { track ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131322)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onPlayTrack(track, allTracks) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = palette.primary,
                                    size = 120.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    iconSize = 40.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0), fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Starred & Favorites
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorites & High Energy",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val displayList = favoriteTracks.ifEmpty { allTracks.take(4) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                displayList.forEach { track ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121222)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(track, allTracks) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtworkThumbnail(
                                artworkUri = track.artworkUri,
                                accentColor = palette.accent,
                                size = 44.dp,
                                shape = RoundedCornerShape(10.dp),
                                iconSize = 24.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${track.genre}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = track.durationFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88889C))
                            )
                        }
                    }
                }
            }
        }
    }
}
