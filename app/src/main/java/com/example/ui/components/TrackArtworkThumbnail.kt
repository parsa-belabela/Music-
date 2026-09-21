package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * High-performance, memory-efficient artwork thumbnail with graceful fallback icon
 * for all lists, rows, cards, and bottom sheets throughout the app.
 */
@Composable
fun TrackArtworkThumbnail(
    artworkUri: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: Shape = RoundedCornerShape(10.dp),
    iconSize: Dp = (size.value * 0.5f).dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF161626)),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUri.isNullOrEmpty()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = "Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
