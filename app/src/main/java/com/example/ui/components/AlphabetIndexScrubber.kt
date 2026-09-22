package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette

/**
 * Ultra-Smooth High-Performance A-Z Scrubber navigation for fast alphabet scrolling.
 * - Single-pass unified gesture detection with instant touch response & zero touch-slop lag
 * - Prevents coroutine piling and abrupt jumping by smooth nearest-neighbor indexing
 * - Apple-grade Liquid Glass Magnifier preview with glowing neon halo
 */
@Composable
fun AlphabetIndexScrubber(
    alphabet: List<String>,
    letterIndices: Map<String, Int>,
    onLetterSelected: (String, Int) -> Unit,
    palette: AmbientPalette,
    modifier: Modifier = Modifier
) {
    var componentHeight by remember { mutableFloatStateOf(0f) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val selectLetterAtY: (Float) -> Unit = { y ->
        if (componentHeight > 0f && alphabet.isNotEmpty()) {
            val fraction = (y / componentHeight).coerceIn(0f, 0.999f)
            val index = (fraction * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
            val letter = alphabet[index]
            if (activeLetter != letter) {
                activeLetter = letter
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {}
                letterIndices[letter]?.let { targetIndex ->
                    onLetterSelected(letter, targetIndex)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Magnifier Bubble (Lightweight fast animation, no frame-drop springs)
        AnimatedVisibility(
            visible = activeLetter != null,
            enter = fadeIn(animationSpec = tween(90)),
            exit = fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(20.dp, CircleShape, ambientColor = palette.primary, spotColor = palette.accent)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                palette.primary,
                                Color(0xFF161628)
                            )
                        )
                    )
                    .border(1.5.dp, palette.accent.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp
                    )
                )
            }
        }

        // The Alphabet Strip
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x280B0C16))
                .border(0.8.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .onGloballyPositioned { coordinates ->
                    componentHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(alphabet, letterIndices) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        selectLetterAtY(down.position.y)
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                break
                            }
                            change.consume()
                            selectLetterAtY(change.position.y)
                        }
                        activeLetter = null
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { letter ->
                val hasSongs = letterIndices.containsKey(letter)
                val isSelected = activeLetter == letter

                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (alphabet.size > 26) 8.5.sp else 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else if (hasSongs) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> palette.accent
                            hasSongs -> Color.White
                            else -> Color(0x38FFFFFF)
                        }
                    ),
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }
    }
}
