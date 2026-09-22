package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
 * Premium Apple-inspired A-Z Scrubber navigation for fast alphabet scrolling.
 * Supports both Persian and English alphabets with real-time haptic feedback
 * and floating magnifier preview bubble.
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

    val selectLetterAtY = { y: Float ->
        if (componentHeight > 0 && alphabet.isNotEmpty()) {
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
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Magnifier Bubble
        AnimatedVisibility(
            visible = activeLetter != null,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(initialScale = 0.7f),
            exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                    scaleOut(targetScale = 0.7f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 46.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(16.dp, CircleShape, ambientColor = palette.primary, spotColor = palette.accent)
                    .clip(CircleShape)
                    .background(palette.primary)
                    .border(1.5.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
            }
        }

        // The Alphabet Strip
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x18000000))
                .padding(horizontal = 4.dp, vertical = 6.dp)
                .onGloballyPositioned { coordinates ->
                    componentHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(alphabet) {
                    detectTapGestures(
                        onPress = { offset ->
                            selectLetterAtY(offset.y)
                            tryAwaitRelease()
                            activeLetter = null
                        }
                    )
                }
                .pointerInput(alphabet) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            selectLetterAtY(offset.y)
                        },
                        onDragEnd = {
                            activeLetter = null
                        },
                        onDragCancel = {
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            selectLetterAtY(change.position.y)
                        }
                    )
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
                        fontSize = if (alphabet.size > 26) 8.5.sp else 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else if (hasSongs) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> palette.accent
                            hasSongs -> Color.White
                            else -> Color(0x35FFFFFF)
                        }
                    ),
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
