package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OnlineMusicRepository
import com.example.rememberAuraCoverRequest

/**
 * High-performance, memory-efficient artwork thumbnail with persistent private disk cache
 * bound strictly to the small cover (`cover_small`) for lists, grids, and MiniPlayer.
 */
@Composable
fun TrackArtworkThumbnail(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF9D4EDD),
    size: Dp = 46.dp,
    cornerRadius: Dp = 10.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    iconSize: Dp = (size.value * 0.5f).dp,
    title: String? = null,
    artist: String? = null,
    trackId: String? = null,
    primaryBorderColor: Color? = null,
    secondaryBorderColor: Color? = null
) {
    val resolvedSmallUrl = remember(artworkUri, trackId, artist) {
        OnlineMusicRepository.getSmallCoverUrlForTrack(
            trackId = trackId.orEmpty(),
            fallbackUrl = artworkUri,
            artistName = artist.orEmpty()
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF161626)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedSmallUrl.isNotBlank()) {
            val imageRequest = rememberAuraCoverRequest(
                trackId = trackId ?: title ?: resolvedSmallUrl,
                coverUrl = resolvedSmallUrl,
                isLarge = false
            )
            AsyncImage(
                model = imageRequest,
                contentDescription = title ?: "Artwork",
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MusicArtworkPlaceholder(
                title = title,
                artist = artist,
                trackId = trackId,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
