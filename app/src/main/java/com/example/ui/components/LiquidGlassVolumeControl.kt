package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientPalette
import com.example.ui.theme.TextSecondary

/**
 * Clean, minimalist system volume slider for the Now Playing screen.
 */
@Composable
fun LiquidGlassVolumeControl(
    palette: AmbientPalette,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val maxVol = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    }

    var volumeFraction by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2)
        mutableFloatStateOf((current.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.VolumeDown,
            contentDescription = "Volume Down",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )

        Slider(
            value = volumeFraction,
            onValueChange = { newFrac ->
                volumeFraction = newFrac.coerceIn(0f, 1f)
                val targetVol = (volumeFraction * maxVol).toInt().coerceIn(0, maxVol)
                try {
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                } catch (_: Exception) {}
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Volume Up",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

