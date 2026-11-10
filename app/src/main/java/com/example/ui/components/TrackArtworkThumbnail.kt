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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.OnlineMusicRepository

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
    val context = LocalContext.current
    val resolvedSmallUrl = remember(artworkUri, trackId, artist) {
        OnlineMusicRepository.getSmallCoverUrlForTrack(
            trackId = trackId.orEmpty(),
            fallbackUrl = artworkUri,
            artistName = artist.orEmpty()
        )
    }
    val isContentUri = resolvedSmallUrl.startsWith("content://")

    var loadedCoverBitmap by remember(resolvedSmallUrl) {
        mutableStateOf(
            if (resolvedSmallUrl.isNotBlank() && !isContentUri) {
                OnlineMusicRepository.getCachedCoverBitmap(resolvedSmallUrl)
            } else null
        )
    }

    LaunchedEffect(resolvedSmallUrl) {
        if (loadedCoverBitmap == null && resolvedSmallUrl.isNotBlank() && !isContentUri) {
            loadedCoverBitmap = OnlineMusicRepository.loadCoverBitmap(resolvedSmallUrl, maxDim = 360)
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
        } else if (resolvedSmallUrl.isNotBlank()) {
            val imageRequest = remember(resolvedSmallUrl, context) {
                ImageRequest.Builder(context)
                    .data(resolvedSmallUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build()
            }
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

