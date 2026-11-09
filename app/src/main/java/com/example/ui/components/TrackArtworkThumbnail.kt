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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.OnlineMusicRepository

/**
 * High-performance, memory-efficient artwork thumbnail with persistent private disk cache
 * for Telegram/Cloudflare track covers and graceful fallback icon.
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
    val resolvedUrl = remember(artworkUri, trackId, artist) {
        val raw = artworkUri?.takeIf { it.isNotBlank() }
            ?: (trackId?.let { OnlineMusicRepository.getCoverUrlForTrack(it, artist ?: "") }).orEmpty()
        raw.replace("size=small", "size=large")
            .replace("size=thumb", "size=large")
            .replace("size=medium", "size=large")
    }
    val isContentUri = resolvedUrl.startsWith("content://")

    var loadedCoverBitmap by remember(resolvedUrl) {
        mutableStateOf(
            if (resolvedUrl.isNotBlank() && !isContentUri) {
                OnlineMusicRepository.getCachedCoverBitmap(resolvedUrl)
            } else null
        )
    }

    LaunchedEffect(resolvedUrl) {
        if (loadedCoverBitmap == null && resolvedUrl.isNotBlank() && !isContentUri) {
            loadedCoverBitmap = OnlineMusicRepository.loadCoverBitmap(resolvedUrl)
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
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isContentUri) {
            AsyncImage(
                model = resolvedUrl,
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
