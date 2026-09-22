package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.audio.AmbientPalette
import com.example.data.model.Track
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class RadialActionItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val onSelect: () -> Unit
)

@Composable
fun RadialActionMenu(
    track: Track,
    palette: AmbientPalette,
    hapticEnabled: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val actions = remember(track) {
        listOf(
            RadialActionItem("fav", if (track.isFavorite) "Liked" else "Like", if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, onToggleFavorite),
            RadialActionItem("next", "Play Next", Icons.Default.QueueMusic, onPlayNext),
            RadialActionItem("queue", "Add to Queue", Icons.Default.PlaylistAdd, onAddToQueue),
            RadialActionItem("playlist", "Playlist", Icons.Default.BookmarkBorder, onAddToPlaylist),
            RadialActionItem("share", "Share", Icons.Default.Share, onShare)
        )
    }

    val animProgress = remember { Animatable(0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val radiusPx = with(density) { 95.dp.toPx() } * animProgress.value

            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Center Album Art Miniature
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(animProgress.value)
                        .liquidGlass(
                            shape = CircleShape,
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.35f,
                            borderWidth = 1.5.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Radial action buttons
                val totalActions = actions.size
                actions.forEachIndexed { index, action ->
                    val angle = (2 * Math.PI / totalActions) * index - (Math.PI / 2)
                    val offsetX = (radiusPx * cos(angle)).roundToInt()
                    val offsetY = (radiusPx * sin(angle)).roundToInt()

                    val isHighlighted = selectedIndex == index

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .scale(animProgress.value)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHighlighted) palette.accent else Color(0xE01A1A2E)
                            )
                            .clickable {
                                if (hapticEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                action.onSelect()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.title,
                            tint = if (isHighlighted) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
