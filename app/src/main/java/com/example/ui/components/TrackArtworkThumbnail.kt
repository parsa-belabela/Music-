package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OnlineMusicRepository

/**
 * High-performance, memory-efficient artwork thumbnail with graceful fallback icon
 * for all lists, rows, cards, and bottom sheets throughout the app.
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
    val cacheKey = artworkUri ?: trackId ?: ""
    var loadedCoverBitmap by remember(cacheKey) {
        mutableStateOf(if (cacheKey.isNotBlank()) OnlineMusicRepository.getCachedCoverBitmap(cacheKey) else null)
    }
    LaunchedEffect(cacheKey) {
        if (loadedCoverBitmap == null && cacheKey.isNotBlank()) {
            val url = artworkUri?.takeIf { it.isNotBlank() }
                ?: (trackId?.let { OnlineMusicRepository.getCoverUrlForTrack(it, artist ?: "") })
            if (!url.isNullOrBlank()) {
                loadedCoverBitmap = OnlineMusicRepository.loadCoverBitmap(url)
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF161626)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = loadedCoverBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = title ?: "Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!artworkUri.isNullOrEmpty()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = title ?: "Artwork",
                contentScale = ContentScale.Crop,
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
